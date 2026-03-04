package com.eazyai.ai.nexus.core.tool;

import java.time.LocalDateTime;

/**
 * 简单的工具使用日志实现
 */
public class SimpleToolUsageLog implements ToolUsageLog {

    private String toolId;
    private String appId;
    private String sessionId;
    private String userId;
    private String requestId;
    private Integer success;
    private String errorCode;
    private String errorMsg;
    private Long executionTime;
    private Integer retryCount;
    private LocalDateTime createTime;

    @Override
    public String getToolId() { return toolId; }
    @Override
    public void setToolId(String toolId) { this.toolId = toolId; }

    @Override
    public String getAppId() { return appId; }
    @Override
    public void setAppId(String appId) { this.appId = appId; }

    @Override
    public String getSessionId() { return sessionId; }
    @Override
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    @Override
    public String getUserId() { return userId; }
    @Override
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String getRequestId() { return requestId; }
    @Override
    public void setRequestId(String requestId) { this.requestId = requestId; }

    @Override
    public Integer getSuccess() { return success; }
    @Override
    public void setSuccess(Integer success) { this.success = success; }

    @Override
    public String getErrorCode() { return errorCode; }
    @Override
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    @Override
    public String getErrorMsg() { return errorMsg; }
    @Override
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    @Override
    public Long getExecutionTime() { return executionTime; }
    @Override
    public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }

    @Override
    public Integer getRetryCount() { return retryCount; }
    @Override
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    @Override
    public LocalDateTime getCreateTime() { return createTime; }
    @Override
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
