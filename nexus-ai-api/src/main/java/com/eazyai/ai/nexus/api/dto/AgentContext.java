package com.eazyai.ai.nexus.api.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 智能体上下文
 * 贯穿整个智能体执行流程的上下文对象
 */
@Data
@Builder
public class AgentContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求唯一标识
     */
    @Builder.Default
    private String requestId = UUID.randomUUID().toString();

    /**
     * 会话ID（用于多轮对话）
     */
    private String sessionId;

    /**
     * 用户标识
     */
    private String userId;

    /**
     * 原始用户输入
     */
    private String userInput;

    /**
     * 当前执行步骤
     */
    @Builder.Default
    private ExecutionStage currentStage = ExecutionStage.INIT;

    /**
     * 执行步骤记录
     */
    @Builder.Default
    private Map<String, Object> executionTrace = new HashMap<>();

    /**
     * 业务参数
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * 中间结果存储
     */
    @Builder.Default
    private Map<String, Object> intermediateResults = new HashMap<>();

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 开始时间
     */
    @Builder.Default
    private long startTime = System.currentTimeMillis();

    /**
     * 执行阶段枚举
     */
    public enum ExecutionStage {
        INIT,              // 初始化
        INTENT_ANALYSIS,   // 意图分析
        PLANNING,          // 规划
        SCHEDULING,        // 调度
        EXECUTING,         // 执行
        INTEGRATING,       // 结果整合
        MEMORY_UPDATE,     // 记忆更新
        COMPLETED,         // 完成
        FAILED             // 失败
    }

    /**
     * 添加上下文属性
     */
    public AgentContext addAttribute(String key, Object value) {
        this.attributes.put(key, value);
        return this;
    }

    /**
     * 获取上下文属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) this.attributes.get(key);
    }

    /**
     * 记录执行步骤
     */
    public void recordStep(String stepName, Object stepData) {
        this.executionTrace.put(stepName, stepData);
    }

    /**
     * 获取执行耗时
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}
