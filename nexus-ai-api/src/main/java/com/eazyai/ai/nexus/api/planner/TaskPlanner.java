package com.eazyai.ai.nexus.api.planner;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.intent.IntentResult;

import java.util.List;

/**
 * 任务规划器接口
 * 负责将意图转换为可执行的任务计划
 */
public interface TaskPlanner {

    /**
     * 创建任务计划
     *
     * @param intent 意图分析结果
     * @param context 执行上下文
     * @return 任务计划
     */
    TaskPlan createPlan(IntentResult intent, AgentContext context);

    /**
     * 获取规划器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 是否支持该意图类型
     */
    boolean supports(String intentType);
}
