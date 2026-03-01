package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 应用管理表 - 业务系统配置
 */
@Getter
@Setter
@TableName(value = "ai_app", autoResultMap = true)
public class AiApp {

    @TableId("app_id")
    private String appId;

    private String appName;

    private String appSecret;

    private String tenantId;

    private String description;

    /**
     * 应用类型: chatbot/assistant/workflow/agent
     */
    private String appType;

    /**
     * 状态: 1启用/0禁用
     */
    private Integer status;

    /**
     * QPS限制
     */
    private Integer qpsLimit;

    /**
     * 日调用限额
     */
    private Integer dailyLimit;

    /**
     * 可使用的AI能力列表（逗号分隔）
     */
    private String abilityIds;

    /**
     * 默认模型ID
     */
    private String defaultModelId;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 温度参数
     */
    private BigDecimal temperature;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 扩展配置
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraConfig;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
