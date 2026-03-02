package com.eazyai.ai.nexus.core.memory;

import com.eazyai.ai.nexus.api.memory.MemoryManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 LangChain4j ChatMemory 的记忆管理器
 * 
 * <p>特点：</p>
 * <ul>
 *   <li>使用 LangChain4j 原生的 ChatMemory 实现</li>
 *   <li>自动管理消息窗口（默认保留最近20条消息）</li>
 *   <li>支持会话隔离</li>
 *   <li>保留原有的长期记忆接口（待实现向量存储）</li>
 * </ul>
 */
@Slf4j
@Component
public class LangChain4jMemoryManager implements MemoryManager {

    // 会话记忆存储 (sessionId -> ChatMemory)
    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();

    // 短期记忆存储 (sessionId -> key -> value with expiry)
    private final Map<String, Map<String, TimedValue>> shortTermMemory = new ConcurrentHashMap<>();

    // 长期记忆存储 (userId -> list of memory entries)
    private final Map<String, List<MemoryEntryImpl>> longTermMemory = new ConcurrentHashMap<>();

    // 默认消息窗口大小
    private static final int DEFAULT_MESSAGE_WINDOW = 20;

    @Override
    public void storeShortTerm(String sessionId, String key, Object value, long ttl) {
        shortTermMemory.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(key, new TimedValue(value, System.currentTimeMillis() + ttl * 1000));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getShortTerm(String sessionId, String key) {
        Map<String, TimedValue> sessionStore = shortTermMemory.get(sessionId);
        if (sessionStore == null) {
            return null;
        }

        TimedValue timedValue = sessionStore.get(key);
        if (timedValue == null) {
            return null;
        }

        if (System.currentTimeMillis() > timedValue.expiryTime) {
            sessionStore.remove(key);
            return null;
        }

        return (T) timedValue.value;
    }

    @Override
    public void storeLongTerm(String userId, MemoryEntry memory) {
        longTermMemory.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new MemoryEntryImpl(memory));
    }

    @Override
    public List<MemoryEntry> retrieveLongTerm(String userId, String query, int limit) {
        List<MemoryEntryImpl> memories = longTermMemory.getOrDefault(userId, Collections.emptyList());

        return memories.stream()
                .filter(m -> m.content != null && m.content.toLowerCase().contains(query.toLowerCase()))
                .sorted(Comparator.comparingLong(m -> -m.timestamp))
                .limit(limit)
                .map(m -> (MemoryEntry) m)
                .toList();
    }

    @Override
    public List<MemoryEntry> retrieveByVector(String userId, float[] embedding, int limit) {
        log.warn("向量检索尚未实现，请配置向量数据库");
        return Collections.emptyList();
    }

    @Override
    public void clearSession(String sessionId) {
        sessionMemories.remove(sessionId);
        shortTermMemory.remove(sessionId);
    }

    @Override
    public List<MessageHistory> getSessionHistory(String sessionId) {
        ChatMemory memory = sessionMemories.get(sessionId);
        if (memory == null) {
            return Collections.emptyList();
        }

        return memory.messages().stream()
                .map(this::toMessageHistory)
                .toList();
    }

    @Override
    public void addMessage(String sessionId, String role, String content) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        
        if ("user".equalsIgnoreCase(role)) {
            memory.add(UserMessage.from(content));
        } else if ("assistant".equalsIgnoreCase(role)) {
            memory.add(AiMessage.from(content));
        }
    }

    /**
     * 获取或创建 ChatMemory
     */
    public ChatMemory getOrCreateMemory(String sessionId) {
        return sessionMemories.computeIfAbsent(sessionId, 
            id -> MessageWindowChatMemory.withMaxMessages(DEFAULT_MESSAGE_WINDOW));
    }

    /**
     * 获取 ChatMemory（如果存在）
     */
    public Optional<ChatMemory> getMemory(String sessionId) {
        return Optional.ofNullable(sessionMemories.get(sessionId));
    }

    /**
     * 清除所有会话记忆
     */
    public void clearAll() {
        sessionMemories.clear();
        shortTermMemory.clear();
    }

    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return sessionMemories.size();
    }

    private MessageHistory toMessageHistory(ChatMessage message) {
        String role = switch (message.type()) {
            case USER -> "user";
            case AI -> "assistant";
            case SYSTEM -> "system";
            case TOOL_EXECUTION_RESULT -> "tool";
        };
        return new MessageHistoryImpl(role, getTextFromMessage(message), System.currentTimeMillis());
    }

    /**
     * 从 ChatMessage 获取文本内容（兼容新 API）
     * ChatMessage.text() 已弃用，需要根据具体消息类型获取文本
     */
    private String getTextFromMessage(ChatMessage message) {
        return switch (message) {
            case AiMessage aiMessage -> aiMessage.text();
            case UserMessage userMessage -> userMessage.singleText();
            case SystemMessage systemMessage -> systemMessage.text();
            case ToolExecutionResultMessage toolMessage -> toolMessage.text();
            default -> null;
        };
    }

    /**
     * 带过期时间的值
     */
    private static class TimedValue {
        final Object value;
        final long expiryTime;

        TimedValue(Object value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
    }

    /**
     * 内存条目实现
     */
    private static class MemoryEntryImpl implements MemoryEntry {
        final String id = UUID.randomUUID().toString();
        final String type;
        final String content;
        final long timestamp;
        final float[] embedding;
        final Object metadata;

        MemoryEntryImpl(MemoryEntry source) {
            this.type = source.getType();
            this.content = source.getContent();
            this.timestamp = source.getTimestamp();
            this.embedding = source.getEmbedding();
            this.metadata = source.getMetadata();
        }

        @Override public String getId() { return id; }
        @Override public String getType() { return type; }
        @Override public String getContent() { return content; }
        @Override public long getTimestamp() { return timestamp; }
        @Override public float[] getEmbedding() { return embedding; }
        @Override public Object getMetadata() { return metadata; }
    }

    /**
     * 消息历史实现
     */
    private record MessageHistoryImpl(String role, String content, long timestamp) implements MessageHistory {
        @Override public String getRole() { return role; }
        @Override public String getContent() { return content; }
        @Override public long getTimestamp() { return timestamp; }
    }
}
