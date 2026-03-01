package com.eazyai.ai.nexus.api.planner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 计划ID
     */
    private String planId;

    /**
     * 计划名称
     */
    private String name;

    /**
     * 计划描述
     */
    private String description;

    /**
     * 执行步骤列表
     */
    @Builder.Default
    private List<TaskStep> steps = new ArrayList<>();

    /**
     * 依赖关系图
     */
    @Builder.Default
    private Map<String, List<String>> dependencies = new HashMap<>();

    /**
     * 执行策略
     */
    @Builder.Default
    private ExecutionStrategy strategy = ExecutionStrategy.SEQUENTIAL;

    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeout = 60000;

    /**
     * 执行策略枚举
     */
    public enum ExecutionStrategy {
        SEQUENTIAL,     // 顺序执行
        PARALLEL,       // 并行执行
        PIPELINE        // 管道执行
    }

    /**
     * 任务步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStep implements Serializable {
        private static final long serialVersionUID = 1L;

        private String stepId;
        private String name;
        private String description;
        private String pluginType;      // 需要使用的插件类型
        private String pluginName;      // 指定插件名称（可选）
        private Map<String, Object> params;  // 执行参数
        private int retryCount;         // 重试次数
        private boolean isCritical;     // 是否关键步骤（失败则整个计划失败）
    }

    /**
     * 添加步骤
     */
    public TaskPlan addStep(TaskStep step) {
        this.steps.add(step);
        return this;
    }

    /**
     * 添加依赖关系
     */
    public TaskPlan addDependency(String stepId, List<String> dependsOn) {
        this.dependencies.put(stepId, dependsOn);
        return this;
    }
}
