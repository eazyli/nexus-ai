package com.eazyai.ai.nexus.api.react;

import com.eazyai.ai.nexus.api.intent.IntentResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ReAct 执行上下文
 * 维护完整的思考-行动-观察循环状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 最大迭代次数
     */
    public static final int MAX_ITERATIONS = 10;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户原始输入
     */
    private String userInput;

    /**
     * 执行步骤历史
     */
    @Builder.Default
    private List<ReActStep> steps = new ArrayList<>();

    /**
     * 当前迭代次数
     */
    @Builder.Default
    private int currentIteration = 0;

    /**
     * 是否已完成
     */
    @Builder.Default
    private boolean completed = false;

    /**
     * 最终答案
     */
    private String finalAnswer;

    /**
     * 意图分析结果
     */
    private IntentResult intentResult;

    /**
     * 添加步骤
     */
    public void addStep(ReActStep step) {
        steps.add(step);
    }

    /**
     * 获取最后的思考步骤
     */
    public ReActStep getLastThought() {
        for (int i = steps.size() - 1; i >= 0; i--) {
            if (steps.get(i).getType() == ReActStep.StepType.THOUGHT) {
                return steps.get(i);
            }
        }
        return null;
    }

    /**
     * 获取最后的观察步骤
     */
    public ReActStep getLastObservation() {
        for (int i = steps.size() - 1; i >= 0; i--) {
            if (steps.get(i).getType() == ReActStep.StepType.OBSERVATION) {
                return steps.get(i);
            }
        }
        return null;
    }

    /**
     * 是否达到最大迭代次数
     */
    public boolean reachedMaxIterations() {
        return currentIteration >= MAX_ITERATIONS;
    }

    /**
     * 增加迭代计数
     */
    public void incrementIteration() {
        currentIteration++;
    }

    /**
     * 获取格式化的执行历史（用于LLM上下文）
     */
    public String formatHistory() {
        StringBuilder sb = new StringBuilder();
        for (ReActStep step : steps) {
            switch (step.getType()) {
                case THOUGHT:
                    sb.append("Thought: ").append(step.getThought()).append("\n");
                    break;
                case ACTION:
                    sb.append("Action: ").append(step.getToolName())
                            .append("(").append(step.getToolInput()).append(")\n");
                    break;
                case OBSERVATION:
                    sb.append("Observation: ").append(step.getObservation()).append("\n");
                    break;
                case REFLECTION:
                    sb.append("Reflection: ").append(step.getThought()).append("\n");
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 获取工具调用次数
     */
    public int getToolCallCount() {
        return (int) steps.stream()
                .filter(s -> s.getType() == ReActStep.StepType.ACTION)
                .count();
    }

    /**
     * 获取使用的工具列表
     */
    public List<String> getUsedTools() {
        return steps.stream()
                .filter(s -> s.getType() == ReActStep.StepType.ACTION)
                .map(ReActStep::getToolName)
                .distinct()
                .toList();
    }
}
