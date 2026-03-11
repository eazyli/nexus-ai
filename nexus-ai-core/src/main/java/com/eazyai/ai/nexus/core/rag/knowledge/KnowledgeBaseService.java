package com.eazyai.ai.nexus.core.rag.knowledge;

import java.util.List;
import java.util.Map;

/**
 * 知识库服务接口
 * 
 * <p>定义知识库管理、文档处理、检索的核心接口</p>
 * <p>实现类在 infra 层，负责调用 core 层的 KnowledgeProcessService 完成业务逻辑</p>
 */
public interface KnowledgeBaseService {

    // ==================== 知识库管理 ====================

    /**
     * 创建知识库
     * 
     * @param knowledgeName 知识库名称
     * @param appId 所属应用ID（可选）
     * @param config 配置参数
     * @return 知识库ID
     */
    String createKnowledge(String knowledgeName, String appId, Map<String, Object> config);

    /**
     * 获取知识库详情
     */
    KnowledgeBaseInfo getKnowledge(String knowledgeId);

    /**
     * 获取应用关联的知识库列表
     */
    List<KnowledgeBaseInfo> listByAppId(String appId);

    /**
     * 删除知识库
     */
    void deleteKnowledge(String knowledgeId);

    /**
     * 更新知识库状态
     */
    void updateStatus(String knowledgeId, int status, int progress);

    // ==================== 文档管理 ====================

    /**
     * 添加文档到知识库
     * 
     * @param knowledgeId 知识库ID
     * @param content 文档内容
     * @param fileName 文件名
     * @return 文档ID
     */
    Long addDocument(String knowledgeId, byte[] content, String fileName);

    /**
     * 添加文本到知识库
     * 
     * @param knowledgeId 知识库ID
     * @param text 文本内容
     * @param metadata 元数据
     * @return 切片数量
     */
    int addText(String knowledgeId, String text, Map<String, Object> metadata);

    /**
     * 删除文档
     */
    void deleteDocument(String knowledgeId, Long docId);

    // ==================== 检索 ====================

    /**
     * 从知识库检索
     * 
     * @param query 查询文本
     * @param knowledgeIds 知识库ID列表
     * @param topK 返回数量
     * @return 检索结果
     */
    RetrievalResult retrieve(String query, List<String> knowledgeIds, int topK);

    /**
     * 从应用关联的知识库检索
     * 
     * @param query 查询文本
     * @param appId 应用ID
     * @param topK 返回数量
     * @return 检索结果
     */
    RetrievalResult retrieveByApp(String query, String appId, int topK);

    // ==================== 数据模型 ====================

    /**
     * 知识库信息
     */
    record KnowledgeBaseInfo(
            String knowledgeId,
            String knowledgeName,
            String knowledgeType,
            String appId,
            int status,
            int docCount,
            int chunkCount
    ) {}

    /**
     * 检索结果
     */
    record RetrievalResult(
            String query,
            List<ChunkInfo> chunks,
            long totalTime,
            String searchType
    ) {
        /**
         * 获取拼接后的上下文
         */
        public String getContext() {
            if (chunks == null || chunks.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                if (i > 0) {
                    sb.append("\n\n");
                }
                sb.append(chunks.get(i).content());
            }
            return sb.toString();
        }
    }

    /**
     * 切片信息
     */
    record ChunkInfo(
            String chunkId,
            String content,
            double score,
            String knowledgeId,
            Map<String, Object> metadata
    ) {}
}
