package com.eazyai.ai.nexus.core.assistant;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 流式智能助手接口
 * 基于 LangChain4j AiServices 实现，支持流式输出
 * 
 * <p>核心特性：</p>
 * <ul>
 *   <li>流式 Token 输出 - 实时返回生成的文本</li>
 *   <li>自动 Tool Calling - LLM 自动选择并执行工具</li>
 *   <li>自动 ReAct 循环 - 无需手动实现</li>
 * </ul>
 */
public interface StreamingAgentAssistant {

    /**
     * 流式执行用户请求
     * LLM 会自动判断是否需要调用工具，并流式返回结果
     */
    @SystemMessage("""
        你是一个智能助手，可以使用工具完成任务。
        
        工具使用规则：
        1. 当用户请求需要工具能力时，自动调用相应的工具
        2. 工具执行完成后，基于结果回答用户
        3. 如果不需要工具，直接回答用户问题
        4. 用中文回答，简洁专业
        """)
    TokenStream chatStream(@UserMessage String userMessage);

    /**
     * 带会话记忆的流式对话
     * 注意：sessionId 通过 AiServices 的 ChatMemory 配置传递，不作为模板变量
     */
    @SystemMessage("""
        你是一个智能助手，可以使用工具完成任务。

        工具使用规则：
        1. 当用户请求需要工具能力时，自动调用相应的工具
        2. 工具执行完成后，基于结果回答用户
        3. 如果不需要工具，直接回答用户问题
        4. 用中文回答，简洁专业
        5. 记住之前的对话内容，保持上下文连贯
        """)
    TokenStream chatStreamWithMemory(@UserMessage String userMessage);
}
