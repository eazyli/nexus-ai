package com.eazyai.ai.nexus.core.rag.knowledge;

import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import com.eazyai.ai.nexus.core.rag.RerankService;
import com.eazyai.ai.nexus.core.rag.VectorStore;
import com.eazyai.ai.nexus.core.rag.model.TextChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 多知识库内容检索器
 * 
 * <p>支持从多个知识库检索内容，用于自动RAG</p>
 * <p>支持混合检索（向量 + BM25）和重排序（Rerank）</p>
 * <p>与 AssistantFactory 集成，实现智能体的自动知识检索能力</p>
 * 
 * <p>属于核心层业务逻辑，负责检索编排</p>
 */
@Slf4j
public class KnowledgeContentRetriever implements ContentRetriever {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final List<String> knowledgeIds;
    private final int maxResults;
    private final double minScore;
    private final boolean useHybridSearch;
    private final RerankService rerankService;
    private final int rerankTopK;

    private KnowledgeContentRetriever(Builder builder) {
        this.vectorStore = builder.vectorStore;
        this.embeddingModel = builder.embeddingModel;
        this.knowledgeIds = builder.knowledgeIds;
        this.maxResults = builder.maxResults;
        this.minScore = builder.minScore;
        this.useHybridSearch = builder.useHybridSearch;
        this.rerankService = builder.rerankService;
        this.rerankTopK = builder.rerankTopK;
    }

    @Override
    public List<Content> retrieve(Query query) {
        String queryText = query.text();
        
        // 添加详细日志
        if (queryText == null || queryText.trim().isEmpty()) {
            log.error("[KnowledgeContentRetriever] queryText为空或null，无法检索. queryText='{}'", queryText);
            return List.of();
        }
        
        log.debug("[KnowledgeContentRetriever] 检索: {}, 知识库: {}", queryText, knowledgeIds);

        try {
            List<TextChunk> allChunks;
            
            // 粗排阶段：检索更多候选文档
            int candidateSize = rerankService != null ? Math.max(maxResults * 3, 20) : maxResults * 2;
            
            if (useHybridSearch) {
                // 混合检索（向量 + BM25 + RRF 融合）
                float[] queryVector = embeddingModel.embed(queryText);
                allChunks = vectorStore.hybridSearch(queryText, queryVector, candidateSize);
            } else {
                // 纯向量检索
                float[] queryVector = embeddingModel.embed(queryText);
                allChunks = vectorStore.similaritySearch(queryVector, candidateSize);
            }

            log.debug("[KnowledgeContentRetriever] 从ES检索到 {} 条chunk", allChunks.size());

            // 过滤知识库（如果指定）
            List<TextChunk> filteredChunks;
            if (knowledgeIds != null && !knowledgeIds.isEmpty()) {
                filteredChunks = allChunks.stream()
                        .filter(chunk -> knowledgeIds.contains(chunk.getKnowledgeId()))
                        .collect(Collectors.toList());
            } else {
                filteredChunks = allChunks;
            }

            log.debug("[KnowledgeContentRetriever] 过滤后得到 {} 条chunk", filteredChunks.size());

            // 检查chunk内容是否为空
            if (!filteredChunks.isEmpty()) {
                int emptyCount = 0;
                for (TextChunk chunk : filteredChunks) {
                    if (chunk.getContent() == null || chunk.getContent().trim().isEmpty()) {
                        emptyCount++;
                        log.warn("[KnowledgeContentRetriever] 发现空内容chunk: id={}, content='{}'", 
                                chunk.getId(), chunk.getContent());
                    }
                }
                if (emptyCount > 0) {
                    log.error("[KnowledgeContentRetriever] 过滤后的chunk中有 {} 条为空内容", emptyCount);
                }
            }

            // 精排阶段：使用 Rerank 模型重排序
            List<TextChunk> rankedChunks;
            if (rerankService != null && !filteredChunks.isEmpty()) {
                rankedChunks = rerankChunks(queryText, filteredChunks);
            } else {
                rankedChunks = filteredChunks;
            }

            // 过滤分数阈值并限制数量
            List<Content> contents = rankedChunks.stream()
                    .filter(chunk -> chunk.getScore() >= minScore)
                    .limit(maxResults)
                    .map(this::toContent)
                    .collect(Collectors.toList());

            log.debug("[KnowledgeContentRetriever] 检索到 {} 条内容", contents.size());
            return contents;

        } catch (Exception e) {
            log.error("[KnowledgeContentRetriever] 检索失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 使用 Rerank 模型对文档进行重排序
     */
    private List<TextChunk> rerankChunks(String query, List<TextChunk> chunks) {
        long startTime = System.currentTimeMillis();
        
        // 构建 Rerank 文档列表
        List<RerankService.RerankDocument> documents = new ArrayList<>();
        for (TextChunk chunk : chunks) {
            documents.add(new RerankService.RerankDocument(
                    chunk.getId(),
                    chunk.getContent(),
                    (float) chunk.getScore()
            ));
        }
        
        // 执行重排序
        int topK = rerankTopK > 0 ? rerankTopK : maxResults;
        List<RerankService.RerankResult> results = rerankService.rerank(query, documents, topK);
        
        // 转换结果，更新分数
        List<TextChunk> rankedChunks = new ArrayList<>();
        for (RerankService.RerankResult result : results) {
            TextChunk originalChunk = chunks.get(result.originalIndex());
            // 创建新的 TextChunk 并更新分数
            TextChunk rankedChunk = new TextChunk();
            rankedChunk.setId(originalChunk.getId());
            rankedChunk.setDocumentId(originalChunk.getDocumentId());
            rankedChunk.setKnowledgeId(originalChunk.getKnowledgeId());
            rankedChunk.setContent(originalChunk.getContent());
            rankedChunk.setEmbedding(originalChunk.getEmbedding());
            rankedChunk.setStartPosition(originalChunk.getStartPosition());
            rankedChunk.setEndPosition(originalChunk.getEndPosition());
            rankedChunk.setChunkIndex(originalChunk.getChunkIndex());
            rankedChunk.setMetadata(originalChunk.getMetadata());
            rankedChunk.setScore(result.rerankScore());  // 使用重排序分数
            rankedChunks.add(rankedChunk);
        }
        
        long costTime = System.currentTimeMillis() - startTime;
        log.debug("[KnowledgeContentRetriever] Rerank 完成, 耗时: {}ms, 输入: {} 条, 输出: {} 条", 
                costTime, chunks.size(), rankedChunks.size());
        
        return rankedChunks;
    }

    private Content toContent(TextChunk chunk) {
        java.util.Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("id", chunk.getId());
        metadataMap.put("knowledgeId", chunk.getKnowledgeId() != null ? chunk.getKnowledgeId() : "");
        metadataMap.put("score", chunk.getScore());
        if (chunk.getMetadata() != null) {
            metadataMap.putAll(chunk.getMetadata());
        }
        
        Metadata metadata = Metadata.from(metadataMap);
        TextSegment segment = TextSegment.from(chunk.getContent(), metadata);
        return Content.from(segment);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private VectorStore vectorStore;
        private EmbeddingModel embeddingModel;
        private List<String> knowledgeIds;
        private int maxResults = 5;
        private double minScore = 0.3;
        private boolean useHybridSearch = true;
        private RerankService rerankService;
        private int rerankTopK = 5;

        public Builder vectorStore(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
            return this;
        }

        public Builder embeddingModel(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }

        public Builder knowledgeIds(List<String> knowledgeIds) {
            this.knowledgeIds = knowledgeIds;
            return this;
        }

        public Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder minScore(double minScore) {
            this.minScore = minScore;
            return this;
        }

        public Builder useHybridSearch(boolean useHybridSearch) {
            this.useHybridSearch = useHybridSearch;
            return this;
        }

        public Builder rerankService(RerankService rerankService) {
            this.rerankService = rerankService;
            return this;
        }

        public Builder rerankTopK(int rerankTopK) {
            this.rerankTopK = rerankTopK;
            return this;
        }

        public KnowledgeContentRetriever build() {
            if (vectorStore == null) {
                throw new IllegalArgumentException("vectorStore 不能为空");
            }
            if (embeddingModel == null) {
                throw new IllegalArgumentException("embeddingModel 不能为空");
            }
            return new KnowledgeContentRetriever(this);
        }
    }
}
