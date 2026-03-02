package com.eazyai.ai.nexus.api.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 反思结果
 * LLM 对执行过程的评估和改进建议
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReflectionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 置信度 (0.0 - 1.0)
     */
    private double confidence;

    /**
     * 发现的问题
     */
    @Builder.Default
    private List<String> issues = new ArrayList<>();

    /**
     * 改进建议
     */
    @Builder.Default
    private List<String> improvements = new ArrayList<>();

    /**
     * 是否需要重试
     */
    private boolean shouldRetry;

    /**
     * 替代方案描述
     */
    private String alternativeApproach;

    /**
     * 反思总结
     */
    private String summary;

    /**
     * 创建成功的反思结果
     */
    public static ReflectionResult success(String summary, double confidence) {
        return ReflectionResult.builder()
                .success(true)
                .confidence(confidence)
                .summary(summary)
                .build();
    }

    /**
     * 创建失败的反思结果
     */
    public static ReflectionResult failure(String issue, boolean shouldRetry) {
        ReflectionResult result = ReflectionResult.builder()
                .success(false)
                .confidence(0.0)
                .shouldRetry(shouldRetry)
                .build();
        result.getIssues().add(issue);
        return result;
    }

    /**
     * 添加问题
     */
    public void addIssue(String issue) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        issues.add(issue);
    }

    /**
     * 添加改进建议
     */
    public void addImprovement(String improvement) {
        if (improvements == null) {
            improvements = new ArrayList<>();
        }
        improvements.add(improvement);
    }
}
