package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eazyai.ai.nexus.core.tool.ToolUsageLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工具使用日志表
 * 记录工具调用历史，用于分析工具使用成功率和辅助LLM工具选择
 */
@Getter
@Setter
@TableName("ai_tool_usage_log")
public class AiToolUsageLog implements ToolUsageLog {

    @TableId("id")
    private Long id;

    /**
     * 工具ID
     */
    private String toolId;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 调用是否成功: 1成功/0失败
     */
    private Integer success;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 执行耗时（ms）
     */
    private Long executionTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 用户满意度评分（1-5）
     */
    private Integer rating;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
