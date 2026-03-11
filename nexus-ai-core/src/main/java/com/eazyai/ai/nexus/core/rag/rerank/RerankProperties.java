package com.eazyai.ai.nexus.core.rag.rerank;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rerank 配置属性
 */
@Data
@ConfigurationProperties(prefix = "ai.agent.rerank")
public class RerankProperties {

    /**
     * 是否启用重排序
     */
    private boolean enabled = false;

    /**
     * 重排序类型: modelscope / http
     */
    private String type = "modelscope";

    /**
     * API 基础地址（用于 modelscope 类型）
     */
    private String baseUrl = "https://api-inference.modelscope.cn/v1";

    /**
     * API Key（用于 modelscope 类型）
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model = "bge-reranker-v2-m3";

    /**
     * 重排序后返回的文档数量（TopK）
     */
    private int topK = 5;
}
