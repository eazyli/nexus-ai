package com.eazyai.ai.nexus.core.rag;

import com.eazyai.ai.nexus.core.rag.model.TextChunk;

import java.util.List;

/**
 * 文档切片器接口
 * 
 * <p>核心层接口，可以使用 LangChain4j 的 DocumentSplitter 实现</p>
 */
public interface DocumentSplitter {
    
    /**
     * 切分文本
     * @param text 原始文本
     * @return 切片列表
     */
    List<TextChunk> split(String text);
    
    /**
     * 获取切片大小
     */
    int getChunkSize();
    
    /**
     * 获取重叠大小
     */
    int getChunkOverlap();
}
