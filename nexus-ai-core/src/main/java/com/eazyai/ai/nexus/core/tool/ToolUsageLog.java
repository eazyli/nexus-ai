package com.eazyai.ai.nexus.core.tool;

import java.time.LocalDateTime;

/**
 * 工具使用日志 - 核心层接口
 */
public interface ToolUsageLog {

    String getToolId();
    String getAppId();
    String getSessionId();
    String getUserId();
    String getRequestId();
    Integer getSuccess();
    String getErrorCode();
    String getErrorMsg();
    Long getExecutionTime();
    Integer getRetryCount();
    LocalDateTime getCreateTime();

    void setToolId(String toolId);
    void setAppId(String appId);
    void setSessionId(String sessionId);
    void setUserId(String userId);
    void setRequestId(String requestId);
    void setSuccess(Integer success);
    void setErrorCode(String errorCode);
    void setErrorMsg(String errorMsg);
    void setExecutionTime(Long executionTime);
    void setRetryCount(Integer retryCount);
    void setCreateTime(LocalDateTime createTime);
}
