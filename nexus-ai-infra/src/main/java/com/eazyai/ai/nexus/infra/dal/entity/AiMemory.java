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
 * 记忆表 - 短期+长期记忆
 */
@Getter
@Setter
@TableName(value = "ai_memory", autoResultMap = true)
public class AiMemory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String appId;

    private String sessionId;

    /**
     * 记忆类型: short/long/business
     */
    private String memoryType;

    /**
     * 角色: user/assistant/system/tool
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 扩展数据JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> memoryData;

    /**
     * Token数量
     */
    private Integer tokenCount;

    /**
     * 过期时间（短期记忆必填）
     */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
