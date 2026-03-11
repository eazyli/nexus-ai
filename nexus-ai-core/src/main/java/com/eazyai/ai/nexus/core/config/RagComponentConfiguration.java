package com.eazyai.ai.nexus.core.config;

import com.eazyai.ai.nexus.core.rag.DocumentSplitter;
import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import com.eazyai.ai.nexus.core.rag.RerankService;
import com.eazyai.ai.nexus.core.rag.VectorStore;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeProcessService;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeRepository;
import com.eazyai.ai.nexus.core.rag.parser.DocumentParserFactory;
import com.eazyai.ai.nexus.core.rag.splitter.Langchain4jDocumentSplitter;
import com.eazyai.ai.nexus.core.rag.splitter.Langchain4jOfficialSplitter;
import com.eazyai.ai.nexus.core.rag.splitter.RecursiveDocumentSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 组件自动配置
 * 
 * <p>注意：EmbeddingModel 的配置由 nexus-ai-model 模块提供</p>
 * 
 * <p>配置项：</p>
 * <ul>
 *   <li>ai.agent.rag.chunk-size: 切片大小（默认 500）</li>
 *   <li>ai.agent.rag.chunk-overlap: 切片重叠（默认 50）</li>
 *   <li>ai.agent.rag.splitter.type: 切片器类型，langchain4j-official, langchain4j 或 custom（默认 langchain4j）</li>
 *   <li>ai.agent.embedding.batch-size: Embedding 批量大小（默认 20）</li>
 * </ul>
 */
@Slf4j
@Configuration
public class RagComponentConfiguration {

    @Value("${ai.agent.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${ai.agent.rag.chunk-overlap:50}")
    private int chunkOverlap;

    @Value("${ai.agent.embedding.batch-size:20}")
    private int embeddingBatchSize;

    @Value("${ai.agent.rag.splitter.type:langchain4j}")
    private String splitterType;

    /**
     * 配置 DocumentSplitter (基于 LangChain4j 官方实现)
     * 
     * <p>推荐使用 LangChain4j 官方实现，经过充分测试，更稳定可靠</p>
     */
    @Bean
    @ConditionalOnMissingBean(DocumentSplitter.class)
    @ConditionalOnProperty(name = "ai.agent.rag.splitter.type", havingValue = "langchain4j-official")
    public DocumentSplitter langchain4jOfficialSplitter() {
        log.info("[RagComponentConfiguration] 配置 LangChain4j 官方 DocumentSplitter: chunkSize={}, chunkOverlap={}", 
                chunkSize, chunkOverlap);
        return new Langchain4jOfficialSplitter(chunkSize, chunkOverlap);
    }

    /**
     * 配置 DocumentSplitter (基于 Langchain4j 自定义实现)
     * 
     * <p>使用基于 Langchain4j 思路的自定义实现</p>
     */
    @Bean
    @ConditionalOnMissingBean(DocumentSplitter.class)
    @ConditionalOnProperty(name = "ai.agent.rag.splitter.type", havingValue = "langchain4j", matchIfMissing = true)
    public DocumentSplitter langchain4jDocumentSplitter() {
        log.info("[RagComponentConfiguration] 配置 Langchain4j DocumentSplitter: chunkSize={}, chunkOverlap={}", 
                chunkSize, chunkOverlap);
        return new Langchain4jDocumentSplitter(chunkSize, chunkOverlap);
    }

    /**
     * 配置 DocumentSplitter (自定义实现)
     * 
     * <p>使用自定义的 RecursiveDocumentSplitter 实现</p>
     */
    @Bean
    @ConditionalOnMissingBean(DocumentSplitter.class)
    @ConditionalOnProperty(name = "ai.agent.rag.splitter.type", havingValue = "custom")
    public DocumentSplitter customDocumentSplitter() {
        log.info("[RagComponentConfiguration] 配置自定义 DocumentSplitter: chunkSize={}, chunkOverlap={}", 
                chunkSize, chunkOverlap);
        return new RecursiveDocumentSplitter(chunkSize, chunkOverlap);
    }

    /**
     * 配置 KnowledgeProcessService
     * 
     * <p>核心层业务服务，负责文档处理、切片、向量化、检索等业务逻辑</p>
     */
    @Bean
    @ConditionalOnMissingBean(KnowledgeProcessService.class)
    public KnowledgeProcessService knowledgeProcessService(
            VectorStore vectorStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter,
            DocumentParserFactory parserFactory,
            KnowledgeRepository knowledgeRepository,
            @Autowired(required = false) RerankService rerankService) {
        
        log.info("[RagComponentConfiguration] 配置 KnowledgeProcessService, embeddingBatchSize={}", embeddingBatchSize);
        return new KnowledgeProcessService(
                vectorStore,
                embeddingModel,
                documentSplitter,
                parserFactory,
                rerankService,
                knowledgeRepository,
                embeddingBatchSize
        );
    }
}
