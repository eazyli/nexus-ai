package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * 能力标签列表（用于多智能体匹配）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> capabilities;

    /**
     * 协作模式: single/react/plan_execute/supervisor
     */
    private String collaborationMode;

    /**
     * Agent执行配置
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> executionConfig;

    /**
     * 变量定义
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> variables;

    /**
     * 开场白
     */
    private String greeting;

    /**
     * 示例问题列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> sampleQuestions;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 图标URL
     */
    private String icon;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
