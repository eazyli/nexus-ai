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
 * 场景配置表
 */
@Getter
@Setter
@TableName(value = "ai_scene", autoResultMap = true)
public class AiScene {

    @TableId("scene_id")
    private String sceneId;

    private String sceneName;

    private String description;

    private String appId;

    /**
     * 场景类型: chat/rag/tool_calling/workflow/multi_agent
     */
    private String sceneType;

    /**
     * 启用的AI能力列表（逗号分隔）
     */
    private String abilityIds;

    /**
     * 可调用的工具列表（逗号分隔）
     */
    private String toolIds;

    /**
     * 绑定的知识库列表（逗号分隔）
     */
    private String knowledgeIds;

    /**
     * 场景配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    /**
     * 优先级（数值越大优先级越高）
     */
    private Integer priority;

    /**
     * 状态: 1启用/0禁用
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
