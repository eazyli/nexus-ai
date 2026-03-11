package com.eazyai.ai.nexus.core.rag;

import com.eazyai.ai.nexus.core.rag.model.TextChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG 编排服务
 * 
 * <p>核心层服务，协调 EmbeddingModel、DocumentSplitter 和 VectorStore</p>
 * <p>负责文档索引、切片、向量化和检索的流程编排</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RagOrchestrator {

    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter documentSplitter;
    private final VectorStore vectorStore;

    /**
     * 索引文本切片（已切片的内容直接索引）
     * 
     * @param chunks 切片列表
     * @param knowledgeId 知识库ID
     * @return 索引的切片数量
     */
    public int indexChunks(List<TextChunk> chunks, String knowledgeId) {
        if (chunks == null || chunks.isEmpty()) {
            return 0;
        }

        long startTime = System.currentTimeMillis();
        int totalChunks = chunks.size();

        // 分批处理（防止OOM）
        int batchSize = 10;
        for (int i = 0; i < totalChunks; i += batchSize) {
            int end = Math.min(i + batchSize, totalChunks);
            List<TextChunk> batch = chunks.subList(i, end);

            // 生成向量
            List<String> texts = new ArrayList<>();
            for (TextChunk chunk : batch) {
                texts.add(chunk.getContent());
            }
            List<float[]> embeddings = embeddingModel.embedBatch(texts);

            // 设置 ID 和向量
            for (int j = 0; j < batch.size(); j++) {
                TextChunk chunk = batch.get(j);
                if (chunk.getId() == null || chunk.getId().isEmpty()) {
                    chunk.setId(UUID.randomUUID().toString());
                }
                chunk.setKnowledgeId(knowledgeId);
                chunk.setEmbedding(embeddings.get(j));
            }

            // 存储向量
            vectorStore.addBatch(batch);

            log.debug("[RagOrchestrator] 索引进度: {}/{}", Math.min(end, totalChunks), totalChunks);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[RagOrchestrator] 索引完成: {} 个切片, 耗时: {}ms", totalChunks, totalTime);

        return totalChunks;
    }

    /**
     * 索引文本到知识库
     * 
     * @param text 文本内容
     * @param knowledgeId 知识库ID
     * @param metadata 元数据
     * @return 切片数量
     */
    public int indexText(String text, String knowledgeId, Map<String, Object> metadata) {
        log.info("[RagOrchestrator] 开始索引文本 -> 知识库: {}", knowledgeId);

        // 1. 切片
        List<TextChunk> chunks = documentSplitter.split(text);
        log.debug("[RagOrchestrator] 文本切片完成: {} 个切片", chunks.size());

        // 2. 设置元数据
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            chunk.setChunkIndex(i);
            if (metadata != null) {
                chunk.getMetadata().putAll(metadata);
            }
        }

        // 3. 索引
        return indexChunks(chunks, knowledgeId);
    }

    /**
     * 向量检索
     */
    public RetrievalResult vectorSearch(String query, int topK) {
        return vectorSearch(query, topK, null);
    }

    /**
     * 向量检索（指定知识库）
     */
    public RetrievalResult vectorSearch(String query, int topK, String knowledgeId) {
        long startTime = System.currentTimeMillis();

        float[] queryVector = embeddingModel.embed(query);
        List<TextChunk> chunks = vectorStore.similaritySearch(queryVector, topK, knowledgeId);

        return RetrievalResult.builder()
                .query(query)
                .chunks(chunks)
                .totalTime(System.currentTimeMillis() - startTime)
                .searchType("vector")
                .build();
    }

    /**
     * 关键词检索
     */
    public RetrievalResult keywordSearch(String query, int topK) {
        long startTime = System.currentTimeMillis();

        List<TextChunk> chunks = vectorStore.keywordSearch(query, topK);

        return RetrievalResult.builder()
                .query(query)
                .chunks(chunks)
                .totalTime(System.currentTimeMillis() - startTime)
                .searchType("keyword")
                .build();
    }

    /**
     * 混合检索（向量 + 关键词）
     */
    public RetrievalResult hybridSearch(String query, int topK) {
        long startTime = System.currentTimeMillis();

        float[] queryVector = embeddingModel.embed(query);
        List<TextChunk> chunks = vectorStore.hybridSearch(query, queryVector, topK);

        return RetrievalResult.builder()
                .query(query)
                .chunks(chunks)
                .totalTime(System.currentTimeMillis() - startTime)
                .searchType("hybrid")
                .build();
    }

    /**
     * 删除知识库的所有向量
     */
    public void deleteKnowledge(String knowledgeId) {
        vectorStore.deleteByKnowledgeId(knowledgeId);
        log.info("[RagOrchestrator] 已删除知识库向量: {}", knowledgeId);
    }
}
