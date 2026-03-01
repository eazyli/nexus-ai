package com.eazyai.ai.nexus.api.integrator;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.api.scheduler.ScheduleResult;

/**
 * 结果整合器接口
 * 负责整合各步骤执行结果，生成最终响应
 */
public interface ResultIntegrator {

    /**
     * 整合执行结果
     *
     * @param scheduleResult 调度执行结果
     * @param context 执行上下文
     * @return 最终响应
     */
    AgentResponse integrate(ScheduleResult scheduleResult, AgentContext context);

    /**
     * 格式化输出
     *
     * @param data 原始数据
     * @param format 目标格式
     * @return 格式化后的数据
     */
    Object formatOutput(Object data, String format);

    /**
     * 获取整合器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
