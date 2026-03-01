package com.eazyai.ai.nexus.core.planner.strategy;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.intent.IntentResult;
import com.eazyai.ai.nexus.api.planner.TaskPlan;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LLM驱动的智能路由（简化版）
 * 
 * <p>说明：</p>
 * 简单任务已被 AiServices 自动处理，此路由器仅用于需要显式编排的高级场景。
 * 
 * <p>使用场景：</p>
 * <ul>
 *   <li>多插件协作编排</li>
 *   <li>需要精确控制执行顺序</li>
 *   <li>需要 Pipeline 模式传递中间结果</li>
 * </ul>
 */
@Slf4j
@Component
public class LlmDrivenRouter {

    @Autowired
    private PluginRegistry pluginRegistry;

    /**
     * 创建任务计划
     * 基于意图和可用插件生成执行计划
     */
    public TaskPlan createPlan(IntentResult intent, AgentContext context) {
        String userInput = context.getUserInput();
        log.info("创建任务计划: {}", userInput);

        List<PluginDescriptor> plugins = getAvailablePlugins();
        if (plugins.isEmpty()) {
            log.warn("没有可用的插件");
            return createEmptyPlan(userInput);
        }

        // 简化：根据意图建议的插件直接构建计划
        List<String> suggestedPlugins = parseSuggestedPlugins(intent, plugins);
        log.info("建议插件: {}", suggestedPlugins);

        return buildPlan(suggestedPlugins, intent, context, plugins);
    }

    /**
     * 获取可用插件列表
     */
    private List<PluginDescriptor> getAvailablePlugins() {
        return pluginRegistry.getAllPlugins().stream()
                .filter(PluginDescriptor::isEnabled)
                .toList();
    }

    /**
     * 解析意图中建议的插件
     */
    private List<String> parseSuggestedPlugins(IntentResult intent, List<PluginDescriptor> plugins) {
        String suggestedTaskType = intent.getSuggestedTaskType();
        if (suggestedTaskType == null || suggestedTaskType.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> validIds = plugins.stream()
                .map(PluginDescriptor::getId)
                .collect(Collectors.toSet());

        return Arrays.stream(suggestedTaskType.split(","))
                .map(String::trim)
                .filter(validIds::contains)
                .collect(Collectors.toList());
    }

    /**
     * 构建任务计划
     */
    private TaskPlan buildPlan(List<String> pluginIds, IntentResult intent,
                               AgentContext context, List<PluginDescriptor> plugins) {
        List<TaskPlan.TaskStep> steps = new ArrayList<>();

        Map<String, PluginDescriptor> pluginMap = plugins.stream()
                .collect(Collectors.toMap(PluginDescriptor::getId, p -> p));

        for (int i = 0; i < pluginIds.size(); i++) {
            String pluginId = pluginIds.get(i);
            PluginDescriptor descriptor = pluginMap.get(pluginId);

            TaskPlan.TaskStep step = TaskPlan.TaskStep.builder()
                    .stepId("step_" + (i + 1) + "_" + pluginId)
                    .name(pluginId)
                    .description(descriptor != null ? descriptor.getDescription() : "Execute " + pluginId)
                    .pluginType(pluginId)
                    .params(buildParams(pluginId, context, intent, descriptor))
                    .isCritical(true)
                    .retryCount(3)
                    .build();

            steps.add(step);
        }

        return TaskPlan.builder()
                .planId(UUID.randomUUID().toString())
                .name("Plugin Orchestration Plan")
                .description("插件编排执行计划")
                .strategy(TaskPlan.ExecutionStrategy.SEQUENTIAL)
                .steps(steps)
                .build();
    }

    /**
     * 构建插件参数
     */
    private Map<String, Object> buildParams(String pluginId, AgentContext context,
                                            IntentResult intent, PluginDescriptor descriptor) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", context.getUserInput());
        params.put("sessionId", context.getSessionId());
        params.put("userId", context.getUserId());

        if (intent.getEntities() != null) {
            params.putAll(intent.getEntities());
        }

        if (descriptor != null && descriptor.getParameters() != null) {
            for (PluginDescriptor.ParameterDef param : descriptor.getParameters()) {
                if (!params.containsKey(param.getName()) && param.getDefaultValue() != null) {
                    params.put(param.getName(), param.getDefaultValue());
                }
            }
        }

        return params;
    }

    /**
     * 创建空计划
     */
    private TaskPlan createEmptyPlan(String userInput) {
        return TaskPlan.builder()
                .planId(UUID.randomUUID().toString())
                .name("Empty Plan")
                .description("没有可用插件")
                .strategy(TaskPlan.ExecutionStrategy.SEQUENTIAL)
                .steps(new ArrayList<>())
                .build();
    }
}
