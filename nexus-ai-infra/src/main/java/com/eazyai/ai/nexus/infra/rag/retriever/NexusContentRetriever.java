package com.eazyai.ai.nexus.infra.rag.retriever;

import com.eazyai.ai.nexus.infra.rag.EmbeddingService;
import com.eazyai.ai.nexus.infra.rag.TextChunk;
import com.eazyai.ai.nexus.infra.rag.VectorStore;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LangChain4j ContentRetriever 适配器
 * 
 * <p>将现有的 VectorStore 和 EmbeddingService 适配为 LangChain4j 的 ContentRetriever 接口，
 * 使 Assistant 可以自动进行 RAG 检索，无需通过工具手动调用。</p>
 * 
 * <p>使用方式：</p>
 * <pre>
 * AiServices.builder(Assistant.class)
 *     .contentRetriever(new NexusContentRetriever(vectorStore, embeddingService, knowledgeId))
 *     .build();
 * </pre>
 */
@Slf4j
public class NexusContentRetriever implements ContentRetriever {

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final String knowledgeId;
    private final int maxResults;
    private final double minScore;

    /**
     * 创建 ContentRetriever
     * 
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param knowledgeId 知识库ID（可选，null 表示搜索所有知识库）
     */
    public NexusContentRetriever(VectorStore vectorStore, EmbeddingService embeddingService, 
                                  String knowledgeId) {
        this(vectorStore, embeddingService, knowledgeId, 5, 0.5);
    }

    /**
     * 创建 ContentRetriever（完整参数）
     * 
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param knowledgeId 知识库ID
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度阈值（0-1）
     */
    public NexusContentRetriever(VectorStore vectorStore, EmbeddingService embeddingService,
                                  String knowledgeId, int maxResults, double minScore) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.knowledgeId = knowledgeId;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @Override
    public List<Content> retrieve(Query query) {
        String queryText = query.text();
        log.debug("[NexusContentRetriever] 检索查询: {}", queryText);

        try {
            // 1. 生成查询向量
            float[] queryVector = embeddingService.embed(queryText);

            // 2. 向量检索
            List<TextChunk> chunks = vectorStore.similaritySearch(queryVector, maxResults, knowledgeId);

            // 3. 转换为 LangChain4j Content
            List<Content> contents = chunks.stream()
                    .filter(chunk -> chunk.getScore() >= minScore)
                    .map(this::toContent)
                    .collect(Collectors.toList());

            log.debug("[NexusContentRetriever] 检索到 {} 条内容", contents.size());
            return contents;

        } catch (Exception e) {
            log.error("[NexusContentRetriever] 检索失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 将 TextChunk 转换为 LangChain4j Content
     */
    private Content toContent(TextChunk chunk) {
        // 构建 Metadata
        java.util.Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("id", chunk.getId());
        metadataMap.put("knowledgeId", chunk.getKnowledgeId() != null ? chunk.getKnowledgeId() : "");
        metadataMap.put("score", chunk.getScore());
        Metadata metadata = Metadata.from(metadataMap);
        
        // 创建 TextSegment 并转换为 Content
        TextSegment segment = TextSegment.from(chunk.getContent(), metadata);
        return Content.from(segment);
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private VectorStore vectorStore;
        private EmbeddingService embeddingService;
        private String knowledgeId;
        private int maxResults = 5;
        private double minScore = 0.5;

        public Builder vectorStore(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
            return this;
        }

        public Builder embeddingService(EmbeddingService embeddingService) {
            this.embeddingService = embeddingService;
            return this;
        }

        public Builder knowledgeId(String knowledgeId) {
            this.knowledgeId = knowledgeId;
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

        public NexusContentRetriever build() {
            if (vectorStore == null) {
                throw new IllegalArgumentException("vectorStore 不能为空");
            }
            if (embeddingService == null) {
                throw new IllegalArgumentException("embeddingService 不能为空");
            }
            return new NexusContentRetriever(vectorStore, embeddingService, knowledgeId, maxResults, minScore);
        }
    }
}
