package com.eazyai.ai.nexus.infra.rag;

import lombok.Data;

import java.util.Map;

/**
 * 文档切片模型
 */
@Data
public class TextChunk {
    
    private String id;
    
    private String documentId;
    
    private String knowledgeId;
    
    private String content;
    
    /**
     * 向量数据
     */
    private float[] embedding;
    
    /**
     * 在原文中的起始位置
     */
    private int startPosition;
    
    /**
     * 在原文中的结束位置
     */
    private int endPosition;
    
    /**
     * 切片索引
     */
    private int chunkIndex;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 相似度分数（检索时使用）
     */
    private double score;
    
    public static TextChunk of(String content) {
        TextChunk chunk = new TextChunk();
        chunk.setContent(content);
        return chunk;
    }
    
    public static TextChunk of(String content, int start, int end, int index) {
        TextChunk chunk = of(content);
        chunk.setStartPosition(start);
        chunk.setEndPosition(end);
        chunk.setChunkIndex(index);
        return chunk;
    }
}
