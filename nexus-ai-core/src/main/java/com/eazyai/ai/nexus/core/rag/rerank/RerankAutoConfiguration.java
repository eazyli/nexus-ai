package com.eazyai.ai.nexus.core.rag.rerank;

import com.eazyai.ai.nexus.core.rag.RerankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rerank 服务自动配置
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RerankProperties.class)
@RequiredArgsConstructor
public class RerankAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "ai.agent.rerank", name = "enabled", havingValue = "true")
    public RerankService rerankService(RerankProperties properties) {
        log.info("[RerankAutoConfiguration] 初始化 Rerank 服务, type: {}", properties.getType());
        
        return switch (properties.getType().toLowerCase()) {
            case "modelscope" -> {
                try {
                    yield new ModelScopeRerankService(properties);
                } catch (Exception e) {
                    log.error("[RerankAutoConfiguration] ModelScope Rerank 服务初始化失败: {}", e.getMessage(), e);
                    throw new RuntimeException("ModelScope Rerank 服务初始化失败", e);
                }
            }
            case "dashscope" -> {
                try {
                    yield new DashScopeRerankService(properties);
                } catch (Exception e) {
                    log.error("[RerankAutoConfiguration] DashScope Rerank 服务初始化失败: {}", e.getMessage(), e);
                    throw new RuntimeException("DashScope Rerank 服务初始化失败", e);
                }
            }
            default -> throw new IllegalArgumentException("不支持的 Rerank 类型: " + properties.getType());
        };
    }
}
