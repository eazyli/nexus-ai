package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 会话表
 */
@Getter
@Setter
@TableName("ai_session")
public class AiSession {

    @TableId("session_id")
    private String sessionId;

    private String appId;

    private String userId;

    private String sceneId;

    private String title;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 最后一条消息
     */
    private String lastMessage;

    /**
     * 状态: 1活跃/0归档
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
