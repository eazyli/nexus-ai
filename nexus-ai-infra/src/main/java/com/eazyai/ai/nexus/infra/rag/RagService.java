package com.eazyai.ai.nexus.infra.rag;

import com.eazyai.ai.nexus.infra.rag.parser.DocumentParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RAG服务 - 提供完整的检索增强生成流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final DocumentParserFactory parserFactory;
    private final TextSplitter textSplitter;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    /**
     * 索引文档到知识库
     * @param content 文档内容
     * @param fileName 文件名
     * @param knowledgeId 知识库ID
     * @return 切片数量
     */
    public int indexDocument(byte[] content, String fileName, String knowledgeId) {
        log.info("开始索引文档: {} -> 知识库: {}", fileName, knowledgeId);
        long startTime = System.currentTimeMillis();
        
        // 1. 解析文档
        Document document = parserFactory.parse(content, fileName);
        document.setId(UUID.randomUUID().toString());
        document.setKnowledgeId(knowledgeId);
        
        // 2. 文档切片
        List<TextChunk> chunks = textSplitter.split(document);
        log.info("文档切片完成: {} -> {} 个切片", fileName, chunks.size());
        
        // 3. 生成向量
        List<String> texts = chunks.stream().map(TextChunk::getContent).toList();
        List<float[]> embeddings = embeddingService.embedBatch(texts);
        
        // 4. 设置向量ID和向量
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            chunk.setId(UUID.randomUUID().toString());
            chunk.setEmbedding(embeddings.get(i));
        }
        
        // 5. 存储向量
        vectorStore.addBatch(chunks);
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("文档索引完成: {} 个切片, 耗时: {}ms", chunks.size(), totalTime);
        
        return chunks.size();
    }

    /**
     * 索引文本到知识库
     * @param text 文本内容
     * @param knowledgeId 知识库ID
     * @param metadata 元数据
     * @return 切片数量
     */
    public int indexText(String text, String knowledgeId, java.util.Map<String, Object> metadata) {
        log.info("开始索引文本 -> 知识库: {}", knowledgeId);
        
        Document document = Document.of(text, metadata);
        document.setId(UUID.randomUUID().toString());
        document.setKnowledgeId(knowledgeId);
        
        List<TextChunk> chunks = textSplitter.split(document);
        
        List<String> texts = chunks.stream().map(TextChunk::getContent).toList();
        List<float[]> embeddings = embeddingService.embedBatch(texts);
        
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            chunk.setId(UUID.randomUUID().toString());
            chunk.setEmbedding(embeddings.get(i));
        }
        
        vectorStore.addBatch(chunks);
        
        log.info("文本索引完成: {} 个切片", chunks.size());
        return chunks.size();
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
        
        float[] queryVector = embeddingService.embed(query);
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
     * 混合检索（向量+关键词）
     */
    public RetrievalResult hybridSearch(String query, int topK) {
        long startTime = System.currentTimeMillis();
        
        float[] queryVector = embeddingService.embed(query);
        List<TextChunk> chunks = vectorStore.hybridSearch(query, queryVector, topK);
        
        return RetrievalResult.builder()
                .query(query)
                .chunks(chunks)
                .totalTime(System.currentTimeMillis() - startTime)
                .searchType("hybrid")
                .build();
    }

    /**
     * 删除知识库
     */
    public void deleteKnowledge(String knowledgeId) {
        vectorStore.deleteByKnowledgeId(knowledgeId);
        log.info("已删除知识库: {}", knowledgeId);
    }
}
