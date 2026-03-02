package com.eazyai.ai.nexus.core.planner;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.api.integrator.ResultIntegrator;
import com.eazyai.ai.nexus.api.observability.AgentEvent;
import com.eazyai.ai.nexus.api.observability.EventListener;
import com.eazyai.ai.nexus.api.planner.TaskPlan;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import com.eazyai.ai.nexus.api.scheduler.PluginScheduler;
import com.eazyai.ai.nexus.api.scheduler.ScheduleResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 内部编排器
 * 
 * <p>用于处理需要显式编排的复杂任务，不暴露给外部 API。</p>
 * 
 * <p>使用场景：</p>
 * <ul>
 *   <li>需要精确控制执行顺序的多步骤任务</li>
 *   <li>需要 Pipeline 模式传递中间结果</li>
 *   <li>需要并行执行多个独立任务</li>
 * </ul>
 * 
 * <p>说明：此组件是内部实现细节，用户应通过 ReActEngine 统一入口访问。</p>
 */
@Slf4j
@Component
public class InternalOrchestrator {

    private static final String PLAN_GENERATION_PROMPT = """
            分析以下任务，生成执行计划。
            
            用户请求: %s
            
            可用插件:
            %s
            
            请以JSON格式输出执行计划：
            ```json
            {
              "strategy": "SEQUENTIAL/PARALLEL/PIPELINE",
              "steps": [
                {
                  "pluginId": "插件ID",
                  "description": "步骤描述",
                  "params": {"参数名": "参数值"}
                }
              ]
            }
            ```
            
            只输出JSON，不要包含其他内容。
            """;

    @Autowired(required = false)
    private PluginScheduler scheduler;

    @Autowired(required = false)
    private ResultIntegrator integrator;

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired(required = false)
    private ChatLanguageModel chatModel;

    @Autowired(required = false)
    private List<EventListener> eventListeners;

    /**
     * 编排执行复杂任务
     */
    public AgentResponse orchestrate(AgentRequest request) {
        if (scheduler == null || integrator == null) {
            log.warn("[InternalOrchestrator] 调度器或整合器不可用");
            return AgentResponse.error("编排能力不可用");
        }

        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        try {
            // 1. 生成执行计划
            TaskPlan plan = generatePlan(request);
            log.info("[InternalOrchestrator] 生成计划: {} 步骤", plan.getSteps().size());

            // 2. 创建上下文
            AgentContext context = AgentContext.builder()
                    .requestId(requestId)
                    .sessionId(request.getSessionId())
                    .userInput(request.getQuery())
                    .currentStage(AgentContext.ExecutionStage.SCHEDULING)
                    .build();

            // 3. 执行调度
            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.SCHEDULING)
                    .stage("orchestrated_scheduling")
                    .build());

            ScheduleResult scheduleResult = scheduler.schedule(plan, context);
            context.setCurrentStage(AgentContext.ExecutionStage.INTEGRATING);

            // 4. 整合结果
            publishEvent(AgentEvent.create(requestId, AgentEvent.EventType.INTEGRATION)
                    .stage("orchestrated_integration")
                    .build());

            AgentResponse response = integrator.integrate(scheduleResult, context);
            context.setCurrentStage(AgentContext.ExecutionStage.COMPLETED);

            response.setExecutionTime(System.currentTimeMillis() - startTime);
            response.getMetadata().put("requestId", requestId);
            response.getMetadata().put("mode", "orchestrated");

            return response;

        } catch (Exception e) {
            log.error("[InternalOrchestrator] 编排执行失败", e);
            return AgentResponse.error(e.getMessage());
        }
    }

    /**
     * 生成执行计划
     */
    private TaskPlan generatePlan(AgentRequest request) {
        if (chatModel == null) {
            log.warn("[InternalOrchestrator] ChatModel 不可用，使用默认计划");
            return createDefaultPlan(request);
        }

        try {
            String pluginsInfo = formatAvailablePlugins();
            String prompt = String.format(PLAN_GENERATION_PROMPT, 
                    request.getQuery(), pluginsInfo);

            String response = chatModel.generate(prompt);
            return parsePlan(response, request);

        } catch (Exception e) {
            log.warn("[InternalOrchestrator] LLM 生成计划失败，使用默认计划", e);
            return createDefaultPlan(request);
        }
    }

    /**
     * 格式化可用插件信息
     */
    private String formatAvailablePlugins() {
        StringBuilder sb = new StringBuilder();
        for (var plugin : pluginRegistry.getAllPlugins()) {
            if (plugin.isEnabled()) {
                sb.append("- ").append(plugin.getId())
                        .append(": ").append(plugin.getDescription())
                        .append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 解析 LLM 生成的计划
     */
    private TaskPlan parsePlan(String response, AgentRequest request) {
        // 简化实现：使用默认计划
        // 实际项目中应解析 JSON 并构建 TaskPlan
        return createDefaultPlan(request);
    }

    /**
     * 创建默认计划
     */
    private TaskPlan createDefaultPlan(AgentRequest request) {
        return TaskPlan.builder()
                .planId(UUID.randomUUID().toString())
                .name("Default Orchestration Plan")
                .description("默认编排计划")
                .strategy(TaskPlan.ExecutionStrategy.SEQUENTIAL)
                .steps(new ArrayList<>())
                .build();
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
}
