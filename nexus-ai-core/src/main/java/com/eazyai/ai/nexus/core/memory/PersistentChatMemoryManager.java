package com.eazyai.ai.nexus.core.memory;

import com.eazyai.ai.nexus.core.config.NexusProperties;
import dev.langchain4j.memory.ChatMemory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 持久化会话记忆管理器
 * 管理会话记忆的创建、获取和清理
 * 
 * <p>特性：</p>
 * <ul>
 *   <li>会话过期自动清理</li>
 *   <li>最大会话数限制</li>
 *   <li>可配置的消息窗口大小</li>
 *   <li>定时清理过期会话</li>
 * </ul>
 */
@Slf4j
@Component
public class PersistentChatMemoryManager {

    private final ChatMemoryStore chatMemoryStore;
    private final NexusProperties.SessionProperties sessionProperties;
    
    // 内存中的ChatMemory缓存
    private final Map<String, SessionWrapper> memoryCache = new ConcurrentHashMap<>();
    
    // 定时清理任务
    private ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> cleanupTask;

    public PersistentChatMemoryManager(ChatMemoryStore chatMemoryStore, 
                                        NexusProperties nexusProperties) {
        this.chatMemoryStore = chatMemoryStore;
        this.sessionProperties = nexusProperties.getSession();
    }

    @PostConstruct
    public void init() {
        log.info("[PersistentChatMemoryManager] 初始化: expirationEnabled={}, expirationMinutes={}, " +
                "cleanupIntervalMinutes={}, maxSessions={}",
            sessionProperties.isExpirationEnabled(), sessionProperties.getExpirationMinutes(),
            sessionProperties.getCleanupIntervalMinutes(), sessionProperties.getMaxSessions());

        // 启动定时清理任务
        if (sessionProperties.isExpirationEnabled()) {
            startCleanupTask();
        }
    }

    @PreDestroy
    public void destroy() {
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
            log.info("[PersistentChatMemoryManager] 清理任务已停止");
        }
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }

    /**
     * 启动定时清理任务
     */
    private void startCleanupTask() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("session-cleanup-");
        taskScheduler.setDaemon(true);
        taskScheduler.initialize();

        long intervalMs = sessionProperties.getCleanupIntervalMinutes() * 60 * 1000;
        cleanupTask = taskScheduler.scheduleAtFixedRate(
            this::cleanupExpiredSessions,
            Instant.now().plusMillis(intervalMs),
            java.time.Duration.ofMillis(intervalMs)
        );

        log.info("[PersistentChatMemoryManager] 定时清理任务已启动: 间隔{}分钟", 
            sessionProperties.getCleanupIntervalMinutes());
    }

    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions() {
        if (!sessionProperties.isExpirationEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long expirationMs = sessionProperties.getExpirationMinutes() * 60 * 1000;
        int cleanedCount = 0;

        for (Map.Entry<String, SessionWrapper> entry : memoryCache.entrySet()) {
            String sessionId = entry.getKey();
            SessionWrapper wrapper = entry.getValue();

            if (now - wrapper.getLastAccessTime() > expirationMs) {
                removeMemory(sessionId);
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("[PersistentChatMemoryManager] 清理过期会话: 数量={}, 剩余={}", 
                cleanedCount, memoryCache.size());
        }
    }

    /**
     * 获取或创建会话记忆
     *
     * @param sessionId 会话ID
     * @return ChatMemory实例
     */
    public ChatMemory getOrCreateMemory(String sessionId) {
        return getOrCreateMemory(sessionId, sessionProperties.getDefaultMemoryWindow());
    }

    /**
     * 获取或创建会话记忆（自定义消息窗口大小）
     *
     * @param sessionId   会话ID
     * @param maxMessages 最大消息数
     * @return ChatMemory实例
     */
    public ChatMemory getOrCreateMemory(String sessionId, int maxMessages) {
        // 检查最大会话数限制
        checkMaxSessions();

        SessionWrapper wrapper = memoryCache.computeIfAbsent(sessionId, 
            id -> new SessionWrapper(
                new PersistentChatMemory(id, chatMemoryStore, maxMessages),
                System.currentTimeMillis()
            ));
        
        // 更新最后访问时间
        wrapper.updateAccessTime();
        return wrapper.getMemory();
    }

    /**
     * 检查最大会话数限制
     */
    private void checkMaxSessions() {
        int maxSessions = sessionProperties.getMaxSessions();
        if (maxSessions > 0 && memoryCache.size() >= maxSessions) {
            // 清理最老的会话
            String oldestSession = findOldestSession();
            if (oldestSession != null) {
                removeMemory(oldestSession);
                log.warn("[PersistentChatMemoryManager] 达到最大会话数限制，清理最老会话: {}", oldestSession);
            }
        }
    }

    /**
     * 查找最老的会话
     */
    private String findOldestSession() {
        String oldestId = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, SessionWrapper> entry : memoryCache.entrySet()) {
            if (entry.getValue().getLastAccessTime() < oldestTime) {
                oldestTime = entry.getValue().getLastAccessTime();
                oldestId = entry.getKey();
            }
        }

        return oldestId;
    }

    /**
     * 获取会话记忆（如果存在）
     *
     * @param sessionId 会话ID
     * @return ChatMemory实例，不存在返回null
     */
    public ChatMemory getMemory(String sessionId) {
        SessionWrapper wrapper = memoryCache.get(sessionId);
        if (wrapper != null) {
            wrapper.updateAccessTime();
            return wrapper.getMemory();
        }
        return null;
    }

    /**
     * 删除会话记忆
     *
     * @param sessionId 会话ID
     */
    public void removeMemory(String sessionId) {
        SessionWrapper wrapper = memoryCache.remove(sessionId);
        if (wrapper != null) {
            wrapper.getMemory().clear();
            log.info("[PersistentChatMemoryManager] 删除会话记忆: sessionId={}", sessionId);
        }
    }

    /**
     * 清空所有会话记忆
     */
    public void clearAll() {
        for (String sessionId : memoryCache.keySet()) {
            removeMemory(sessionId);
        }
        log.info("[PersistentChatMemoryManager] 清空所有会话记忆");
    }

    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return memoryCache.size();
    }

    /**
     * 刷新会话缓存过期时间
     *
     * @param sessionId 会话ID
     */
    public void refreshSession(String sessionId) {
        SessionWrapper wrapper = memoryCache.get(sessionId);
        if (wrapper != null) {
            wrapper.updateAccessTime();
        }
        chatMemoryStore.refreshSession(sessionId);
    }

    /**
     * 检查会话是否存在
     */
    public boolean exists(String sessionId) {
        return memoryCache.containsKey(sessionId) || chatMemoryStore.exists(sessionId);
    }

    /**
     * 会话包装器
     * 包含ChatMemory和最后访问时间
     */
    private static class SessionWrapper {
        private final PersistentChatMemory memory;
        private volatile long lastAccessTime;

        public SessionWrapper(PersistentChatMemory memory, long lastAccessTime) {
            this.memory = memory;
            this.lastAccessTime = lastAccessTime;
        }

        public PersistentChatMemory getMemory() {
            return memory;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
