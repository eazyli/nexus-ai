package com.eazyai.ai.nexus.core.engine;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import com.eazyai.ai.nexus.api.observability.AgentEvent;
import com.eazyai.ai.nexus.api.observability.EventListener;
import com.eazyai.ai.nexus.api.react.ReActContext;
import com.eazyai.ai.nexus.api.react.ReActStep;
import com.eazyai.ai.nexus.api.react.ReflectionResult;
import com.eazyai.ai.nexus.api.react.ThoughtEvent;
import com.eazyai.ai.nexus.api.scheduler.PluginScheduler;
import com.eazyai.ai.nexus.api.scheduler.ScheduleResult;
import com.eazyai.ai.nexus.core.assistant.AgentAssistant;
import com.eazyai.ai.nexus.core.assistant.AssistantFactory;
import com.eazyai.ai.nexus.core.planner.InternalOrchestrator;
import com.eazyai.ai.nexus.core.reflection.ReflectionAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ReAct 执行引擎（统一入口）
 * 
 * <p>完整的 ReAct 流程：</p>
 * <ol>
 *   <li>Think - 思考：分析用户意图，决定下一步行动</li>
 *   <li>Act - 行动：选择并调用合适的工具</li>
 *   <li>Observe - 观察：获取工具执行结果</li>
 *   <li>Reflect - 反思：评估执行效果，必要时调整策略</li>
 * </ol>
 * 
 * <p>架构简化说明：</p>
 * <ul>
 *   <li>合并原有的简单模式和高级模式为统一入口</li>
 *   <li>保留 PluginScheduler 作为内部编排能力</li>
 *   <li>通过 ThoughtEvent 回调暴露思考过程</li>
 * </ul>
 */
@Slf4j
@Component
public class ReActEngine {

    @Autowired
    private AssistantFactory assistantFactory;

    @Autowired(required = false)
    private PluginScheduler scheduler;

    @Autowired(required = false)
    private ReflectionAgent reflectionAgent;

    @Autowired(required = false)
    private InternalOrchestrator orchestrator;

    @Autowired(required = false)
    private List<EventListener> eventListeners;

    private final Map<String, AgentContext> activeContexts = new ConcurrentHashMap<>();

    /**
     * 执行请求（统一入口）
     * 
     * @param request 代理请求
     * @return 执行结果
     */
    public AgentResponse execute(AgentRequest request) {
        return execute(request, ThoughtEvent.ThinkingCallback.noop());
    }

    /**
     * 执行请求（带思考过程回调）
     * 
     * @param request 代理请求
     * @param thinkingCallback 思考过程回调
     * @return 执行结果
     */
    public AgentResponse execute(AgentRequest request, Consumer<ThoughtEvent> thinkingCallback) {
        long startTime = System.currentTimeMillis();
        String requestId = java.util.UUID.randomUUID().toString();

        // 转义用户输入中的模板语法
        String query = escapeTemplateVariables(request.getQuery());

        // 处理 sessionId
        String sessionId = request.getSessionId();
        boolean isNewSession = (sessionId == null);
        if (isNewSession) {
            sessionId = java.util.UUID.randomUUID().toString();
        }

        // 初始化上下文
        ToolExecutionContext.init();
        ReActContext reactContext = ReActContext.builder()
                .sessionId(sessionId)
                .userInput(query)
                .build();

        AgentContext context = AgentContext.builder()
                .requestId(requestId)
                .sessionId(sessionId)
                .userInput(request.getQuery())
                .currentStage(AgentContext.ExecutionStage.INTENT_ANALYSIS)
                .build();

        activeContexts.put(requestId, context);

        try {
            // 发布开始事件
            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.REQUEST_START)
                    .stage("react_start")
                    .data(Map.of("query", query, "appId", request.getAppId(), "sessionId", sessionId))
                    .build());

            thinkingCallback.accept(ThoughtEvent.builder()
                    .type(ThoughtEvent.EventType.THINKING_START)
                    .content("开始处理请求...")
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 核心执行：使用 LangChain4j AiServices
            // LLM 会自动完成 Think -> Act -> Observe 循环
            String result = executeWithReActLoop(request, query, sessionId, reactContext, thinkingCallback);

            reactContext.setCompleted(true);
            reactContext.setFinalAnswer(result);
            context.setCurrentStage(AgentContext.ExecutionStage.COMPLETED);

            // 反思阶段（可选）
            if (shouldReflect(reactContext) && reflectionAgent != null) {
                ReflectionResult reflection = reflectionAgent.reflect(reactContext);
                thinkingCallback.accept(ThoughtEvent.builder()
                        .type(ThoughtEvent.EventType.REFLECTION)
                        .content(reflection.getSummary())
                        .timestamp(System.currentTimeMillis())
                        .build());
            }

            // 获取工具执行记录
            ToolExecutionContext toolContext = ToolExecutionContext.current();

            // 构建响应
            AgentResponse response = buildResponse(sessionId, requestId, result, 
                    startTime, toolContext, reactContext);

            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.REQUEST_END)
                    .stage("completed")
                    .success(true)
                    .duration(System.currentTimeMillis() - startTime)
                    .build());

            return response;

        } catch (Exception e) {
            log.error("[ReActEngine] 执行失败", e);
            context.setCurrentStage(AgentContext.ExecutionStage.FAILED);
            reactContext.addStep(ReActStep.builder()
                    .stepNumber(reactContext.getSteps().size() + 1)
                    .type(ReActStep.StepType.ACTION)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build());

            thinkingCallback.accept(ThoughtEvent.error(e.getMessage()));

            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.ERROR)
                    .stage("error")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());

            return AgentResponse.error(e.getMessage());

        } finally {
            activeContexts.remove(requestId);
            ToolExecutionContext.clear();
        }
    }

    /**
     * ReAct 循环执行
     * LangChain4j 内部已实现 Tool Calling 循环，这里主要是记录和回调
     */
    private String executeWithReActLoop(AgentRequest request, String query, 
            String sessionId, ReActContext reactContext, Consumer<ThoughtEvent> thinkingCallback) {

        AgentAssistant assistant;
        if (request.getAppId() != null) {
            assistant = assistantFactory.getAssistantByAppId(request.getAppId(), sessionId);
        } else {
            assistant = assistantFactory.getAssistantWithMemory(sessionId);
        }

        // LangChain4j 自动处理 Tool Calling
        // 我们通过 ToolExecutionContext 捕获工具调用记录
        String result = assistant.chatWithMemory(query);

        // 从 ToolExecutionContext 构建 ReAct 步骤
        buildReActStepsFromToolContext(reactContext, thinkingCallback, result);

        return result;
    }

    /**
     * 从工具执行上下文构建 ReAct 步骤
     */
    private void buildReActStepsFromToolContext(ReActContext reactContext, 
            Consumer<ThoughtEvent> thinkingCallback, String finalResult) {
        ToolExecutionContext toolContext = ToolExecutionContext.current();
        int stepNumber = 1;

        for (AgentResponse.ExecutionStep step : toolContext.getExecutionSteps()) {
            // Thought: LLM 决定调用工具
            reactContext.addStep(ReActStep.thought(stepNumber++, 
                    "决定调用工具: " + step.getStage()));

            thinkingCallback.accept(ThoughtEvent.toolSelected(step.getStage(), 
                    step.getInput() != null ? step.getInput().toString() : ""));

            // Action: 执行工具
            reactContext.addStep(ReActStep.action(stepNumber++, 
                    step.getStage(), 
                    step.getInput() != null ? step.getInput().toString() : ""));

            thinkingCallback.accept(ThoughtEvent.toolExecuting(step.getStage()));

            // Observation: 观察结果
            String output = step.getOutput() != null ? step.getOutput().toString() : "";
            reactContext.addStep(ReActStep.observation(stepNumber++, 
                    output, step.isSuccess(), step.getErrorMessage()));

            thinkingCallback.accept(ThoughtEvent.toolResult(step.getStage(), 
                    output, step.isSuccess(), step.getErrorMessage()));

            reactContext.incrementIteration();
        }

        // 最终答案 - 使用传入的 finalResult
        thinkingCallback.accept(ThoughtEvent.finalAnswer(finalResult));
    }

    /**
     * 执行需要编排的复杂任务（内部方法）
     */
    public AgentResponse executeOrchestrated(AgentRequest request) {
        return executeOrchestrated(request, ThoughtEvent.ThinkingCallback.noop());
    }

    /**
     * 执行需要编排的复杂任务（带思考过程回调）
     */
    public AgentResponse executeOrchestrated(AgentRequest request, Consumer<ThoughtEvent> thinkingCallback) {
        if (orchestrator == null) {
            return execute(request, thinkingCallback);
        }

        return orchestrator.orchestrate(request);
    }

    /**
     * 流式执行
     */
    public void executeStreaming(AgentRequest request, StreamingCallback callback) {
        executeStreaming(request, callback, ThoughtEvent.ThinkingCallback.noop());
    }

    /**
     * 流式执行（带思考过程回调）
     */
    public void executeStreaming(AgentRequest request, StreamingCallback callback, 
            Consumer<ThoughtEvent> thinkingCallback) {
        try {
            AgentAssistant assistant = request.getSessionId() != null
                    ? assistantFactory.getAssistantWithMemory(request.getSessionId())
                    : assistantFactory.getAssistant();

            String result = assistant.chat(request.getQuery());
            callback.onComplete(result);

        } catch (Exception e) {
            log.error("[ReActEngine] 流式执行失败", e);
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
     * 判断是否需要反思
     */
    private boolean shouldReflect(ReActContext context) {
        // 工具调用次数大于0时进行反思
        return context.getToolCallCount() > 0;
    }

    /**
     * 构建响应
     */
    private AgentResponse buildResponse(String sessionId, String requestId, String result,
            long startTime, ToolExecutionContext toolContext, ReActContext reactContext) {
        
        AgentResponse response = AgentResponse.builder()
                .sessionId(sessionId)
                .output(result)
                .success(true)
                .executionTime(System.currentTimeMillis() - startTime)
                .steps(toolContext.getExecutionSteps())
                .usedPlugins(toolContext.getUsedPlugins())
                .build();
        
        response.getMetadata().put("requestId", requestId);
        response.getMetadata().put("mode", "react");
        response.getMetadata().put("iterations", reactContext.getCurrentIteration());
        response.getMetadata().put("toolCalls", reactContext.getToolCallCount());

        return response;
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
                    log.warn("EventListener {} 处理失败: {}", listener.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * 转义模板变量
     */
    private String escapeTemplateVariables(String input) {
        if (input == null) {
            return null;
        }
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
