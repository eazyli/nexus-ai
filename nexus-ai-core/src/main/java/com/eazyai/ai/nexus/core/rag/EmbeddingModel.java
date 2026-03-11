package com.eazyai.ai.nexus.core.rag;

import com.eazyai.ai.nexus.core.rag.model.TextChunk;

import java.util.List;

/**
 * Embedding 模型接口
 * 
 * <p>核心层接口，由 infra 层实现具体调用逻辑</p>
 */
public interface EmbeddingModel {
    
    /**
     * 生成文本向量
     * @param text 文本
     * @return 向量
     */
    float[] embed(String text);
    
    /**
     * 批量生成向量
     * @param texts 文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);
    
    /**
     * 获取向量维度
     * @return 维度
     */
    int dimension();
    
    /**
     * 获取模型名称
     * @return 模型名称
     */
    String modelName();
}
