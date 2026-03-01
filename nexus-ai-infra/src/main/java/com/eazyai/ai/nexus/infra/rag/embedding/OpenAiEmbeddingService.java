package com.eazyai.ai.nexus.infra.rag.embedding;

import com.eazyai.ai.nexus.infra.rag.EmbeddingService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI/DeepSeek 向量嵌入服务
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.agent.embedding.type", havingValue = "openai", matchIfMissing = true)
public class OpenAiEmbeddingService implements EmbeddingService {

    @Value("${ai.agent.embedding.api-key:}")
    private String apiKey;

    @Value("${ai.agent.embedding.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.agent.embedding.model:text-embedding-ada-002}")
    private String modelName;

    @Value("${ai.agent.embedding.dimension:1536}")
    private int dimension;

    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
            log.info("Embedding模型初始化成功: {} @ {}", modelName, baseUrl);
        } else {
            log.warn("未配置Embedding API Key，向量嵌入功能将不可用");
        }
    }

    @Override
    public float[] embed(String text) {
        if (embeddingModel == null) {
            throw new IllegalStateException("Embedding模型未初始化");
        }

        try {
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(text).content();
            return embedding.vector();
        } catch (Exception e) {
            log.error("生成向量失败: {}", text.substring(0, Math.min(50, text.length())), e);
            throw new RuntimeException("生成向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (embeddingModel == null) {
            throw new IllegalStateException("Embedding模型未初始化");
        }

        List<float[]> embeddings = new ArrayList<>();
        
        // LangChain4j 批量嵌入
        try {
            List<dev.langchain4j.data.embedding.Embedding> results = 
                    embeddingModel.embedAll(texts.stream()
                            .map(dev.langchain4j.data.segment.TextSegment::from)
                            .toList())
                            .content();
            
            for (dev.langchain4j.data.embedding.Embedding embedding : results) {
                embeddings.add(embedding.vector());
            }
            
            return embeddings;
        } catch (Exception e) {
            log.error("批量生成向量失败", e);
            // 降级为逐个处理
            for (String text : texts) {
                try {
                    embeddings.add(embed(text));
                } catch (Exception ex) {
                    log.warn("单个文本嵌入失败，跳过: {}", text.substring(0, Math.min(50, text.length())));
                    embeddings.add(new float[dimension]); // 返回空向量
                }
            }
            return embeddings;
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
