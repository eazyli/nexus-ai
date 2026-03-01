package com.eazyai.ai.nexus.core.scheduler;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.executor.ExecutionResult;
import com.eazyai.ai.nexus.api.executor.PluginExecutor;
import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.planner.TaskPlan;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import com.eazyai.ai.nexus.api.scheduler.PluginScheduler;
import com.eazyai.ai.nexus.api.scheduler.ScheduleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * 默认插件调度器
 * 负责任务的调度和执行编排
 */
@Slf4j
@Component
public class DefaultPluginScheduler implements PluginScheduler {

    @Autowired
    private PluginRegistry registry;

    @Autowired
    private PluginExecutor executor;

    private final Map<String, ScheduleResult.ExecutionStatus> executionStatuses = new ConcurrentHashMap<>();

    @Override
    public ScheduleResult schedule(TaskPlan plan, AgentContext context) {
        String executionId = java.util.UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        executionStatuses.put(executionId, ScheduleResult.ExecutionStatus.RUNNING);

        List<ScheduleResult.StepResult> stepResults = new ArrayList<>();
        boolean overallSuccess = true;

        try {
            switch (plan.getStrategy()) {
                case SEQUENTIAL:
                    stepResults = executeSequential(plan, context);
                    break;
                case PARALLEL:
                    stepResults = executeParallel(plan, context);
                    break;
                case PIPELINE:
                    stepResults = executePipeline(plan, context);
                    break;
            }

            overallSuccess = stepResults.stream().allMatch(ScheduleResult.StepResult::isSuccess);

            if (!overallSuccess) {
                // 检查是否有关键步骤失败
                for (int i = 0; i < stepResults.size(); i++) {
                    if (!stepResults.get(i).isSuccess() && plan.getSteps().get(i).isCritical()) {
                        executionStatuses.put(executionId, ScheduleResult.ExecutionStatus.FAILED);
                        return ScheduleResult.builder()
                                .executionId(executionId)
                                .success(false)
                                .status(ScheduleResult.ExecutionStatus.FAILED)
                                .stepResults(stepResults)
                                .executionTime(System.currentTimeMillis() - startTime)
                                .errorMessage("Critical step failed: " + plan.getSteps().get(i).getStepId())
                                .build();
                    }
                }
            }

            executionStatuses.put(executionId, ScheduleResult.ExecutionStatus.COMPLETED);

            return ScheduleResult.builder()
                    .executionId(executionId)
                    .success(true)
                    .status(ScheduleResult.ExecutionStatus.COMPLETED)
                    .stepResults(stepResults)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Schedule execution failed", e);
            executionStatuses.put(executionId, ScheduleResult.ExecutionStatus.FAILED);
            return ScheduleResult.builder()
                    .executionId(executionId)
                    .success(false)
                    .status(ScheduleResult.ExecutionStatus.FAILED)
                    .stepResults(stepResults)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public CompletableFuture<ScheduleResult> scheduleAsync(TaskPlan plan, AgentContext context) {
        return CompletableFuture.supplyAsync(() -> schedule(plan, context));
    }

    @Override
    public boolean cancel(String executionId) {
        executionStatuses.put(executionId, ScheduleResult.ExecutionStatus.CANCELLED);
        return true;
    }

    @Override
    public ScheduleResult.ExecutionStatus getStatus(String executionId) {
        return executionStatuses.getOrDefault(executionId, ScheduleResult.ExecutionStatus.PENDING);
    }

    /**
     * 顺序执行
     */
    private List<ScheduleResult.StepResult> executeSequential(TaskPlan plan, AgentContext context) {
        List<ScheduleResult.StepResult> results = new ArrayList<>();

        for (TaskPlan.TaskStep step : plan.getSteps()) {
            ScheduleResult.StepResult result = executeStep(step, context);
            results.add(result);

            // 如果关键步骤失败，停止执行
            if (!result.isSuccess() && step.isCritical()) {
                log.error("Critical step {} failed, stopping execution", step.getStepId());
                break;
            }
        }

        return results;
    }

    /**
     * 并行执行
     */
    private List<ScheduleResult.StepResult> executeParallel(TaskPlan plan, AgentContext context) {
        List<CompletableFuture<ScheduleResult.StepResult>> futures = plan.getSteps().stream()
                .map(step -> CompletableFuture.supplyAsync(() -> executeStep(step, context)))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    /**
     * 管道执行（上一步的输出作为下一步的输入）
     */
    private List<ScheduleResult.StepResult> executePipeline(TaskPlan plan, AgentContext context) {
        List<ScheduleResult.StepResult> results = new ArrayList<>();
        Object previousOutput = null;

        for (TaskPlan.TaskStep step : plan.getSteps()) {
            // 将上一步的输出注入到当前步骤参数
            if (previousOutput != null) {
                step.getParams().put("_previous_output", previousOutput);
            }

            ScheduleResult.StepResult result = executeStep(step, context);
            results.add(result);

            if (result.isSuccess()) {
                previousOutput = result.getOutput();
            } else if (step.isCritical()) {
                break;
            }
        }

        return results;
    }

    /**
     * 执行单个步骤
     */
    private ScheduleResult.StepResult executeStep(TaskPlan.TaskStep step, AgentContext context) {
        long startTime = System.currentTimeMillis();

        // 查找插件
        List<Plugin> plugins = registry.findByType(step.getPluginType());
        if (plugins.isEmpty()) {
            return ScheduleResult.StepResult.builder()
                    .stepId(step.getStepId())
                    .pluginName(step.getPluginType())
                    .success(false)
                    .errorMessage("No plugin found for type: " + step.getPluginType())
                    .startTime(startTime)
                    .endTime(System.currentTimeMillis())
                    .build();
        }

        // 使用第一个匹配的插件
        Plugin plugin = plugins.get(0);

        // 执行并重试
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= step.getRetryCount()) {
            try {
                ExecutionResult execResult = executor.execute(plugin, step.getParams(), context);

                return ScheduleResult.StepResult.builder()
                        .stepId(step.getStepId())
                        .pluginName(plugin.getDescriptor().getName())
                        .success(execResult.isSuccess())
                        .output(execResult.getData())
                        .errorMessage(execResult.getErrorMessage())
                        .startTime(startTime)
                        .endTime(System.currentTimeMillis())
                        .retryCount(retryCount)
                        .build();

            } catch (Exception e) {
                lastException = e;
                retryCount++;
                log.warn("Step {} execution failed (attempt {}), retrying...", step.getStepId(), retryCount);

                if (retryCount <= step.getRetryCount()) {
                    try {
                        Thread.sleep(1000 * retryCount); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return ScheduleResult.StepResult.builder()
                .stepId(step.getStepId())
                .pluginName(plugin.getDescriptor().getName())
                .success(false)
                .errorMessage(lastException != null ? lastException.getMessage() : "Unknown error")
                .startTime(startTime)
                .endTime(System.currentTimeMillis())
                .retryCount(retryCount)
                .build();
    }
}
