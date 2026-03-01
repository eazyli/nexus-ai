package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI调用日志表
 */
@Getter
@Setter
@TableName(value = "ai_call_log", autoResultMap = true)
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private String appId;

    private String sceneId;

    private String sessionId;

    private String userId;

    /**
     * 用户输入
     */
    private String query;

    /**
     * 调用结果JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> result;

    /**
     * 自然语言回复
     */
    private String naturalResponse;

    /**
     * 使用的AI能力列表
     */
    private String usedAbilities;

    /**
     * 使用的工具列表
     */
    private String usedTools;

    /**
     * 执行步骤数
     */
    private Integer executionSteps;

    /**
     * 执行耗时（ms）
     */
    private Integer executionTime;

    /**
     * 输入Token数
     */
    private Integer tokenInput;

    /**
     * 输出Token数
     */
    private Integer tokenOutput;

    /**
     * 调用状态: 1成功/0失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    private String clientIp;

    private LocalDateTime createTime;
}
