package com.eazyai.ai.nexus.core.engine;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import com.eazyai.ai.nexus.api.exception.AgentException;
import com.eazyai.ai.nexus.api.integrator.ResultIntegrator;
import com.eazyai.ai.nexus.api.observability.AgentEvent;
import com.eazyai.ai.nexus.api.observability.EventListener;
import com.eazyai.ai.nexus.api.planner.TaskPlan;
import com.eazyai.ai.nexus.api.scheduler.PluginScheduler;
import com.eazyai.ai.nexus.api.scheduler.ScheduleResult;
import com.eazyai.ai.nexus.core.assistant.AgentAssistant;
import com.eazyai.ai.nexus.core.assistant.AssistantFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体引擎
 * 
 * <p>重构说明：</p>
 * <ul>
 *   <li>核心流程使用 LangChain4j AiServices 自动处理 Tool Calling</li>
 *   <li>保留插件调度器用于复杂的多步骤任务编排</li>
 *   <li>保留结果整合器用于结果聚合和格式转换</li>
 *   <li>保留可观测性层用于监控和追踪</li>
 * </ul>
 * 
 * <p>执行模式：</p>
 * <ol>
 *   <li>简单模式：直接使用 AiServices，LLM 自动选择工具执行</li>
 *   <li>复杂模式：保留完整的意图分析→规划→调度→执行流程</li>
 * </ol>
 */
@Slf4j
@Component
public class AgentEngine {

    @Autowired
    private AssistantFactory assistantFactory;

    @Autowired(required = false)
    private PluginScheduler scheduler;

    @Autowired(required = false)
    private ResultIntegrator integrator;

    @Autowired(required = false)
    private List<EventListener> eventListeners;

    private final Map<String, AgentContext> activeContexts = new ConcurrentHashMap<>();

    /**
     * 执行请求（简单模式）
     * 使用 LangChain4j AiServices 自动处理工具调用
     * 
     * 会话记忆逻辑：
     * - 始终使用会话记忆模式，确保多轮对话上下文连续
     * - 新会话：生成 sessionId 并创建空记忆
     * - 已有会话：加载历史记忆
     */
    public AgentResponse execute(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = java.util.UUID.randomUUID().toString();
        
        // 转义用户输入中的模板语法，防止 {{xxx}} 被当作模板变量解析
        String query = escapeTemplateVariables(request.getQuery());
        
        // 处理 sessionId：始终使用会话记忆模式
        String sessionId = request.getSessionId();
        boolean isNewSession = (sessionId == null);
        
        if (isNewSession) {
            sessionId = java.util.UUID.randomUUID().toString();
            log.info("[AgentEngine] 生成新会话ID, sessionId={}", sessionId);
        } else {
            log.info("[AgentEngine] 使用已有会话ID, sessionId={}", sessionId);
        }

        log.info("[AgentEngine] 执行请求: appId={}, isNewSession={}, sessionId={}, query={}", 
                request.getAppId(), isNewSession, sessionId, query);

        // 初始化工具执行上下文
        ToolExecutionContext.init();

        AgentContext context = AgentContext.builder()
                .requestId(requestId)
                .sessionId(sessionId)
                .userInput(request.getQuery())
                .currentStage(AgentContext.ExecutionStage.INTENT_ANALYSIS)
                .build();

        activeContexts.put(requestId, context);

        try {
            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.REQUEST_START)
                    .stage("start")
                    .data(Map.of("query", request.getQuery(), "appId", request.getAppId(), "sessionId", sessionId))
                    .build());

            // 使用 LangChain4j AiServices 执行（始终使用会话记忆模式）
            String result;
            if (request.getAppId() != null) {
                // 应用专属 + 会话记忆
                log.info("[AgentEngine] 使用应用专属记忆模式, appId={}, sessionId={}", request.getAppId(), sessionId);
                AgentAssistant assistant = assistantFactory.getAssistantByAppId(request.getAppId(), sessionId);
                result = assistant.chatWithMemory(query);
            } else {
                // 通用 + 会话记忆
                log.info("[AgentEngine] 使用通用记忆模式, sessionId={}", sessionId);
                AgentAssistant assistant = assistantFactory.getAssistantWithMemory(sessionId);
                result = assistant.chatWithMemory(query);
            }

            log.info("[AgentEngine] 执行完成, 结果长度: {}", result != null ? result.length() : 0);

            context.setCurrentStage(AgentContext.ExecutionStage.COMPLETED);

            // 获取工具执行记录
            ToolExecutionContext toolContext = ToolExecutionContext.current();
            log.info("[AgentEngine] 工具调用记录: usedPlugins={}, steps={}", 
                toolContext.getUsedPlugins(), toolContext.getExecutionSteps().size());

            AgentResponse response = AgentResponse.builder()
                    .sessionId(sessionId)
                    .output(result)
                    .success(true)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .steps(toolContext.getExecutionSteps())
                    .usedPlugins(toolContext.getUsedPlugins())
                    .build();
            response.getMetadata().put("requestId", requestId);
            response.getMetadata().put("mode", "ai-services");

            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.REQUEST_END)
                    .stage("completed")
                    .success(true)
                    .duration(System.currentTimeMillis() - startTime)
                    .build());

            return response;

        } catch (Exception e) {
            log.error("[AgentEngine] Agent execution failed", e);
            context.setCurrentStage(AgentContext.ExecutionStage.FAILED);

            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.ERROR)
                    .stage("error")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());

            return AgentResponse.error(e.getMessage());
        } finally {
            activeContexts.remove(requestId);
            // 清理工具执行上下文
            ToolExecutionContext.clear();
        }
    }

    /**
     * 执行请求（高级模式）
     * 保留完整的意图分析→规划→调度→执行流程
     * 适用于需要精细控制的复杂任务
     */
    public AgentResponse executeWithOrchestration(AgentRequest request, TaskPlan plan) {
        if (scheduler == null || integrator == null) {
            throw new AgentException(AgentException.ErrorCode.EXECUTION_FAILED,
                    "Advanced mode requires PluginScheduler and ResultIntegrator");
        }

        long startTime = System.currentTimeMillis();
        String requestId = java.util.UUID.randomUUID().toString();

        AgentContext context = AgentContext.builder()
                .requestId(requestId)
                .sessionId(request.getSessionId())
                .userInput(request.getQuery())
                .currentStage(AgentContext.ExecutionStage.SCHEDULING)
                .build();

        activeContexts.put(requestId, context);

        try {
            // 1. 插件调度层
            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.SCHEDULING)
                    .stage("scheduling")
                    .data(Map.of("planId", plan.getPlanId()))
                    .build());

            ScheduleResult scheduleResult = scheduler.schedule(plan, context);
            context.recordStep("scheduling", scheduleResult);
            context.setCurrentStage(AgentContext.ExecutionStage.INTEGRATING);

            // 2. 结果整合层
            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.INTEGRATION)
                    .stage("integration")
                    .build());

            AgentResponse response = integrator.integrate(scheduleResult, context);
            context.recordStep("integration", response);

            context.setCurrentStage(AgentContext.ExecutionStage.COMPLETED);
            response.setExecutionTime(System.currentTimeMillis() - startTime);
            response.getMetadata().put("requestId", requestId);
            response.getMetadata().put("mode", "orchestrated");

            return response;

        } catch (Exception e) {
            log.error("Orchestrated execution failed", e);
            context.setCurrentStage(AgentContext.ExecutionStage.FAILED);
            return AgentResponse.error(e.getMessage());
        } finally {
            activeContexts.remove(requestId);
        }
    }

    /**
     * 流式执行
     * 支持 SSE / WebSocket 流式响应
     */
    public void executeStreaming(AgentRequest request, StreamingCallback callback) {
        try {
            AgentAssistant assistant = request.getSessionId() != null
                    ? assistantFactory.getAssistantWithMemory(request.getSessionId())
                    : assistantFactory.getAssistant();

            // LangChain4j 支持流式输出（需要配置 StreamingChatLanguageModel）
            String result = assistant.chat(request.getQuery());
            callback.onComplete(result);

        } catch (Exception e) {
            log.error("Streaming execution failed", e);
            callback.onError(e);
        }
    }

    /**
     * 清理会话记忆
     */
    public void clearSession(String sessionId) {
        assistantFactory.clearSessionMemory(sessionId);
    }

    /**
     * 获取活跃上下文
     */
    public AgentContext getContext(String requestId) {
        return activeContexts.get(requestId);
    }

    /**
     * 发布事件
     */
    private void publishEvent(AgentEvent event) {
        if (eventListeners == null) {
            return;
        }

        for (EventListener listener : eventListeners) {
            if (listener.supports(event.getType())) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.warn("Event listener {} failed: {}", listener.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * 转义模板变量语法
     * LangChain4j 会把 {{xxx}} 当作模板变量解析，需要转义防止误解析
     */
    private String escapeTemplateVariables(String input) {
        if (input == null) {
            return null;
        }
        // 将 {{ 替换为 { { ，避免被解析为模板变量
        return input.replace("{{", "{ {").replace("}}", "} }");
    }

    /**
     * 流式回调接口
     */
    public interface StreamingCallback {
        void onToken(String token);
        void onComplete(String fullResponse);
        void onError(Exception e);
    }
}
