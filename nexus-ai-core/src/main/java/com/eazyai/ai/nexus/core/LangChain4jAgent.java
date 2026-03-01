package com.eazyai.ai.nexus.core;

import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.core.assistant.AgentAssistant;
import com.eazyai.ai.nexus.core.assistant.AssistantFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 LangChain4j AiServices 的智能体实现
 * 
 * <p>核心特性：</p>
 * <ul>
 *   <li>自动 Tool Calling - LLM 自动选择并执行工具</li>
 *   <li>自动会话记忆 - 支持多轮对话上下文</li>
 *   <li>结构化输出 - 支持返回 Java 对象</li>
 * </ul>
 * 
 * <p>这是替代原 ReActAgent 的新实现，利用 LangChain4j 原生能力。</p>
 */
@Slf4j
@Component
public class LangChain4jAgent implements Agent {

    @Autowired
    private AssistantFactory assistantFactory;

    @Override
    public String getName() {
        return "LangChain4jAgent";
    }

    @Override
    public String getDescription() {
        return "基于 LangChain4j AiServices 的智能体，支持自动工具调用和会话记忆";
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        List<AgentResponse.ExecutionStep> steps = new ArrayList<>();

        try {
            AgentAssistant assistant;
            
            // 根据是否需要会话记忆选择不同的 Assistant
            if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
                assistant = assistantFactory.getAssistantWithMemory(request.getSessionId());
                steps.add(AgentResponse.ExecutionStep.builder()
                        .stepNumber(1)
                        .stage("INIT")
                        .description("初始化带记忆的会话: " + request.getSessionId())
                        .build());
            } else {
                assistant = assistantFactory.getAssistant();
            }

            // 执行请求
            steps.add(AgentResponse.ExecutionStep.builder()
                    .stepNumber(2)
                    .stage("EXECUTE")
                    .description("调用 LLM 执行请求")
                    .input(request.getQuery())
                    .build());

            String result = assistant.chat(request.getQuery());

            steps.add(AgentResponse.ExecutionStep.builder()
                    .stepNumber(3)
                    .stage("COMPLETE")
                    .description("执行完成")
                    .output(result.length() > 200 ? result.substring(0, 200) + "..." : result)
                    .build());

            return AgentResponse.builder()
                    .output(result)
                    .success(true)
                    .steps(steps)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("LangChain4jAgent 执行失败", e);
            return AgentResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .steps(steps)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public boolean supports(String taskType) {
        // 支持所有类型任务
        return true;
    }
}
