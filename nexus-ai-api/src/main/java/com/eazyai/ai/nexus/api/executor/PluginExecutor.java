package com.eazyai.ai.nexus.api.executor;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 插件执行器接口
 * 负责插件的实际执行
 */
public interface PluginExecutor {

    /**
     * 执行插件
     *
     * @param plugin 插件实例
     * @param params 执行参数
     * @param context 执行上下文
     * @return 执行结果
     */
    ExecutionResult execute(Plugin plugin, Map<String, Object> params, AgentContext context);

    /**
     * 异步执行插件
     *
     * @param plugin 插件实例
     * @param params 执行参数
     * @param context 执行上下文
     * @return CompletableFuture
     */
    CompletableFuture<ExecutionResult> executeAsync(Plugin plugin, Map<String, Object> params, AgentContext context);

    /**
     * 批量执行
     *
     * @param tasks 执行任务列表
     * @param context 执行上下文
     * @return 执行结果列表
     */
    default java.util.List<ExecutionResult> executeBatch(java.util.List<ExecutionTask> tasks, AgentContext context) {
        return tasks.stream()
                .map(task -> execute(task.getPlugin(), task.getParams(), context))
                .toList();
    }

    /**
     * 执行任务封装
     */
    interface ExecutionTask {
        Plugin getPlugin();
        Map<String, Object> getParams();
    }
}
