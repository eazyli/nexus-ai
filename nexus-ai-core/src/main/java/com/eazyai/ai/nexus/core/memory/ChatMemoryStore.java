package com.eazyai.ai.nexus.core.memory;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 会话记忆存储接口
 * 支持多种存储后端（MySQL、Redis等）
 */
public interface ChatMemoryStore {

    /**
     * 获取会话消息列表
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> getMessages(String sessionId);

    /**
     * 更新会话消息
     *
     * @param sessionId 会话ID
     * @param messages  消息列表
     */
    void updateMessages(String sessionId, List<ChatMessage> messages);

    /**
     * 删除会话消息
     *
     * @param sessionId 会话ID
     */
    void deleteMessages(String sessionId);

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    boolean exists(String sessionId);

    /**
     * 刷新会话过期时间
     * 用于延长会话的生命周期
     *
     * @param sessionId 会话ID
     */
    default void refreshSession(String sessionId) {
        // 默认空实现，子类可覆盖
    }
}
