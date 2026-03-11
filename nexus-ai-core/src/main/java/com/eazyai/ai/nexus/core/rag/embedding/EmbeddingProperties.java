package com.eazyai.ai.nexus.core.rag.embedding;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Embedding 配置属性
 */
@Data
@ConfigurationProperties(prefix = "ai.agent.embedding")
public class EmbeddingProperties {

    /**
     * Embedding 类型: modelscope / openai / http
     */
    private String type = "modelscope";

    /**
     * API 基础地址
     */
    private String baseUrl = "https://api-inference.modelscope.cn/v1";

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model = "bge-large-zh-v1.5";

    /**
     * 向量维度
     */
    private int dimension = 1024;

    /**
     * 批量请求大小（调用 embedding API 时每批处理的文本数量）
     * 建议根据 API 限制设置，ModelScope 建议 10-20，OpenAI 建议 100-200
     */
    private int batchSize = 20;

    /**
     * 批量请求最大 token 数（用于控制每批的总 token 数）
     * 某些 API 有限制，如 OpenAI 限制每批最多 8191 tokens
     */
    private int maxTokensPerBatch = 8000;
}
