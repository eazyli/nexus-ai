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
 * 插件表
 */
@Getter
@Setter
@TableName(value = "ai_plugin", autoResultMap = true)
public class AiPlugin {

    @TableId("plugin_id")
    private String pluginId;

    private String pluginName;

    private String pluginVersion;

    private String description;

    private String pluginType;

    /**
     * 主类全路径
     */
    private String mainClass;

    /**
     * 配置Schema
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> configSchema;

    /**
     * 状态: 1启用/0禁用
     */
    private Integer status;

    private LocalDateTime installTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
