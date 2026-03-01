package com.eazyai.ai.nexus.api.exception;

/**
 * 智能体异常基类
 */
public class AgentException extends RuntimeException {

    private final ErrorCode errorCode;

    public AgentException(String message) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
    }

    public AgentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AgentException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        UNKNOWN_ERROR("AGENT-0000", "未知错误"),
        INVALID_REQUEST("AGENT-0001", "无效请求"),
        INTENT_ANALYSIS_FAILED("AGENT-0002", "意图分析失败"),
        PLANNING_FAILED("AGENT-0003", "任务规划失败"),
        SCHEDULING_FAILED("AGENT-0004", "任务调度失败"),
        EXECUTION_FAILED("AGENT-0005", "执行失败"),
        INTEGRATION_FAILED("AGENT-0006", "结果整合失败"),
        MEMORY_ERROR("AGENT-0007", "记忆操作失败"),
        PLUGIN_NOT_FOUND("AGENT-0008", "插件未找到"),
        PLUGIN_EXECUTION_ERROR("AGENT-0009", "插件执行错误"),
        TIMEOUT("AGENT-0010", "执行超时"),
        CIRCUIT_BREAKER_OPEN("AGENT-0011", "熔断器开启"),
        RATE_LIMIT_EXCEEDED("AGENT-0012", "超出限流");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
