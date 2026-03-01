package com.eazyai.ai.nexus.core;

import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;

/**
 * 智能体基础接口
 * 
 * <p>说明：</p>
 * <ul>
 *   <li>提供统一的智能体抽象</li>
 *   <li>支持多种智能体模式的扩展</li>
 *   <li>与 LangChain4j AiServices 无缝集成</li>
 * </ul>
 */
public interface Agent {

    /**
     * 获取智能体名称
     */
    String getName();

    /**
     * 获取智能体描述
     */
    String getDescription();

    /**
     * 执行智能体任务
     *
     * @param request 请求参数
     * @return 响应结果
     */
    AgentResponse execute(AgentRequest request);

    /**
     * 是否支持此类型任务
     *
     * @param taskType 任务类型
     * @return true/false
     */
    default boolean supports(String taskType) {
        return true;
    }
}
