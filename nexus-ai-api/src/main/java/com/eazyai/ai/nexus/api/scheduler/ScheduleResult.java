package com.eazyai.ai.nexus.api.scheduler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调度结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 各步骤执行结果
     */
    private List<StepResult> stepResults;

    /**
     * 执行时间（毫秒）
     */
    private long executionTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        PENDING,        // 等待中
        RUNNING,        // 执行中
        COMPLETED,      // 完成
        FAILED,         // 失败
        CANCELLED,      // 已取消
        TIMEOUT         // 超时
    }

    /**
     * 步骤执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private String stepId;
        private String pluginName;
        private boolean success;
        private Object output;
        private String errorMessage;
        private long startTime;
        private long endTime;
        private int retryCount;
    }
}
