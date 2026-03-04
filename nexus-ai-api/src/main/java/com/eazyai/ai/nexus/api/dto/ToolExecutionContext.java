package com.eazyai.ai.nexus.api.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具执行上下文
 * 使用 InheritableThreadLocal 存储每次请求的工具调用记录
 * 
 * <p>使用 InheritableThreadLocal 的原因：</p>
 * <ul>
 *   <li>支持异步线程池中的上下文传递</li>
 *   <li>流式执行时子线程可以访问父线程的上下文</li>
 * </ul>
 * 
 * <p>使用方式：</p>
 * <pre>
 * // 请求开始时初始化
 * ToolExecutionContext.init();
 * 
 * // 工具执行时记录
 * ToolExecutionContext.current().recordToolCall(...);
 * 
 * // 请求结束时获取记录并清理
 * List<ExecutionStep> steps = ToolExecutionContext.current().getExecutionSteps();
 * ToolExecutionContext.clear();
 * </pre>
 */
public class ToolExecutionContext {

    /**
     * 使用 InheritableThreadLocal 支持子线程继承上下文
     * 解决异步线程池中 ThreadLocal 丢失的问题
     */
    private static final InheritableThreadLocal<ToolExecutionContext> CONTEXT = new InheritableThreadLocal<>();

    @Getter
    private final List<ToolCallRecord> toolCalls = new ArrayList<>();

    @Getter
    private final List<String> usedTools = new ArrayList<>();

    private final long startTime;

    /**
     * 工具调用记录
     */
    @Getter
    public static class ToolCallRecord {
        private final String toolName;
        private final String description;
        private final Object input;
        private final Object output;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;

        public ToolCallRecord(String toolName, String description, Object input, 
                             Object output, long executionTime, boolean success, String errorMessage) {
            this.toolName = toolName;
            this.description = description;
            this.input = input;
            this.output = output;
            this.executionTime = executionTime;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        /**
         * 转换为 ExecutionStep
         */
        public AgentResponse.ExecutionStep toExecutionStep(int stepNumber, long requestStartTime) {
            return AgentResponse.ExecutionStep.builder()
                    .stepNumber(stepNumber)
                    .stage("tool_call")
                    .description(description)
                    .input(input)
                    .output(output)
                    .startTime(requestStartTime)
                    .endTime(requestStartTime + executionTime)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();
        }
    }

    private ToolExecutionContext() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 初始化上下文（请求开始时调用）
     */
    public static void init() {
        CONTEXT.set(new ToolExecutionContext());
    }

    /**
     * 获取当前上下文
     */
    public static ToolExecutionContext current() {
        ToolExecutionContext ctx = CONTEXT.get();
        if (ctx == null) {
            // 如果没有初始化，自动初始化一个
            ctx = new ToolExecutionContext();
            CONTEXT.set(ctx);
        }
        return ctx;
    }

    /**
     * 记录工具调用
     */
    public void recordToolCall(String toolName, String description, Object input, 
                               Object output, long executionTime, boolean success, String errorMessage) {
        toolCalls.add(new ToolCallRecord(toolName, description, input, output, executionTime, success, errorMessage));
        if (success && !usedTools.contains(toolName)) {
            usedTools.add(toolName);
        }
    }

    /**
     * 记录成功的工具调用
     */
    public void recordToolCall(String toolName, String description, Object input, Object output, long executionTime) {
        recordToolCall(toolName, description, input, output, executionTime, true, null);
    }

    /**
     * 记录失败的工具调用
     */
    public void recordToolCallFailure(String toolName, String description, Object input, 
                                      String errorMessage, long executionTime) {
        recordToolCall(toolName, description, input, null, executionTime, false, errorMessage);
    }

    /**
     * 获取所有执行步骤
     */
    public List<AgentResponse.ExecutionStep> getExecutionSteps() {
        List<AgentResponse.ExecutionStep> steps = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            steps.add(toolCalls.get(i).toExecutionStep(i + 1, startTime));
        }
        return steps;
    }

    /**
     * 清理上下文（请求结束时调用）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 检查是否有工具调用
     */
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
