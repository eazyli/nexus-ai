package com.eazyai.ai.nexus.infra.rag;

import java.util.List;

/**
 * 文本切片器接口
 */
public interface TextSplitter {
    
    /**
     * 切分文本
     * @param text 原始文本
     * @return 切片列表
     */
    List<TextChunk> split(String text);
    
    /**
     * 切分文档
     * @param document 文档
     * @return 切片列表
     */
    List<TextChunk> split(Document document);
    
    /**
     * 获取切片大小
     */
    int getChunkSize();
    
    /**
     * 获取重叠大小
     */
    int getChunkOverlap();
}
