package com.eazyai.ai.nexus.api.dto;

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
 * 智能体响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID，用于保持上下文
     */
    private String sessionId;

    /**
     * 是否成功
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 输出结果
     */
    private String output;

    /**
     * 结构化输出
     */
    private Object structuredOutput;

    /**
     * 执行步骤详情
     */
    @Builder.Default
    private List<ExecutionStep> steps = new ArrayList<>();

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private long executionTime;

    /**
     * Token使用量
     */
    private TokenUsage tokenUsage;

    /**
     * 使用的插件列表
     */
    @Builder.Default
    private List<String> usedPlugins = new ArrayList<>();

    /**
     * 扩展数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 执行步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionStep implements Serializable {
        private static final long serialVersionUID = 1L;

        private int stepNumber;
        private String stage;           // 阶段名称
        private String description;     // 描述
        private Object input;           // 输入
        private Object output;          // 输出
        private long startTime;
        private long endTime;
        private boolean success;
        private String errorMessage;
    }

    /**
     * Token使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage implements Serializable {
        private static final long serialVersionUID = 1L;

        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }

    /**
     * 创建成功响应
     */
    public static AgentResponse success(String output) {
        return AgentResponse.builder()
                .success(true)
                .output(output)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static AgentResponse error(String errorMessage) {
        return AgentResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
