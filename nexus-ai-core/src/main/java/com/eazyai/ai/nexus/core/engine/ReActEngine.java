package com.eazyai.ai.nexus.core.engine;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import com.eazyai.ai.nexus.api.intent.IntentAnalyzer;
import com.eazyai.ai.nexus.api.intent.IntentResult;
import com.eazyai.ai.nexus.api.observability.AgentEvent;
import com.eazyai.ai.nexus.api.observability.EventListener;
import com.eazyai.ai.nexus.api.react.ReActContext;
import com.eazyai.ai.nexus.api.react.ReActStep;
import com.eazyai.ai.nexus.api.react.ReflectionResult;
import com.eazyai.ai.nexus.api.react.ThoughtEvent;
import com.eazyai.ai.nexus.core.assistant.AgentAssistant;
import com.eazyai.ai.nexus.core.assistant.AssistantFactory;
import com.eazyai.ai.nexus.core.assistant.DynamicToolAdapter;
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
 * ReAct 执行引擎 - 重构版
 * 
 * <p>核心改进：</p>
 * <ul>
 *   <li>使用 LangChain4j systemMessageProvider 自动注入意图上下文</li>
 *   <li>使用 @MemoryId 自动关联会话记忆</li>
 *   <li>意图分析结果直接传递给 Assistant，无需手动拼接</li>
 * </ul>
 * 
 * <p>ReAct 流程：</p>
 * <ol>
 *   <li>Think - 思考：分析用户意图，决定下一步行动</li>
 *   <li>Act - 行动：选择并调用合适的工具（LLM 自动完成）</li>
 *   <li>Observe - 观察：获取工具执行结果（LLM 自动完成）</li>
 *   <li>Reflect - 反思：评估执行效果（可选）</li>
 * </ol>
 */
@Slf4j
@Component
public class ReActEngine {

    @Autowired
    private AssistantFactory assistantFactory;

    @Autowired(required = false)
    private IntentAnalyzer intentAnalyzer;

    @Autowired(required = false)
    private ReflectionAgent reflectionAgent;

    @Autowired(required = false)
    private InternalOrchestrator orchestrator;

    @Autowired(required = false)
    private List<EventListener> eventListeners;

    private final Map<String, AgentContext> activeContexts = new ConcurrentHashMap<>();

    /**
     * 执行请求
     */
    public AgentResponse execute(AgentRequest request) {
        return execute(request, ThoughtEvent.ThinkingCallback.noop());
    }

    /**
     * 执行请求（带思考过程回调）
     */
    public AgentResponse execute(AgentRequest request, Consumer<ThoughtEvent> thinkingCallback) {
        long startTime = System.currentTimeMillis();
        String requestId = java.util.UUID.randomUUID().toString();
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
                .appId(request.getAppId())
                .sessionId(sessionId)
                .userId(request.getUserId())
                .userInput(request.getQuery())
                .currentStage(AgentContext.ExecutionStage.INTENT_ANALYSIS)
                .build();

        activeContexts.put(requestId, context);
        DynamicToolAdapter.setCurrentRequestId(requestId);
        DynamicToolAdapter.setCurrentContext(context);

        try {
            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.REQUEST_START)
                    .stage("react_start")
                    .data(Map.of("query", query, "appId", request.getAppId(), "sessionId", sessionId))
                    .build());

            thinkingCallback.accept(ThoughtEvent.builder()
                    .type(ThoughtEvent.EventType.THINKING_START)
                    .content("开始处理请求...")
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 执行 ReAct 循环
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

            ToolExecutionContext toolContext = ToolExecutionContext.current();
            AgentResponse response = buildResponse(sessionId, requestId, result, startTime, toolContext, reactContext);

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
            DynamicToolAdapter.clearCurrentRequestId();
            DynamicToolAdapter.clearCurrentContext();
        }
    }

    /**
     * ReAct 循环执行
     * 
     * <p>核心改进：意图上下文通过 systemMessageProvider 自动注入</p>
     */
    private String executeWithReActLoop(AgentRequest request, String query,
            String sessionId, ReActContext reactContext, Consumer<ThoughtEvent> thinkingCallback) {

        // 1. 意图分析阶段
        IntentResult intentResult = null;
        if (intentAnalyzer != null && request.getAppId() != null) {
            thinkingCallback.accept(ThoughtEvent.builder()
                    .type(ThoughtEvent.EventType.THINKING_START)
                    .content("正在分析用户意图...")
                    .timestamp(System.currentTimeMillis())
                    .build());

            try {
                AgentContext analysisContext = AgentContext.builder()
                        .appId(request.getAppId())
                        .userId(request.getUserId())
                        .build();
                intentResult = intentAnalyzer.analyze(request, analysisContext);
                reactContext.setIntentResult(intentResult);

                if (intentResult != null) {
                    thinkingCallback.accept(ThoughtEvent.builder()
                            .type(ThoughtEvent.EventType.THOUGHT)
                            .content(String.format("意图分析完成: %s (置信度: %.2f)",
                                    intentResult.getIntentType(), intentResult.getConfidence()))
                            .timestamp(System.currentTimeMillis())
                            .build());

                    if (!intentResult.getRecommendedTools().isEmpty()) {
                        log.info("[ReActEngine] 推荐工具: {}",
                                intentResult.getRecommendedTools().stream()
                                        .map(t -> t.getToolName() + "(" + t.getScore() + ")")
                                        .toList());
                    }
                }
            } catch (Exception e) {
                log.warn("[ReActEngine] 意图分析失败，继续执行: {}", e.getMessage());
            }
        }

        // 2. 创建 Assistant（意图上下文通过 systemMessageProvider 自动注入）
        AgentAssistant assistant = assistantFactory.createAssistant(request.getAppId(), intentResult);

        // 3. 执行对话（@MemoryId 自动关联会话记忆，意图上下文已通过 systemMessageProvider 注入）
        String result = assistant.chatWithMemory(sessionId, query);

        // 4. 从 ToolExecutionContext 构建 ReAct 步骤
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
            reactContext.addStep(ReActStep.thought(stepNumber++, "决定调用工具: " + step.getStage()));
            thinkingCallback.accept(ThoughtEvent.toolSelected(step.getStage(),
                    step.getInput() != null ? step.getInput().toString() : ""));

            reactContext.addStep(ReActStep.action(stepNumber++,
                    step.getStage(),
                    step.getInput() != null ? step.getInput().toString() : ""));
            thinkingCallback.accept(ThoughtEvent.toolExecuting(step.getStage()));

            String output = step.getOutput() != null ? step.getOutput().toString() : "";
            reactContext.addStep(ReActStep.observation(stepNumber++,
                    output, step.isSuccess(), step.getErrorMessage()));
            thinkingCallback.accept(ThoughtEvent.toolResult(step.getStage(),
                    output, step.isSuccess(), step.getErrorMessage()));

            reactContext.incrementIteration();
        }

        thinkingCallback.accept(ThoughtEvent.finalAnswer(finalResult));
    }

    /**
     * 执行需要编排的复杂任务
     */
    public AgentResponse executeOrchestrated(AgentRequest request) {
        return executeOrchestrated(request, ThoughtEvent.ThinkingCallback.noop());
    }

    public AgentResponse executeOrchestrated(AgentRequest request, Consumer<ThoughtEvent> thinkingCallback) {
        if (orchestrator == null) {
            return execute(request, thinkingCallback);
        }
        return orchestrator.orchestrate(request);
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

    private boolean shouldReflect(ReActContext context) {
        return context.getToolCallCount() > 0;
    }

    private AgentResponse buildResponse(String sessionId, String requestId, String result,
            long startTime, ToolExecutionContext toolContext, ReActContext reactContext) {

        AgentResponse response = AgentResponse.builder()
                .sessionId(sessionId)
                .output(result)
                .success(true)
                .executionTime(System.currentTimeMillis() - startTime)
                .steps(toolContext.getExecutionSteps())
                .usedTools(toolContext.getUsedTools())
                .build();

        response.getMetadata().put("requestId", requestId);
        response.getMetadata().put("mode", "react");
        response.getMetadata().put("iterations", reactContext.getCurrentIteration());
        response.getMetadata().put("toolCalls", reactContext.getToolCallCount());

        return response;
    }

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
