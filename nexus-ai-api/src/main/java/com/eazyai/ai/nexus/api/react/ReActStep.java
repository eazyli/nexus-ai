package com.eazyai.ai.nexus.api.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ReAct 执行步骤
 * 记录思考-行动-观察循环中的每一步
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActStep implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 步骤类型
     */
    public enum StepType {
        /**
         * 思考：分析当前状态，决定下一步行动
         */
        THOUGHT,
        /**
         * 行动：选择并调用工具
         */
        ACTION,
        /**
         * 观察：获取工具执行结果
         */
        OBSERVATION,
        /**
         * 反思：评估执行效果，调整策略
         */
        REFLECTION
    }

    /**
     * 步骤序号
     */
    private int stepNumber;

    /**
     * 步骤类型
     */
    private StepType type;

    /**
     * 思考内容（THOUGHT类型时有效）
     */
    private String thought;

    /**
     * 工具名称（ACTION类型时有效）
     */
    private String toolName;

    /**
     * 工具输入参数（ACTION类型时有效）
     */
    private String toolInput;

    /**
     * 工具输出结果（OBSERVATION类型时有效）
     */
    private String observation;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间戳
     */
    private long timestamp;

    /**
     * 执行耗时（毫秒）
     */
    private long duration;

    /**
     * 创建思考步骤
     */
    public static ReActStep thought(int stepNumber, String thought) {
        return ReActStep.builder()
                .stepNumber(stepNumber)
                .type(StepType.THOUGHT)
                .thought(thought)
                .timestamp(System.currentTimeMillis())
                .success(true)
                .build();
    }

    /**
     * 创建行动步骤
     */
    public static ReActStep action(int stepNumber, String toolName, String toolInput) {
        return ReActStep.builder()
                .stepNumber(stepNumber)
                .type(StepType.ACTION)
                .toolName(toolName)
                .toolInput(toolInput)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建观察步骤
     */
    public static ReActStep observation(int stepNumber, String observation, boolean success, String errorMessage) {
        return ReActStep.builder()
                .stepNumber(stepNumber)
                .type(StepType.OBSERVATION)
                .observation(observation)
                .success(success)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建反思步骤
     */
    public static ReActStep reflection(int stepNumber, String reflection) {
        return ReActStep.builder()
                .stepNumber(stepNumber)
                .type(StepType.REFLECTION)
                .thought(reflection)
                .timestamp(System.currentTimeMillis())
                .success(true)
                .build();
    }
}
