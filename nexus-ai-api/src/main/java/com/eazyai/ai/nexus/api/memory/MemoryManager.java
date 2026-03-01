package com.eazyai.ai.nexus.api.memory;

import com.eazyai.ai.nexus.api.dto.AgentContext;

import java.util.List;

/**
 * 记忆管理器接口
 * 负责短期和长期记忆的存储与检索
 */
public interface MemoryManager {

    /**
     * 存储短期记忆（会话级别）
     *
     * @param sessionId 会话ID
     * @param key 键
     * @param value 值
     * @param ttl 过期时间（秒）
     */
    void storeShortTerm(String sessionId, String key, Object value, long ttl);

    /**
     * 获取短期记忆
     *
     * @param sessionId 会话ID
     * @param key 键
     * @return 值
     */
    <T> T getShortTerm(String sessionId, String key);

    /**
     * 存储长期记忆（持久化）
     *
     * @param userId 用户ID
     * @param memory 记忆内容
     */
    void storeLongTerm(String userId, MemoryEntry memory);

    /**
     * 检索长期记忆
     *
     * @param userId 用户ID
     * @param query 查询条件
     * @param limit 返回数量限制
     * @return 记忆条目列表
     */
    List<MemoryEntry> retrieveLongTerm(String userId, String query, int limit);

    /**
     * 基于向量检索记忆
     *
     * @param userId 用户ID
     * @param embedding 向量嵌入
     * @param limit 返回数量限制
     * @return 记忆条目列表
     */
    List<MemoryEntry> retrieveByVector(String userId, float[] embedding, int limit);

    /**
     * 清除会话记忆
     *
     * @param sessionId 会话ID
     */
    void clearSession(String sessionId);

    /**
     * 获取会话历史
     *
     * @param sessionId 会话ID
     * @return 历史记录列表
     */
    List<MessageHistory> getSessionHistory(String sessionId);

    /**
     * 添加消息到历史
     *
     * @param sessionId 会话ID
     * @param role 角色（user/assistant）
     * @param content 内容
     */
    void addMessage(String sessionId, String role, String content);

    /**
     * 记忆条目
     */
    interface MemoryEntry {
        String getId();
        String getType();
        String getContent();
        long getTimestamp();
        float[] getEmbedding();
        Object getMetadata();
    }

    /**
     * 消息历史
     */
    interface MessageHistory {
        String getRole();
        String getContent();
        long getTimestamp();
    }
}
