package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 模型配置表
 */
@Getter
@Setter
@TableName(value = "ai_model_config", autoResultMap = true)
public class AiModelConfig {

    @TableId("model_id")
    private String modelId;

    private String modelName;

    /**
     * 模型提供商: openai/azure/anthropic/qwen/deepseek
     */
    private String provider;

    /**
     * 模型类型: chat/embedding/rerank
     */
    private String modelType;

    /**
     * API密钥（加密存储）
     */
    private String apiKey;

    /**
     * API地址
     */
    private String apiBase;

    /**
     * 模型版本
     */
    private String modelVersion;

    /**
     * 最大上下文长度
     */
    private Integer maxContext;

    /**
     * 模型配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    /**
     * 状态: 1启用/0禁用
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
