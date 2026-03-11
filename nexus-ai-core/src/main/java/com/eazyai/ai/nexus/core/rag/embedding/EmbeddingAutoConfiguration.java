package com.eazyai.ai.nexus.core.rag.embedding;

import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 服务自动配置
 * 
 * <p>支持多种 Embedding 实现：</p>
 * <ul>
 *   <li>modelscope: 使用 ModelScope API（默认）</li>
 *   <li>spring-ai-openai: 使用 Spring AI 的 OpenAI 实现</li>
 *   <li>spring-ai-custom: 使用自定义的 Spring AI EmbeddingModel Bean</li>
 * </ul>
 * 
 * <p>配置示例：</p>
 * <pre>
 * # 使用 ModelScope
 * ai.agent.embedding.type=modelscope
 * ai.agent.embedding.api-key=your-api-key
 * ai.agent.embedding.batch-size=20
 * 
 * # 使用 Spring AI OpenAI
 * ai.agent.embedding.type=spring-ai-openai
 * spring.ai.openai.api-key=your-openai-key
 * spring.ai.openai.embedding.options.model=text-embedding-ada-002
 * </pre>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
@RequiredArgsConstructor
public class EmbeddingAutoConfiguration {

    /**
     * ModelScope Embedding 模型
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnProperty(prefix = "ai.agent.embedding", name = "type", havingValue = "modelscope", matchIfMissing = true)
    public EmbeddingModel modelScopeEmbeddingModel(EmbeddingProperties properties) {
        log.info("[EmbeddingAutoConfiguration] 初始化 ModelScope Embedding 模型, batchSize={}", properties.getBatchSize());
        return new ModelScopeEmbeddingModel(properties);
    }

    /**
     * Spring AI OpenAI Embedding 适配器
     * 
     * <p>需要配置 spring.ai.openai.api-key</p>
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnProperty(prefix = "ai.agent.embedding", name = "type", havingValue = "spring-ai-openai")
    @ConditionalOnBean(OpenAiEmbeddingModel.class)
    public EmbeddingModel springAiOpenAiEmbeddingModel(
            OpenAiEmbeddingModel openAiEmbeddingModel,
            EmbeddingProperties properties) {
        
        log.info("[EmbeddingAutoConfiguration] 使用 Spring AI OpenAI Embedding 模型");
        return new SpringAIEmbeddingModel(
                openAiEmbeddingModel,
                properties.getModel()
        );
    }

    /**
     * 自定义 Spring AI Embedding 适配器
     * 
     * <p>当用户提供了自己的 org.springframework.ai.embedding.EmbeddingModel Bean 时使用</p>
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnProperty(prefix = "ai.agent.embedding", name = "type", havingValue = "spring-ai-custom")
    public EmbeddingModel springAiCustomEmbeddingModel(
            @Autowired org.springframework.ai.embedding.EmbeddingModel customEmbeddingModel,
            EmbeddingProperties properties) {
        
        log.info("[EmbeddingAutoConfiguration] 使用自定义 Spring AI Embedding 模型");
        return new SpringAIEmbeddingModel(
                customEmbeddingModel,
                properties.getModel()
        );
    }
}
