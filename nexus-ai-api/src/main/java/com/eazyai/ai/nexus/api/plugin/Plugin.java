package com.eazyai.ai.nexus.api.plugin;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.executor.ExecutionResult;

import java.util.Map;

/**
 * 插件接口
 * 所有插件必须实现此接口
 */
public interface Plugin {

    /**
     * 执行插件
     *
     * @param params 执行参数
     * @param context 执行上下文
     * @return 执行结果
     */
    ExecutionResult execute(Map<String, Object> params, AgentContext context);

    /**
     * 获取插件描述符
     */
    PluginDescriptor getDescriptor();

    /**
     * 初始化插件
     *
     * @param config 配置参数
     */
    default void initialize(Map<String, Object> config) {
        // 默认空实现
    }

    /**
     * 销毁插件
     */
    default void destroy() {
        // 默认空实现
    }

    /**
     * 健康检查
     *
     * @return 是否健康
     */
    default boolean healthCheck() {
        return true;
    }

    /**
     * 是否支持该参数配置
     *
     * @param params 参数
     * @return 是否支持
     */
    default boolean supports(Map<String, Object> params) {
        return true;
    }
}
