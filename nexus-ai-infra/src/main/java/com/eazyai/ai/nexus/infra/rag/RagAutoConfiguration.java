package com.eazyai.ai.nexus.infra.rag;

import com.eazyai.ai.nexus.infra.rag.embedding.OpenAiEmbeddingService;
import com.eazyai.ai.nexus.infra.rag.parser.DocumentParserFactory;
import com.eazyai.ai.nexus.infra.rag.parser.TxtDocumentParser;
import com.eazyai.ai.nexus.infra.rag.splitter.RecursiveCharacterTextSplitter;
import com.eazyai.ai.nexus.infra.rag.store.InMemoryVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * RAG自动配置
 */
@Configuration
public class RagAutoConfiguration {

    @Value("${ai.agent.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${ai.agent.rag.chunk-overlap:50}")
    private int chunkOverlap;

    @Bean
    public TxtDocumentParser txtDocumentParser() {
        return new TxtDocumentParser();
    }

    @Bean
    public DocumentParserFactory documentParserFactory(List<DocumentParser> parsers) {
        return new DocumentParserFactory(parsers);
    }

    @Bean
    public TextSplitter textSplitter() {
        return new RecursiveCharacterTextSplitter(chunkSize, chunkOverlap);
    }

    @Bean
    public InMemoryVectorStore inMemoryVectorStore() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.createIndex();
        return store;
    }
}
