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
 * MCP工具注册表
 */
@Getter
@Setter
@TableName(value = "ai_mcp_tool", autoResultMap = true)
public class AiMcpTool {

    @TableId("tool_id")
    private String toolId;

    private String toolName;

    /**
     * 工具类型: HTTP/DB/Dubbo/THIRD_PARTY_API
     */
    private String toolType;

    private String description;

    /**
     * 工具配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    private String appId;

    /**
     * 可见性: PRIVATE/PUBLIC/SHARED
     */
    private String visibility;

    /**
     * 状态: 1启用/0禁用
     */
    private Integer status;

    /**
     * 可调用该工具的AppId列表（逗号分隔）
     */
    private String permissionApps;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 重试间隔（ms）
     */
    private Integer retryInterval;

    /**
     * 超时时间（ms）
     */
    private Integer timeout;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
