package com.eazyai.ai.nexus.api.intent;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentRequest;

/**
 * 意图分析器接口
 * 负责理解用户输入的意图
 */
public interface IntentAnalyzer {

    /**
     * 分析用户意图
     *
     * @param request 原始请求
     * @param context 执行上下文
     * @return 意图分析结果
     */
    IntentResult analyze(AgentRequest request, AgentContext context);

    /**
     * 获取分析器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取分析器优先级（数值越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否支持该类型的请求
     */
    default boolean supports(AgentRequest request) {
        return true;
    }
}
