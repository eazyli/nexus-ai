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
 * AI能力配置表
 */
@Getter
@Setter
@TableName(value = "ai_ability", autoResultMap = true)
public class AiAbility {

    @TableId("ability_id")
    private String abilityId;

    private String abilityName;

    private String abilityDesc;

    private String abilityType;

    /**
     * 状态: 1启用/0禁用
     */
    private Integer status;

    /**
     * 能力配置JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    /**
     * 排序
     */
    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
