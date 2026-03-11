package com.eazyai.ai.nexus.core.rag;

import java.util.List;

/**
 * 重排序服务接口
 * 
 * <p>定义文档重排序能力，由 infra 层实现具体的重排序逻辑</p>
 * <p>重排序用于在检索结果中进行精细排序，提升检索相关性</p>
 */
public interface RerankService {

    /**
     * 对文档进行重排序（使用默认topK）
     *
     * @param query     查询文本
     * @param documents 待重排序的文档列表
     * @return 重排序后的结果列表
     */
    List<RerankResult> rerank(String query, List<RerankDocument> documents);

    /**
     * 对文档进行重排序
     *
     * @param query     查询文本
     * @param documents 待重排序的文档列表
     * @param topK      返回的最相关文档数量
     * @return 重排序后的结果列表
     */
    List<RerankResult> rerank(String query, List<RerankDocument> documents, int topK);

    /**
     * 待重排序的文档
     *
     * @param id            文档ID
     * @param content       文档内容
     * @param initialScore  初始分数（来自向量检索）
     */
    record RerankDocument(String id, String content, float initialScore) {}

    /**
     * 重排序结果
     *
     * @param documentId    文档ID
     * @param content       文档内容
     * @param initialScore  初始分数
     * @param rerankScore   重排序分数
     * @param originalIndex 在原始列表中的索引
     */
    record RerankResult(
            String documentId,
            String content,
            double initialScore,
            double rerankScore,
            int originalIndex
    ) {}
}
