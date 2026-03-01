package com.eazyai.ai.nexus.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型描述符
 * 定义模型的元信息和能力
 */
@Data
@Builder
public class ModelDescriptor {

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型提供商 (openai, azure, anthropic, ollama, qwen, deepseek)
     */
    private String provider;

    /**
     * 模型类型 (chat, embedding, rerank, image)
     */
    private String type;

    /**
     * 模型能力列表
     */
    private List<ModelCapability> capabilities;

    /**
     * 上下文窗口大小
     */
    private Integer contextWindow;

    /**
     * 最大输出token数
     */
    private Integer maxOutputTokens;

    /**
     * 是否支持流式输出
     */
    private Boolean supportsStreaming;

    /**
     * 是否支持函数调用
     */
    private Boolean supportsFunctionCalling;

    /**
     * 是否支持视觉能力
     */
    private Boolean supportsVision;

    /**
     * 输入价格 (每1K token, 美元)
     */
    private Double inputPrice;

    /**
     * 输出价格 (每1K token, 美元)
     */
    private Double outputPrice;

    /**
     * 模型配置参数
     */
    private Map<String, Object> config;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 优先级 (用于负载均衡)
     */
    private Integer priority;

    /**
     * 模型描述
     */
    private String description;
}
