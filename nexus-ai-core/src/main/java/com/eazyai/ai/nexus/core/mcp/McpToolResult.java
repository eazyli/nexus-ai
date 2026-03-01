package com.eazyai.ai.nexus.core.mcp;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * MCP工具调用结果
 */
@Data
@Builder
public class McpToolResult {

    /**
     * 工具ID
     */
    private String toolId;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 返回数据
     */
    private Object data;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 执行时间(毫秒)
     */
    private long executionTime;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建成功结果
     */
    public static McpToolResult success(String toolId, Object data) {
        return McpToolResult.builder()
                .toolId(toolId)
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static McpToolResult error(String toolId, String errorCode, String errorMessage) {
        return McpToolResult.builder()
                .toolId(toolId)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
