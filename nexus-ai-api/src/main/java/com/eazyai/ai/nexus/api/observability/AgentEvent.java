package com.eazyai.ai.nexus.api.observability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能体事件
 * 用于监控和追踪
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 事件类型
     */
    private EventType type;

    /**
     * 事件阶段
     */
    private String stage;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 执行耗时（毫秒）
     */
    private long duration;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 事件数据
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    /**
     * 事件类型枚举
     */
    public enum EventType {
        REQUEST_START,          // 请求开始
        REQUEST_END,            // 请求结束
        INTENT_ANALYSIS,        // 意图分析
        PLANNING,               // 任务规划
        SCHEDULING,             // 任务调度
        PLUGIN_EXECUTE,         // 插件执行
        INTEGRATION,            // 结果整合
        MEMORY_ACCESS,          // 记忆访问
        ERROR                   // 错误
    }

    /**
     * 创建事件构建器
     */
    public static AgentEventBuilder create(String requestId, EventType type) {
        return AgentEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .requestId(requestId)
                .type(type)
                .timestamp(System.currentTimeMillis())
                .success(true);
    }
}
