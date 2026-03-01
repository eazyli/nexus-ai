package com.eazyai.ai.nexus.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型响应
 */
@Data
@Builder
public class ModelResponse {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 输出内容
     */
    private String content;

    /**
     * 工具调用列表
     */
    private List<ModelRequest.ToolCall> toolCalls;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 输入token数
     */
    private Integer inputTokens;

    /**
     * 输出token数
     */
    private Integer outputTokens;

    /**
     * 总token数
     */
    private Integer totalTokens;

    /**
     * 响应时间(毫秒)
     */
    private Long responseTime;

    /**
     * 完成原因 (stop, length, tool_calls, content_filter)
     */
    private String finishReason;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建成功响应
     */
    public static ModelResponse success(String requestId, String content) {
        return ModelResponse.builder()
                .requestId(requestId)
                .content(content)
                .success(true)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static ModelResponse error(String requestId, String errorCode, String errorMessage) {
        return ModelResponse.builder()
                .requestId(requestId)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
