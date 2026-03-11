package com.eazyai.ai.nexus.core.rag;

import com.eazyai.ai.nexus.core.rag.model.TextChunk;

import java.util.List;

/**
 * 向量存储接口
 * 
 * <p>核心层接口，定义向量存储的基本操作</p>
 * <p>由 infra 层实现具体的存储调用（如 Elasticsearch）</p>
 */
public interface VectorStore {
    
    /**
     * 添加切片
     * @param chunk 文本切片
     */
    void add(TextChunk chunk);
    
    /**
     * 批量添加切片
     * @param chunks 切片列表
     */
    void addBatch(List<TextChunk> chunks);
    
    /**
     * 向量相似度搜索
     * @param query 查询向量
     * @param topK 返回数量
     * @return 相似切片列表
     */
    List<TextChunk> similaritySearch(float[] query, int topK);
    
    /**
     * 向量相似度搜索（带过滤条件）
     * @param query 查询向量
     * @param topK 返回数量
     * @param knowledgeId 知识库ID
     * @return 相似切片列表
     */
    List<TextChunk> similaritySearch(float[] query, int topK, String knowledgeId);
    
    /**
     * 关键词搜索
     * @param query 查询关键词
     * @param topK 返回数量
     * @return 匹配切片列表
     */
    List<TextChunk> keywordSearch(String query, int topK);
    
    /**
     * 混合搜索（向量+关键词）
     * @param query 查询文本
     * @param queryVector 查询向量
     * @param topK 返回数量
     * @return 混合检索结果
     */
    List<TextChunk> hybridSearch(String query, float[] queryVector, int topK);
    
    /**
     * 删除知识库的所有向量
     * @param knowledgeId 知识库ID
     */
    void deleteByKnowledgeId(String knowledgeId);
    
    /**
     * 删除切片
     * @param chunkId 切片ID
     */
    void delete(String chunkId);
    
    /**
     * 检查索引是否存在
     */
    boolean indexExists();
    
    /**
     * 创建索引
     */
    void createIndex();
}
