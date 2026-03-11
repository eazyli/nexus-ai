package com.eazyai.ai.nexus.core.rag.knowledge;

import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import com.eazyai.ai.nexus.core.rag.RerankService;
import com.eazyai.ai.nexus.core.rag.VectorStore;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库检索器工厂实现
 * 
 * <p>为智能体创建关联知识库的内容检索器</p>
 * <p>支持混合检索和 Rerank 重排序</p>
 * 
 * <p>属于核心层业务逻辑</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetrieverFactoryImpl implements KnowledgeRetrieverFactory {

    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    /**
     * Rerank 服务（可选注入）
     */
    @Autowired(required = false)
    private RerankService rerankService;

    @Override
    public ContentRetriever createForApp(String appId) {
        return createForApp(appId, 5, 0.3);
    }

    @Override
    public ContentRetriever createForApp(String appId, int maxResults, double minScore) {
        List<KnowledgeBaseService.KnowledgeBaseInfo> knowledgeBases = 
                knowledgeBaseService.listByAppId(appId);
        
        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            log.debug("[KnowledgeRetrieverFactory] 应用 {} 没有关联知识库", appId);
            return null;
        }
        
        List<String> knowledgeIds = knowledgeBases.stream()
                .map(KnowledgeBaseService.KnowledgeBaseInfo::knowledgeId)
                .collect(Collectors.toList());
        
        log.info("[KnowledgeRetrieverFactory] 为应用 {} 创建知识库检索器, 知识库数量: {}, Rerank: {}", 
                appId, knowledgeIds.size(), rerankService != null ? "启用" : "未启用");
        
        return KnowledgeContentRetriever.builder()
                .vectorStore(vectorStore)
                .embeddingModel(embeddingModel)
                .knowledgeIds(knowledgeIds)
                .maxResults(maxResults)
                .minScore(minScore)
                .useHybridSearch(true)
                .rerankService(rerankService)
                .rerankTopK(maxResults)
                .build();
    }

    @Override
    public ContentRetriever createForKnowledgeBases(List<String> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return null;
        }
        
        return KnowledgeContentRetriever.builder()
                .vectorStore(vectorStore)
                .embeddingModel(embeddingModel)
                .knowledgeIds(knowledgeIds)
                .maxResults(5)
                .minScore(0.3)
                .useHybridSearch(true)
                .rerankService(rerankService)
                .rerankTopK(5)
                .build();
    }

    @Override
    public ContentRetriever createGlobalRetriever() {
        return KnowledgeContentRetriever.builder()
                .vectorStore(vectorStore)
                .embeddingModel(embeddingModel)
                .knowledgeIds(Collections.emptyList()) // 空列表表示搜索所有
                .maxResults(5)
                .minScore(0.3)
                .useHybridSearch(true)
                .rerankService(rerankService)
                .rerankTopK(5)
                .build();
    }
}
