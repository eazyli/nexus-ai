package com.eazyai.ai.nexus.api.scheduler;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.planner.TaskPlan;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 插件调度器接口
 * 负责任务的调度和执行编排
 */
public interface PluginScheduler {

    /**
     * 调度执行任务计划
     *
     * @param plan 任务计划
     * @param context 执行上下文
     * @return 调度结果
     */
    ScheduleResult schedule(TaskPlan plan, AgentContext context);

    /**
     * 异步调度执行
     *
     * @param plan 任务计划
     * @param context 执行上下文
     * @return CompletableFuture
     */
    CompletableFuture<ScheduleResult> scheduleAsync(TaskPlan plan, AgentContext context);

    /**
     * 取消执行
     *
     * @param executionId 执行ID
     * @return 是否取消成功
     */
    boolean cancel(String executionId);

    /**
     * 获取执行状态
     *
     * @param executionId 执行ID
     * @return 执行状态
     */
    ScheduleResult.ExecutionStatus getStatus(String executionId);
}
