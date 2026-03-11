package com.eazyai.ai.nexus.core.config;

import com.eazyai.ai.nexus.core.rag.DocumentSplitter;
import com.eazyai.ai.nexus.core.rag.embedding.EmbeddingProperties;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeProcessService;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeRepository;
import com.eazyai.ai.nexus.core.rag.parser.DocumentParserFactory;
import com.eazyai.ai.nexus.core.rag.splitter.Langchain4jOfficialSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG配置类
 * 
 * <p>配置RAG相关的组件</p>
 */
@Slf4j
@Configuration
public class RagConfig {

    @Value("${ai.agent.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${ai.agent.rag.chunk-overlap:50}")
    private int chunkOverlap;

    @Value("${ai.agent.embedding.batch-size:20}")
    private int embeddingBatchSize;

    @Bean
    public DocumentSplitter documentSplitter() {
        log.info("[RagConfig] 初始化文档切片器, chunkSize: {}, chunkOverlap: {}", chunkSize, chunkOverlap);
        return new Langchain4jOfficialSplitter(chunkSize, chunkOverlap);
    }

    @Bean
    public DocumentParserFactory documentParserFactory() {
        return new DocumentParserFactory();
    }
}
