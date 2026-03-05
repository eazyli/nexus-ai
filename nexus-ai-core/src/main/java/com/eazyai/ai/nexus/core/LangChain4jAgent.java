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
import java.util.UUID;

/**
 * 基于 LangChain4j AiServices 的智能体实现 - 重构版
 * 
 * <p>核心改进：</p>
 * <ul>
 *   <li>使用 @MemoryId 自动关联会话记忆</li>
 *   <li>意图上下文通过 systemMessageProvider 自动注入</li>
 * </ul>
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
            // 创建 Assistant
            AgentAssistant assistant = assistantFactory.createAssistant(request.getAppId(), null);

            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }

            steps.add(AgentResponse.ExecutionStep.builder()
                    .stepNumber(1)
                    .stage("INIT")
                    .description("初始化会话: " + sessionId)
                    .build());

            steps.add(AgentResponse.ExecutionStep.builder()
                    .stepNumber(2)
                    .stage("EXECUTE")
                    .description("调用 LLM 执行请求")
                    .input(request.getQuery())
                    .build());

            // 使用新接口：@MemoryId 自动关联会话记忆，意图上下文通过 systemMessageProvider 注入
            String result = assistant.chatWithMemory(sessionId, request.getQuery());

            steps.add(AgentResponse.ExecutionStep.builder()
                    .stepNumber(3)
                    .stage("COMPLETE")
                    .description("执行完成")
                    .output(result.length() > 200 ? result.substring(0, 200) + "..." : result)
                    .build());

            return AgentResponse.builder()
                    .sessionId(sessionId)
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
        return true;
    }
}
