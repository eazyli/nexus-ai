package com.eazyai.ai.nexus.core.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * 智能助手接口 - 重构版
 * 
 * <p>核心改进：</p>
 * <ul>
 *   <li>使用 @MemoryId 自动关联会话记忆，无需手动管理</li>
 *   <li>系统消息通过 systemMessageProvider 动态注入（包含意图上下文）</li>
 *   <li>简化接口，移除冗余方法和模板变量</li>
 * </ul>
 * 
 * <p>LangChain4j 特性：</p>
 * <ul>
 *   <li>自动 Tool Calling - LLM 自动选择并执行工具</li>
 *   <li>自动 ReAct 循环 - 无需手动实现 Thought/Action/Observation</li>
 *   <li>自动会话记忆 - 通过 @MemoryId 自动关联</li>
 * </ul>
 */
public interface AgentAssistant {

    /**
     * 执行对话（无会话记忆）
     * 
     * <p>系统消息由 systemMessageProvider 动态提供，已包含意图上下文</p>
     * 
     * @param userMessage 用户消息
     * @return 助手响应
     */
    String chat(@UserMessage String userMessage);

    /**
     * 执行对话（带会话记忆）
     * 
     * <p>使用 @MemoryId 自动关联会话记忆：
     * <ul>
     *   <li>相同 memoryId 的请求会自动共享历史消息</li>
     *   <li>通过 chatMemoryProvider 统一管理记忆存储</li>
     * </ul>
     * 
     * <p>系统消息由 systemMessageProvider 动态提供，已包含意图上下文</p>
     * 
     * @param memoryId 会话标识（自动关联 ChatMemory）
     * @param userMessage 用户消息
     * @return 助手响应
     */
    String chatWithMemory(
        @MemoryId String memoryId,
        @UserMessage String userMessage
    );
}
