package com.eazyai.ai.nexus.infra.rag;

import java.util.List;

/**
 * 向量嵌入服务接口
 */
public interface EmbeddingService {
    
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
     */
    int getDimension();
    
    /**
     * 获取模型名称
     */
    String getModelName();
}
