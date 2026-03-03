package com.eazyai.ai.nexus.api.tool;

import com.eazyai.ai.nexus.api.dto.AgentContext;

import java.util.Map;

/**
 * 工具执行器接口
 * 统一的工具执行抽象，支持多协议适配
 *
 * <p>执行器是真正执行工具调用的底层组件，负责与外部系统交互并返回结果。</p>
 * <p>不同类型的工具（HTTP、DB、Function等）通过实现此接口提供统一的执行入口。</p>
 *
 * <h3>架构位置：</h3>
 * <pre>
 * 协议接入层(MCP/OpenAI/LangChain)
 *        ↓
 * 协议适配层(ProtocolAdapter)
 *        ↓
 * 统一工具总线(ToolBus)
 *        ↓
 * 执行器层(ToolExecutor) ← 当前接口
 *        ↓
 * 外部系统(HTTP API/数据库/函数等)
 * </pre>
 *
 * @see ToolDescriptor 工具描述符
 * @see ToolResult 执行结果
 * @see ToolBus 工具总线
 */
public interface ToolExecutor {

    /**
     * 执行工具
     *
     * @param descriptor 工具描述符，包含工具元数据和配置
     * @param params     输入参数
     * @param context    智能体上下文
     * @return 执行结果
     */
    ToolResult execute(ToolDescriptor descriptor, Map<String, Object> params, AgentContext context);

    /**
     * 获取支持的执行器类型
     * 用于工具总线自动路由
     *
     * @return 执行器类型标识，如: http, db, function, grpc, dubbo等
     */
    String getExecutorType();

    /**
     * 判断是否支持执行指定工具
     *
     * @param descriptor 工具描述符
     * @return 是否支持
     */
    default boolean supports(ToolDescriptor descriptor) {
        return getExecutorType().equalsIgnoreCase(descriptor.getExecutorType());
    }

    /**
     * 健康检查
     * 检查执行器是否处于可用状态
     *
     * @return 是否健康
     */
    default boolean healthCheck() {
        return true;
    }

    /**
     * 初始化执行器
     *
     * @param config 配置参数
     */
    default void initialize(Map<String, Object> config) {
        // 默认空实现
    }

    /**
     * 销毁执行器，释放资源
     */
    default void destroy() {
        // 默认空实现
    }
}
