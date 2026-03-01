package com.eazyai.ai.nexus.core.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 智能助手接口
 * 基于 LangChain4j AiServices 实现
 * 
 * <p>核心特性：</p>
 * <ul>
 *   <li>自动 Tool Calling - LLM 自动选择并执行工具</li>
 *   <li>自动 ReAct 循环 - 无需手动实现 Thought/Action/Observation</li>
 *   <li>结构化输出 - 支持返回 Java 对象</li>
 * </ul>
 */
public interface AgentAssistant {

    /**
     * 执行用户请求
     * LLM 会自动判断是否需要调用工具
     */
    @SystemMessage("""
        你是一个智能助手，可以使用工具完成任务。
        
        工具使用规则：
        1. 当用户请求需要工具能力时，自动调用相应的工具
        2. 工具执行完成后，基于结果回答用户
        3. 如果不需要工具，直接回答用户问题
        4. 用中文回答，简洁专业
        """)
    String chat(@UserMessage String userMessage);

    /**
     * 带会话ID的对话（支持记忆）
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
    String chatWithMemory(@UserMessage String userMessage, @V("sessionId") String sessionId);

    /**
     * 带意图分析的对话
     */
    @SystemMessage("""
        你是一个智能助手，请先分析用户意图，然后选择合适的工具完成任务。
        
        分析步骤：
        1. 理解用户的核心需求
        2. 判断是否需要工具支持
        3. 如需工具，选择最合适的工具执行
        4. 整合结果，给出专业回答
        """)
    AgentResponse chatWithAnalysis(@UserMessage String userMessage);

    /**
     * Agent 响应结构
     */
    record AgentResponse(
        String intent,
        String thinking,
        String toolUsed,
        String answer
    ) {}
}
