package com.eazyai.ai.nexus.api.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * 思考过程事件
 * 用于流式暴露 LLM 的思考过程
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 开始思考
         */
        THINKING_START,
        /**
         * 思考内容
         */
        THOUGHT,
        /**
         * 选择工具
         */
        TOOL_SELECTED,
        /**
         * 工具执行中
         */
        TOOL_EXECUTING,
        /**
         * 工具执行完成
         */
        TOOL_RESULT,
        /**
         * 开始反思
         */
        REFLECTION_START,
        /**
         * 反思结果
         */
        REFLECTION,
        /**
         * 得出最终答案
         */
        FINAL_ANSWER,
        /**
         * 错误
         */
        ERROR
    }

    /**
     * 事件类型
     */
    private EventType type;

    /**
     * 思考内容
     */
    private String content;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具输入
     */
    private String toolInput;

    /**
     * 工具输出
     */
    private String toolOutput;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 创建思考事件
     */
    public static ThoughtEvent thought(String content) {
        return ThoughtEvent.builder()
                .type(EventType.THOUGHT)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建工具选择事件
     */
    public static ThoughtEvent toolSelected(String toolName, String toolInput) {
        return ThoughtEvent.builder()
                .type(EventType.TOOL_SELECTED)
                .toolName(toolName)
                .toolInput(toolInput)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建工具执行事件
     */
    public static ThoughtEvent toolExecuting(String toolName) {
        return ThoughtEvent.builder()
                .type(EventType.TOOL_EXECUTING)
                .toolName(toolName)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建工具结果事件
     */
    public static ThoughtEvent toolResult(String toolName, String output, boolean success, String errorMessage) {
        return ThoughtEvent.builder()
                .type(EventType.TOOL_RESULT)
                .toolName(toolName)
                .toolOutput(output)
                .success(success)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建最终答案事件
     */
    public static ThoughtEvent finalAnswer(String answer) {
        return ThoughtEvent.builder()
                .type(EventType.FINAL_ANSWER)
                .content(answer)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建错误事件
     */
    public static ThoughtEvent error(String errorMessage) {
        return ThoughtEvent.builder()
                .type(EventType.ERROR)
                .errorMessage(errorMessage)
                .success(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 思考过程回调接口
     */
    @FunctionalInterface
    public interface ThinkingCallback extends Consumer<ThoughtEvent> {
        /**
         * 处理思考事件
         */
        @Override
        void accept(ThoughtEvent event);

        /**
         * 空回调
         */
        static ThinkingCallback noop() {
            return event -> {};
        }
    }
}
