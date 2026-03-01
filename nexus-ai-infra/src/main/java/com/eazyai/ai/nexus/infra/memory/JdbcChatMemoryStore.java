package com.eazyai.ai.nexus.infra.memory;

import com.eazyai.ai.nexus.core.memory.ChatMemoryStore;
import com.eazyai.ai.nexus.infra.dal.entity.AiMemory;
import com.eazyai.ai.nexus.infra.dal.repository.AiMemoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于MySQL的会话记忆存储
 * 使用 ai_memory 表存储会话消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcChatMemoryStore implements ChatMemoryStore {

    private final AiMemoryRepository aiMemoryRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        List<AiMemory> memories = aiMemoryRepository.findBySessionId(sessionId);
        List<ChatMessage> messages = new ArrayList<>();

        for (AiMemory memory : memories) {
            ChatMessage message = toChatMessage(memory);
            if (message != null) {
                messages.add(message);
            }
        }

        log.debug("[JdbcChatMemoryStore] 加载会话消息: sessionId={}, count={}", sessionId, messages.size());
        return messages;
    }

    @Override
    public void updateMessages(String sessionId, List<ChatMessage> messages) {
        // 先删除旧消息
        aiMemoryRepository.deleteBySessionId(sessionId);

        // 保存新消息
        List<AiMemory> memories = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            AiMemory memory = toAiMemory(sessionId, message, i);
            memories.add(memory);
        }

        if (!memories.isEmpty()) {
            aiMemoryRepository.insertBatch(memories);
            log.debug("[JdbcChatMemoryStore] 保存会话消息: sessionId={}, count={}", sessionId, memories.size());
        }
    }

    @Override
    public void deleteMessages(String sessionId) {
        int deleted = aiMemoryRepository.deleteBySessionId(sessionId);
        log.debug("[JdbcChatMemoryStore] 删除会话消息: sessionId={}, deleted={}", sessionId, deleted);
    }

    @Override
    public boolean exists(String sessionId) {
        List<AiMemory> memories = aiMemoryRepository.findBySessionId(sessionId);
        return !memories.isEmpty();
    }

    /**
     * 将 AiMemory 转换为 ChatMessage
     */
    private ChatMessage toChatMessage(AiMemory memory) {
        String role = memory.getRole();
        String content = memory.getContent();

        try {
            return switch (role.toLowerCase()) {
                case "user" -> UserMessage.from(content);
                case "assistant" -> {
                    // 尝试解析工具调用
                    if (memory.getMemoryData() != null && memory.getMemoryData().containsKey("toolCalls")) {
                        List<Map<String, Object>> toolCallsData = 
                            objectMapper.convertValue(memory.getMemoryData().get("toolCalls"), 
                                new TypeReference<List<Map<String, Object>>>() {});
                        List<ToolExecutionRequest> toolCalls = new ArrayList<>();
                        for (Map<String, Object> tc : toolCallsData) {
                            toolCalls.add(ToolExecutionRequest.builder()
                                .id((String) tc.get("id"))
                                .name((String) tc.get("name"))
                                .arguments((String) tc.get("arguments"))
                                .build());
                        }
                        yield AiMessage.from(content, toolCalls);
                    }
                    yield AiMessage.from(content);
                }
                case "system" -> SystemMessage.from(content);
                case "tool" -> {
                    if (memory.getMemoryData() != null) {
                        String toolExecutionId = (String) memory.getMemoryData().get("toolExecutionId");
                        String toolName = (String) memory.getMemoryData().getOrDefault("toolName", "");
                        // 创建 ToolExecutionRequest 用于构建 ToolExecutionResultMessage
                        ToolExecutionRequest request = ToolExecutionRequest.builder()
                            .id(toolExecutionId)
                            .name(toolName)
                            .build();
                        yield ToolExecutionResultMessage.toolExecutionResultMessage(request, content);
                    }
                    yield ToolExecutionResultMessage.toolExecutionResultMessage(
                        ToolExecutionRequest.builder().id("").name("").build(), content);
                }
                default -> {
                    log.warn("[JdbcChatMemoryStore] 未知消息角色: {}", role);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("[JdbcChatMemoryStore] 转换消息失败: role={}, content={}", role, content, e);
            return null;
        }
    }

    /**
     * 将 ChatMessage 转换为 AiMemory
     */
    private AiMemory toAiMemory(String sessionId, ChatMessage message, int order) {
        AiMemory memory = new AiMemory();
        memory.setSessionId(sessionId);
        memory.setMemoryType("short");
        memory.setCreateTime(LocalDateTime.now());
        memory.setUpdateTime(LocalDateTime.now());
        
        // 设置过期时间（默认7天）
        memory.setExpireTime(LocalDateTime.now().plusDays(7));

        String role = switch (message.type()) {
            case USER -> "user";
            case AI -> "assistant";
            case SYSTEM -> "system";
            case TOOL_EXECUTION_RESULT -> "tool";
        };
        memory.setRole(role);
        memory.setContent(message.text());

        // 处理特殊消息类型
        if (message instanceof AiMessage aiMessage) {
            if (aiMessage.hasToolExecutionRequests()) {
                try {
                    List<Map<String, Object>> toolCalls = new ArrayList<>();
                    for (ToolExecutionRequest tc : aiMessage.toolExecutionRequests()) {
                        toolCalls.add(Map.of(
                            "id", tc.id() != null ? tc.id() : "",
                            "name", tc.name() != null ? tc.name() : "",
                            "arguments", tc.arguments() != null ? tc.arguments() : ""
                        ));
                    }
                    memory.setMemoryData(Map.of("toolCalls", toolCalls));
                } catch (Exception e) {
                    log.warn("[JdbcChatMemoryStore] 序列化工具调用失败", e);
                }
            }
        } else if (message instanceof ToolExecutionResultMessage toolMessage) {
            memory.setMemoryData(Map.of("toolExecutionId", toolMessage.id() != null ? toolMessage.id() : ""));
        }

        // 估算token数量（简单估算：字符数/4）
        memory.setTokenCount(message.text() != null ? message.text().length() / 4 : 0);

        return memory;
    }
}
