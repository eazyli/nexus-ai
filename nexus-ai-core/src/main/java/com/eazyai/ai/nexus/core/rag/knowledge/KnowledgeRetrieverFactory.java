package com.eazyai.ai.nexus.core.rag.knowledge;

import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.List;

/**
 * 知识库检索器工厂接口
 * 
 * <p>为智能体创建关联知识库的内容检索器</p>
 */
public interface KnowledgeRetrieverFactory {

    /**
     * 为应用创建知识库检索器
     * 
     * @param appId 应用ID
     * @return ContentRetriever（如果没有关联知识库则返回null）
     */
    ContentRetriever createForApp(String appId);

    /**
     * 为应用创建知识库检索器
     * 
     * @param appId 应用ID
     * @param maxResults 最大返回数量
     * @param minScore 最小相似度阈值
     * @return ContentRetriever（如果没有关联知识库则返回null）
     */
    ContentRetriever createForApp(String appId, int maxResults, double minScore);

    /**
     * 为指定知识库创建检索器
     * 
     * @param knowledgeIds 知识库ID列表
     * @return ContentRetriever
     */
    ContentRetriever createForKnowledgeBases(List<String> knowledgeIds);

    /**
     * 创建全局检索器（搜索所有知识库）
     */
    ContentRetriever createGlobalRetriever();
}
