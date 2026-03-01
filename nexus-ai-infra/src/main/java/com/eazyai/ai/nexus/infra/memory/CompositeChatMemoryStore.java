package com.eazyai.ai.nexus.infra.memory;

import com.eazyai.ai.nexus.core.memory.ChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 组合型会话记忆存储
 * 实现二级缓存策略：Redis作为一级缓存，MySQL作为二级存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeChatMemoryStore implements ChatMemoryStore {

    private final JdbcChatMemoryStore jdbcStore;
    private final RedisChatMemoryStore redisStore;

    // 会话过期时间
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        // 先从Redis获取
        List<ChatMessage> messages = redisStore.getMessages(sessionId);

        if (messages.isEmpty()) {
            // Redis中没有，从MySQL加载
            messages = jdbcStore.getMessages(sessionId);

            if (!messages.isEmpty()) {
                // 回填到Redis缓存
                redisStore.updateMessagesWithTtl(sessionId, messages, SESSION_TTL);
                log.debug("[CompositeChatMemoryStore] 从MySQL加载并缓存: sessionId={}, count={}", 
                    sessionId, messages.size());
            }
        } else {
            log.debug("[CompositeChatMemoryStore] 从Redis缓存命中: sessionId={}, count={}", 
                sessionId, messages.size());
        }

        return messages;
    }

    @Override
    public void updateMessages(String sessionId, List<ChatMessage> messages) {
        // 同时写入MySQL和Redis
        jdbcStore.updateMessages(sessionId, messages);
        redisStore.updateMessagesWithTtl(sessionId, messages, SESSION_TTL);
        log.debug("[CompositeChatMemoryStore] 双写存储: sessionId={}, count={}", sessionId, messages.size());
    }

    @Override
    public void deleteMessages(String sessionId) {
        // 同时删除MySQL和Redis
        jdbcStore.deleteMessages(sessionId);
        redisStore.deleteMessages(sessionId);
        log.debug("[CompositeChatMemoryStore] 双删: sessionId={}", sessionId);
    }

    @Override
    public boolean exists(String sessionId) {
        // 先查Redis
        if (redisStore.exists(sessionId)) {
            return true;
        }
        // 再查MySQL
        return jdbcStore.exists(sessionId);
    }

    @Override
    public void refreshSession(String sessionId) {
        redisStore.refreshTtl(sessionId, SESSION_TTL);
    }

    /**
     * 仅更新MySQL（用于异步持久化场景）
     */
    public void persistToMysql(String sessionId, List<ChatMessage> messages) {
        jdbcStore.updateMessages(sessionId, messages);
    }

    /**
     * 仅更新Redis缓存
     */
    public void updateCache(String sessionId, List<ChatMessage> messages) {
        redisStore.updateMessagesWithTtl(sessionId, messages, SESSION_TTL);
    }
}
