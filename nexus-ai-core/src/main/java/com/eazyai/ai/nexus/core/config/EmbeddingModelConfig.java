package com.eazyai.ai.nexus.core.config;

import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import com.eazyai.ai.nexus.core.rag.embedding.EmbeddingProperties;
import com.eazyai.ai.nexus.core.rag.embedding.ModelScopeEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding模型配置
 * 
 * <p>根据配置类型自动创建对应的EmbeddingModel实现</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingModelConfig {

    @Bean
    public EmbeddingModel embeddingModel(EmbeddingProperties properties) {
        String type = properties.getType();
        log.info("[EmbeddingModelConfig] 初始化EmbeddingModel, type: {}", type);
        
        if ("openai".equalsIgnoreCase(type) || "modelscope".equalsIgnoreCase(type)) {
            // openai 和 modelscope 都使用相同的实现（都兼容OpenAI格式）
            log.info("[EmbeddingModelConfig] 使用 OpenAI兼容接口 (ModelScope/阿里云DashScope)");
            return new ModelScopeEmbeddingModel(properties);
        } else {
            throw new IllegalArgumentException("不支持的Embedding类型: " + type + 
                    ", 支持的类型: openai, modelscope");
        }
    }
}
