package com.eazyai.ai.nexus.core.rag.knowledge;

import com.eazyai.ai.nexus.core.rag.model.TextChunk;

import java.util.List;
import java.util.Map;

/**
 * 知识库数据访问接口
 * 
 * <p>定义知识库相关的数据访问操作，由 infra 层实现</p>
 * <p>core 层通过此接口访问数据，不直接依赖 infra 层的具体实现</p>
 */
public interface KnowledgeRepository {

    // ==================== 知识库管理 ====================

    /**
     * 保存知识库
     */
    KnowledgeInfo saveKnowledge(KnowledgeInfo info);

    /**
     * 查询知识库
     */
    KnowledgeInfo findKnowledgeById(String knowledgeId);

    /**
     * 查询应用的知识库列表
     */
    List<KnowledgeInfo> findKnowledgeByAppId(String appId);

    /**
     * 删除知识库
     */
    void deleteKnowledgeById(String knowledgeId);

    /**
     * 更新知识库状态
     */
    void updateStatus(String knowledgeId, int status, int progress);

    /**
     * 增加切片计数
     */
    void incrementChunkCount(String knowledgeId, int increment);

    /**
     * 增加文档计数
     */
    void incrementDocCount(String knowledgeId, int increment);

    // ==================== 文档管理 ====================

    /**
     * 保存文档
     */
    DocInfo saveDocument(DocInfo info);

    /**
     * 更新文档状态
     */
    void updateDocumentStatus(Long docId, int status, Integer chunkCount, String errorMsg);

    /**
     * 删除文档
     */
    void deleteDocumentById(Long docId);

    /**
     * 查询文档列表
     */
    List<DocInfo> findDocumentsByKnowledgeId(String knowledgeId);

    // ==================== 切片管理 ====================

    /**
     * 批量保存切片
     */
    void saveChunks(List<TextChunk> chunks, Long docId);

    /**
     * 查询切片列表
     */
    List<TextChunk> findChunksByKnowledgeId(String knowledgeId);

    /**
     * 查询文档的切片列表
     */
    List<TextChunk> findChunksByDocId(Long docId);

    /**
     * 删除知识库的所有切片
     */
    void deleteChunksByKnowledgeId(String knowledgeId);

    /**
     * 删除文档的所有切片
     */
    int deleteChunksByDocId(Long docId);

    /**
     * 查询文档的所有切片ID（用于删除ES数据）
     */
    List<String> findChunkIdsByDocId(Long docId);

    // ==================== 数据模型 ====================

    /**
     * 知识库信息
     */
    record KnowledgeInfo(
            String knowledgeId,
            String knowledgeName,
            String knowledgeType,
            String appId,
            String description,
            String embeddingModel,
            Integer chunkSize,
            Integer chunkOverlap,
            Integer status,
            Integer processProgress,
            Integer docCount,
            Integer chunkCount,
            Map<String, Object> vectorDbConfig
    ) {}

    /**
     * 文档信息
     */
    record DocInfo(
            Long id,
            String knowledgeId,
            String fileName,
            Long fileSize,
            String fileType,
            Integer status,
            Integer chunkCount,
            String errorMsg
    ) {}
}
