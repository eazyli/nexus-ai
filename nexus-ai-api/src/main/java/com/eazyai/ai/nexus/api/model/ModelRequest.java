package com.eazyai.ai.nexus.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型请求
 */
@Data
@Builder
public class ModelRequest {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 场景ID
     */
    private String sceneId;

    /**
     * 用户输入
     */
    private String input;

    /**
     * 对话历史
     */
    private List<Message> messages;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 工具定义列表
     */
    private List<ToolDefinition> tools;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大输出token数
     */
    private Integer maxTokens;

    /**
     * 是否流式输出
     */
    private Boolean streaming;

    /**
     * 响应格式 (text, json)
     */
    private String responseFormat;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;

    /**
     * 消息定义
     */
    @Data
    @Builder
    public static class Message {
        private String role; // system, user, assistant, tool
        private String content;
        private String name;
        private String toolCallId;
        private List<ToolCall> toolCalls;
    }

    /**
     * 工具调用
     */
    @Data
    @Builder
    public static class ToolCall {
        private String id;
        private String type;
        private String name;
        private String arguments;
    }

    /**
     * 工具定义
     */
    @Data
    @Builder
    public static class ToolDefinition {
        private String type;
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }
}
