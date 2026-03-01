package com.eazyai.ai.nexus.web.config;

import com.eazyai.ai.nexus.tools.CalculatorTools;
import com.eazyai.ai.nexus.tools.RagTools;
import com.eazyai.ai.nexus.tools.SearchTools;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智能体配置类
 */
@Configuration
public class AgentConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String modelName;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    /**
     * 配置LangChain4j ChatLanguageModel
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
    }

    /**
     * 配置LangChain4j StreamingChatLanguageModel
     * 用于流式输出
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
    }

    /**
     * 将工具类注册为Bean
     */
    @Bean
    public SearchTools searchTools() {
        return new SearchTools();
    }

    @Bean
    public CalculatorTools calculatorTools() {
        return new CalculatorTools();
    }

    @Bean
    public RagTools ragTools() {
        return new RagTools();
    }
}
