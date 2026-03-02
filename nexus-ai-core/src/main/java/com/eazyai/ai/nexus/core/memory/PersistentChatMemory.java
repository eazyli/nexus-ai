package com.eazyai.ai.nexus.core.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久化会话记忆
 * 支持自动持久化到存储后端
 */
@Slf4j
public class PersistentChatMemory implements ChatMemory {

    private final String sessionId;
    private final ChatMemoryStore store;
    private final int maxMessages;
    private final ChatMemoryStore.MemoryContext context;
    
    // 内存中的消息缓存
    private List<ChatMessage> messages;
    
    // 是否已从存储加载
    private boolean loaded = false;

    public PersistentChatMemory(String sessionId, ChatMemoryStore store) {
        this(sessionId, store, 20, null);
    }

    public PersistentChatMemory(String sessionId, ChatMemoryStore store, int maxMessages) {
        this(sessionId, store, maxMessages, null);
    }

    public PersistentChatMemory(String sessionId, ChatMemoryStore store, int maxMessages, ChatMemoryStore.MemoryContext context) {
        this.sessionId = sessionId;
        this.store = store;
        this.maxMessages = maxMessages;
        this.context = context;
        this.messages = new ArrayList<>();
    }

    @Override
    public Object id() {
        return sessionId;
    }

    @Override
    public void add(ChatMessage message) {
        ensureLoaded();
        messages.add(message);
        
        // 如果超过最大消息数，移除最早的消息（保留系统消息）
        while (messages.size() > maxMessages) {
            // 找到第一个可以移除的消息（跳过系统消息）
            int removeIndex = -1;
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).type() != ChatMessageType.SYSTEM) {
                    removeIndex = i;
                    break;
                }
            }
            if (removeIndex >= 0) {
                messages.remove(removeIndex);
            } else {
                break; // 全是系统消息，无法移除
            }
        }
        
        // 持久化
        persist();
    }

    /**
     * 批量添加消息
     */
    public void addAll(List<ChatMessage> messages) {
        ensureLoaded();
        for (ChatMessage message : messages) {
            add(message);
        }
    }

    @Override
    public List<ChatMessage> messages() {
        ensureLoaded();
        return new ArrayList<>(messages);
    }

    @Override
    public void clear() {
        messages.clear();
        store.deleteMessages(sessionId);
        loaded = true;
        log.debug("[PersistentChatMemory] 清空会话记忆: sessionId={}", sessionId);
    }

    /**
     * 确保消息已从存储加载
     */
    private void ensureLoaded() {
        if (!loaded) {
            List<ChatMessage> storedMessages = store.getMessages(sessionId);
            this.messages = new ArrayList<>(storedMessages);
            this.loaded = true;
            log.debug("[PersistentChatMemory] 从存储加载消息: sessionId={}, count={}", 
                sessionId, messages.size());
        }
    }

    /**
     * 持久化到存储
     */
    private void persist() {
        if (context != null) {
            store.updateMessages(sessionId, messages, context);
        } else {
            store.updateMessages(sessionId, messages);
        }
        log.debug("[PersistentChatMemory] 持久化消息: sessionId={}, count={}", 
            sessionId, messages.size());
    }

    /**
     * 获取会话ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 获取消息数量
     */
    public int getMessageCount() {
        ensureLoaded();
        return messages.size();
    }

    /**
     * 强制刷新缓存
     */
    public void refresh() {
        List<ChatMessage> storedMessages = store.getMessages(sessionId);
        this.messages = new ArrayList<>(storedMessages);
        this.loaded = true;
    }
}
