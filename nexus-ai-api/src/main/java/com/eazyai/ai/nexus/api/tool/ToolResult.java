package com.eazyai.ai.nexus.api.tool;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 统一工具执行结果
 * 工具执行后的标准返回格式
 *
 * <p>无论工具来自哪个协议层，执行结果都统一为此格式。</p>
 * <p>协议适配层负责将此结果转换为各协议所需的格式。</p>
 *
 * <h3>结果流转：</h3>
 * <pre>
 * ToolResult（统一结果）
 *      ↓
 * MCP: McpToolResult / JSON-RPC响应
 * OpenAI: ToolCall结果格式
 * LangChain: ToolExecutionResult
 * </pre>
 */
@Data
@Builder
public class ToolResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
    private String toolId;

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 返回数据
     */
    private Object data;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 异常对象
     */
    private Throwable exception;

    /**
     * 执行耗时（毫秒）
     */
    private long executionTime;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 结果类型
     * 如: json, text, binary, stream
     */
    private String resultType;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功结果
     */
    public static ToolResult success(String toolId, Object data) {
        return ToolResult.builder()
                .toolId(toolId)
                .success(true)
                .data(data)
                .resultType("json")
                .build();
    }

    /**
     * 创建成功结果（带执行时间）
     */
    public static ToolResult success(String toolId, Object data, long executionTime) {
        return ToolResult.builder()
                .toolId(toolId)
                .success(true)
                .data(data)
                .executionTime(executionTime)
                .resultType("json")
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ToolResult error(String toolId, String errorCode, String errorMessage) {
        return ToolResult.builder()
                .toolId(toolId)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建失败结果（带异常）
     */
    public static ToolResult error(String toolId, String errorCode, String errorMessage, Throwable exception) {
        return ToolResult.builder()
                .toolId(toolId)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .exception(exception)
                .build();
    }

    /**
     * 创建超时结果
     */
    public static ToolResult timeout(String toolId, long timeout) {
        return ToolResult.builder()
                .toolId(toolId)
                .success(false)
                .errorCode("TIMEOUT")
                .errorMessage("工具执行超时，超时时间: " + timeout + "ms")
                .build();
    }

    /**
     * 判断是否需要重试
     */
    public boolean shouldRetry() {
        // 非成功且错误码为可重试类型
        return !success && isRetryableError(errorCode);
    }

    /**
     * 判断是否为可重试错误
     */
    private boolean isRetryableError(String errorCode) {
        if (errorCode == null) {
            return false;
        }
        return errorCode.equals("TIMEOUT") 
            || errorCode.equals("CONNECTION_ERROR")
            || errorCode.equals("SERVICE_UNAVAILABLE")
            || errorCode.equals("RATE_LIMITED");
    }
}
