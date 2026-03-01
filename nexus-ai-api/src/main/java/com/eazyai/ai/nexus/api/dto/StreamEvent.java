package com.eazyai.ai.nexus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 流式事件
 * 用于 SSE 推送 AI 执行过程中的各类事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private EventType type;

    /**
     * 事件数据
     */
    private Object data;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        // 请求开始
        REQUEST_START,
        // Token 输出
        TOKEN,
        // 工具调用开始
        TOOL_CALL_START,
        // 工具调用结束
        TOOL_CALL_END,
        // 思考过程
        THINKING,
        // 请求结束
        REQUEST_END,
        // 错误
        ERROR
    }

    /**
     * 创建 Token 事件
     */
    public static StreamEvent token(String token) {
        return StreamEvent.builder()
                .type(EventType.TOKEN)
                .data(token)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建请求开始事件
     */
    public static StreamEvent requestStart(String requestId, String query) {
        return StreamEvent.builder()
                .type(EventType.REQUEST_START)
                .data(new RequestStartData(requestId, query))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建工具调用开始事件
     */
    public static StreamEvent toolCallStart(String toolName, String description, Object input) {
        return StreamEvent.builder()
                .type(EventType.TOOL_CALL_START)
                .data(new ToolCallData(toolName, description, input, null, 0, true, null))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建工具调用结束事件
     */
    public static StreamEvent toolCallEnd(String toolName, String description, Object input, 
                                          Object output, long executionTime, boolean success, String errorMessage) {
        return StreamEvent.builder()
                .type(EventType.TOOL_CALL_END)
                .data(new ToolCallData(toolName, description, input, output, executionTime, success, errorMessage))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建请求结束事件
     */
    public static StreamEvent requestEnd(String requestId, long executionTime) {
        return StreamEvent.builder()
                .type(EventType.REQUEST_END)
                .data(new RequestEndData(requestId, executionTime))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建错误事件
     */
    public static StreamEvent error(String message) {
        return StreamEvent.builder()
                .type(EventType.ERROR)
                .data(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 请求开始数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestStartData implements Serializable {
        private String requestId;
        private String query;
    }

    /**
     * 工具调用数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallData implements Serializable {
        private String toolName;
        private String description;
        private Object input;
        private Object output;
        private long executionTime;
        private boolean success;
        private String errorMessage;
    }

    /**
     * 请求结束数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestEndData implements Serializable {
        private String requestId;
        private long executionTime;
    }
}
