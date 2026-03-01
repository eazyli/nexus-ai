package com.eazyai.ai.nexus.infra.memory;

import com.eazyai.ai.nexus.core.memory.ChatMemoryStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于Redis的会话记忆存储
 * 提供高性能的会话消息缓存
 */
@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        if (redisTemplate == null) {
            log.warn("[RedisChatMemoryStore] Redis未配置，返回空消息列表");
            return new ArrayList<>();
        }

        String key = getKey(sessionId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<ChatMessage> messages = objectMapper.readValue(json, 
                new TypeReference<List<ChatMessage>>() {});
            log.debug("[RedisChatMemoryStore] 加载会话消息: sessionId={}, count={}", sessionId, messages.size());
            return messages;
        } catch (JsonProcessingException e) {
            log.error("[RedisChatMemoryStore] 反序列化消息失败: sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(String sessionId, List<ChatMessage> messages) {
        if (redisTemplate == null) {
            log.warn("[RedisChatMemoryStore] Redis未配置，跳过保存");
            return;
        }

        String key = getKey(sessionId);
        try {
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL);
            log.debug("[RedisChatMemoryStore] 保存会话消息: sessionId={}, count={}", sessionId, messages.size());
        } catch (JsonProcessingException e) {
            log.error("[RedisChatMemoryStore] 序列化消息失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public void deleteMessages(String sessionId) {
        if (redisTemplate == null) {
            return;
        }

        String key = getKey(sessionId);
        redisTemplate.delete(key);
        log.debug("[RedisChatMemoryStore] 删除会话消息: sessionId={}", sessionId);
    }

    @Override
    public boolean exists(String sessionId) {
        if (redisTemplate == null) {
            return false;
        }

        String key = getKey(sessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void refreshSession(String sessionId) {
        refreshTtl(sessionId, DEFAULT_TTL);
    }

    /**
     * 刷新会话过期时间
     */
    public void refreshTtl(String sessionId, Duration ttl) {
        if (redisTemplate == null) {
            return;
        }

        String key = getKey(sessionId);
        redisTemplate.expire(key, ttl);
    }

    /**
     * 设置自定义过期时间
     */
    public void updateMessagesWithTtl(String sessionId, List<ChatMessage> messages, Duration ttl) {
        if (redisTemplate == null) {
            return;
        }

        String key = getKey(sessionId);
        try {
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("[RedisChatMemoryStore] 序列化消息失败", e);
        }
    }

    private String getKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
