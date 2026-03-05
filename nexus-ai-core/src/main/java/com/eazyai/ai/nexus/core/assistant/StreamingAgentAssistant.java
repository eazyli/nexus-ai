package com.eazyai.ai.nexus.core.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 流式智能助手接口 - 重构版
 * 
 * <p>核心改进：</p>
 * <ul>
 *   <li>使用 @MemoryId 自动关联会话记忆</li>
 *   <li>系统消息通过 systemMessageProvider 动态注入（包含意图上下文）</li>
 *   <li>简化接口，移除模板变量</li>
 * </ul>
 * 
 * <p>LangChain4j 特性：</p>
 * <ul>
 *   <li>流式 Token 输出 - 实时返回生成的文本</li>
 *   <li>自动 Tool Calling - LLM 自动选择并执行工具</li>
 *   <li>自动会话记忆 - 通过 @MemoryId 自动关联</li>
 * </ul>
 */
public interface StreamingAgentAssistant {

    /**
     * 流式执行对话（无会话记忆）
     * 
     * <p>系统消息由 systemMessageProvider 动态提供，已包含意图上下文</p>
     * 
     * @param userMessage 用户消息
     * @return TokenStream 流式输出
     */
    TokenStream chatStream(@UserMessage String userMessage);

    /**
     * 流式执行对话（带会话记忆）
     * 
     * <p>使用 @MemoryId 自动关联会话记忆</p>
     * <p>系统消息由 systemMessageProvider 动态提供，已包含意图上下文</p>
     * 
     * @param memoryId 会话标识
     * @param userMessage 用户消息
     * @return TokenStream 流式输出
     */
    TokenStream chatStreamWithMemory(
        @MemoryId String memoryId,
        @UserMessage String userMessage
    );
}
