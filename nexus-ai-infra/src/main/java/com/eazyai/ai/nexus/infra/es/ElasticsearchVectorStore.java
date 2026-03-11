package com.eazyai.ai.nexus.infra.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.eazyai.ai.nexus.core.rag.VectorStore;
import com.eazyai.ai.nexus.core.rag.model.TextChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch 8.x 向量存储实现
 * 
 * <p>支持：</p>
 * <ul>
 *   <li>向量相似度检索（kNN）</li>
 *   <li>关键词检索（BM25）</li>
 *   <li>混合检索（RRF融合）</li>
 * </ul>
 * 
 * <p>ES索引映射：</p>
 * <pre>
 * {
 *   "mappings": {
 *     "properties": {
 *       "chunkId": { "type": "keyword" },
 *       "knowledgeId": { "type": "keyword" },
 *       "content": { "type": "text", "analyzer": "standard" },
 *       "contentVector": { 
 *         "type": "dense_vector",
 *         "dims": 1536,
 *         "index": true,
 *         "similarity": "cosine"
 *       },
 *       "metadata": { "type": "object" }
 *     }
 *   }
 * }
 * </pre>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.agent.rag.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchVectorStore implements VectorStore {

    @Value("${ai.agent.rag.elasticsearch.host:localhost}")
    private String host;

    @Value("${ai.agent.rag.elasticsearch.port:9200}")
    private int port;

    @Value("${ai.agent.rag.elasticsearch.username:}")
    private String username;

    @Value("${ai.agent.rag.elasticsearch.password:}")
    private String password;

    @Value("${ai.agent.rag.elasticsearch.index:knowledge_chunks}")
    private String indexName;

    @Value("${ai.agent.rag.elasticsearch.vector-dims:1536}")
    private int vectorDims;

    private ElasticsearchClient client;
    private RestClient restClient;

    @PostConstruct
    public void init() {
        try {
            // 创建低级 RestClient
            restClient = RestClient.builder(new HttpHost(host, port, "http"))
                    .build();

            // 创建传输层和 ElasticsearchClient
            RestClientTransport transport = new RestClientTransport(
                    restClient, 
                    new JacksonJsonpMapper(new ObjectMapper())
            );
            client = new ElasticsearchClient(transport);

            log.info("[ElasticsearchVectorStore] 初始化成功: {}:{}", host, port);
            
            // 确保索引存在
            if (!indexExists()) {
                createIndex();
            }
        } catch (Exception e) {
            log.error("[ElasticsearchVectorStore] 初始化失败", e);
        }
    }

    @PreDestroy
    public void close() {
        if (restClient != null) {
            try {
                restClient.close();
                log.info("[ElasticsearchVectorStore] 客户端已关闭");
            } catch (IOException e) {
                log.error("[ElasticsearchVectorStore] 关闭客户端失败", e);
            }
        }
    }

    @Override
    public void add(TextChunk chunk) {
        addBatch(List.of(chunk));
    }

    @Override
    public void addBatch(List<TextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        try {
            List<BulkOperation> operations = new ArrayList<>();
            
            for (TextChunk chunk : chunks) {
                if (chunk.getId() == null) {
                    chunk.setId(UUID.randomUUID().toString());
                }

                EsDocument doc = new EsDocument();
                doc.setChunkId(chunk.getId());
                doc.setKnowledgeId(chunk.getKnowledgeId());
                doc.setDocumentId(chunk.getDocumentId());
                doc.setContent(chunk.getContent());
                doc.setContentVector(chunk.getEmbedding() != null ? toFloatList(chunk.getEmbedding()) : null);
                doc.setMetadata(chunk.getMetadata());
                doc.setCreateTime(new Date());

                operations.add(BulkOperation.of(b -> b
                        .index(IndexOperation.of(i -> i
                                .index(indexName)
                                .id(chunk.getId())
                                .document(doc)
                        ))
                ));
            }

            BulkResponse response = client.bulk(b -> b
                    .index(indexName)
                    .operations(operations)
            );

            if (response.errors()) {
                log.error("[ElasticsearchVectorStore] 批量插入有错误");
                response.items().forEach(item -> {
                    if (item.error() != null) {
                        log.error("  - 错误: {}", item.error().reason());
                    }
                });
            } else {
                log.info("[ElasticsearchVectorStore] 批量插入 {} 个切片成功", chunks.size());
            }

        } catch (Exception e) {
            log.error("[ElasticsearchVectorStore] 批量插入失败", e);
            throw new RuntimeException("向量存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TextChunk> similaritySearch(float[] query, int topK) {
        return similaritySearch(query, topK, null);
    }

    @Override
    public List<TextChunk> similaritySearch(float[] query, int topK, String knowledgeId) {
        try {
            List<Float> queryVector = toFloatList(query);
            
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(indexName)
                    .size(topK)
                    .knn(k -> k
                            .field("contentVector")
                            .queryVector(queryVector)
                            .k((long) topK)
                            .numCandidates((long) Math.max(topK * 10, 100))
                    );

            // 如果指定了知识库ID，添加过滤条件
            if (knowledgeId != null && !knowledgeId.isEmpty()) {
                searchBuilder.knn(k -> k
                        .field("contentVector")
                        .queryVector(queryVector)
                        .k((long) topK)
                        .numCandidates((long) Math.max(topK * 10, 100))
                        .filter(Query.of(q -> q.term(TermQuery.of(t -> t.field("knowledgeId").value(knowledgeId)))))
                );
            }

            SearchResponse<EsDocument> response = client.search(searchBuilder.build(), EsDocument.class);
            
            return response.hits().hits().stream()
                    .map(this::hitToChunk)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[ElasticsearchVectorStore] 向量检索失败", e);
            return List.of();
        }
    }

    @Override
    public List<TextChunk> keywordSearch(String query, int topK) {
        try {
            SearchResponse<EsDocument> response = client.search(s -> s
                    .index(indexName)
                    .size(topK)
                    .query(q -> q.match(m -> m.field("content").query(query))),
                    EsDocument.class
            );

            return response.hits().hits().stream()
                    .map(this::hitToChunk)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[ElasticsearchVectorStore] 关键词检索失败", e);
            return List.of();
        }
    }

    @Override
    public List<TextChunk> hybridSearch(String query, float[] queryVector, int topK) {
        try {
            // 1. 向量检索
            List<TextChunk> vectorResults = similaritySearch(queryVector, topK * 2);
            
            // 2. 关键词检索
            List<TextChunk> keywordResults = keywordSearch(query, topK * 2);
            
            // 3. RRF融合 (Reciprocal Rank Fusion)
            Map<String, Double> scores = new HashMap<>();
            Map<String, TextChunk> chunkMap = new HashMap<>();
            int rank;
            
            // RRF公式: score = sum(1 / (k + rank))，k通常为60
            final int K = 60;
            
            rank = 1;
            for (TextChunk chunk : vectorResults) {
                String id = chunk.getId();
                scores.merge(id, 1.0 / (K + rank), Double::sum);
                chunkMap.putIfAbsent(id, chunk);
                rank++;
            }
            
            rank = 1;
            for (TextChunk chunk : keywordResults) {
                String id = chunk.getId();
                scores.merge(id, 1.0 / (K + rank), Double::sum);
                chunkMap.putIfAbsent(id, chunk);
                rank++;
            }
            
            // 按融合分数排序
            return scores.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(topK)
                    .map(e -> {
                        TextChunk chunk = chunkMap.get(e.getKey());
                        chunk.setScore(e.getValue());
                        return chunk;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[ElasticsearchVectorStore] 混合检索失败", e);
            return List.of();
        }
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        try {
            DeleteByQueryResponse response = client.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("knowledgeId").value(knowledgeId)))
            );
            
            log.info("[ElasticsearchVectorStore] 删除知识库 {} 的 {} 条记录", knowledgeId, response.deleted());

        } catch (Exception e) {
            log.error("[ElasticsearchVectorStore] 删除知识库失败: {}", knowledgeId, e);
        }
    }

    @Override
    public void delete(String chunkId) {
        try {
            DeleteResponse response = client.delete(d -> d
                    .index(indexName)
                    .id(chunkId)
            );
            log.debug("[ElasticsearchVectorStore] 删除切片: {}, result: {}", chunkId, response.result());

        } catch (Exception e) {
            log.error("[ElasticsearchVectorStore] 删除切片失败: {}", chunkId, e);
        }
    }

    @Override
    public boolean indexExists() {
        try {
            return client.indices().exists(e -> e.index(indexName)).value();
        } catch (IOException e) {
            log.error("[ElasticsearchVectorStore] 检查索引失败", e);
            return false;
        }
    }

    @Override
    public void createIndex() {
        try {
            client.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("chunkId", p -> p.keyword(k -> k))
                            .properties("knowledgeId", p -> p.keyword(k -> k))
                            .properties("documentId", p -> p.keyword(k -> k))
                            .properties("content", p -> p.text(t -> t.analyzer("standard")))
                            .properties("contentVector", p -> p.denseVector(d -> d
                                    .dims(vectorDims)
                                    .index(true)
                                    .similarity("cosine")
                            ))
                            .properties("metadata", p -> p.object(o -> o.enabled(true)))
                            .properties("createTime", p -> p.date(d -> d))
                    )
            );
            
            log.info("[ElasticsearchVectorStore] 创建索引: {}", indexName);

        } catch (Exception e) {
            log.error("[ElasticsearchVectorStore] 创建索引失败", e);
        }
    }

    /**
     * 转换 Hit 到 TextChunk
     */
    private TextChunk hitToChunk(Hit<EsDocument> hit) {
        EsDocument doc = hit.source();
        if (doc == null) {
            log.warn("[ElasticsearchVectorStore] Hit source为null");
            return null;
        }
        
        // 检查content是否为空
        String content = doc.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.warn("[ElasticsearchVectorStore] 发现空内容chunk: chunkId={}, content='{}'", 
                    doc.getChunkId(), content);
        }
        
        TextChunk chunk = new TextChunk();
        chunk.setId(doc.getChunkId());
        chunk.setKnowledgeId(doc.getKnowledgeId());
        chunk.setDocumentId(doc.getDocumentId());
        chunk.setContent(content);
        chunk.setMetadata(doc.getMetadata() != null ? doc.getMetadata() : new HashMap<>());
        chunk.setScore(hit.score() != null ? hit.score().doubleValue() : null);
        
        return chunk;
    }

    /**
     * float[] 转 List<Float>
     */
    private List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) {
            list.add(f);
        }
        return list;
    }

    /**
     * ES 文档实体（用于序列化）
     */
    @lombok.Data
    public static class EsDocument {
        private String chunkId;
        private String knowledgeId;
        private String documentId;
        private String content;
        private List<Float> contentVector;
        private Map<String, Object> metadata;
        private Date createTime;
    }
}
