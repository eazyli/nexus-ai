package com.eazyai.ai.nexus.api.tool;

import com.eazyai.ai.nexus.api.dto.AgentContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 统一工具总线接口
 * 工具注册、发现、调用的统一入口
 *
 * <p>工具总线是整个工具系统的核心枢纽，负责：</p>
 * <ul>
 *   <li>工具注册管理：维护工具注册表</li>
 *   <li>执行器路由：根据工具类型路由到对应执行器</li>
 *   <li>工具调用：统一调用入口，支持同步/批量</li>
 *   <li>能力发现：按能力标签查找工具</li>
 * </ul>
 *
 * <h3>架构位置：</h3>
 * <pre>
 * 协议适配层
 *      ↓
 * ToolBus（当前接口）← 统一调用入口
 *      ↓
 * ToolExecutor（执行器）
 *      ↓
 * 外部系统
 * </pre>
 *
 * @see ToolExecutor 工具执行器
 * @see ToolDescriptor 工具描述符
 * @see ToolResult 执行结果
 */
public interface ToolBus {

    // ==================== 工具注册管理 ====================

    /**
     * 注册工具
     *
     * @param descriptor 工具描述符
     */
    void registerTool(ToolDescriptor descriptor);

    /**
     * 批量注册工具
     *
     * @param descriptors 工具描述符列表
     */
    default void registerTools(List<ToolDescriptor> descriptors) {
        if (descriptors != null) {
            descriptors.forEach(this::registerTool);
        }
    }

    /**
     * 注销工具
     *
     * @param toolId 工具ID
     */
    void unregisterTool(String toolId);

    /**
     * 获取工具描述符
     *
     * @param toolId 工具ID
     * @return 工具描述符
     */
    Optional<ToolDescriptor> getTool(String toolId);

    /**
     * 获取所有已注册的工具
     *
     * @return 工具列表
     */
    List<ToolDescriptor> getAllTools();

    /**
     * 检查工具是否存在
     *
     * @param toolId 工具ID
     * @return 是否存在
     */
    default boolean hasTool(String toolId) {
        return getTool(toolId).isPresent();
    }

    // ==================== 工具发现 ====================

    /**
     * 按能力标签查找工具
     *
     * @param capability 能力标签
     * @return 匹配的工具列表
     */
    List<ToolDescriptor> findByCapability(String capability);

    /**
     * 按应用ID查找工具
     *
     * @param appId 应用ID
     * @return 匹配的工具列表
     */
    List<ToolDescriptor> findByAppId(String appId);

    /**
     * 按执行器类型查找工具
     *
     * @param executorType 执行器类型
     * @return 匹配的工具列表
     */
    List<ToolDescriptor> findByExecutorType(String executorType);

    /**
     * 按协议来源查找工具
     *
     * @param protocol 协议类型
     * @return 匹配的工具列表
     */
    List<ToolDescriptor> findByProtocol(String protocol);

    // ==================== 工具调用 ====================

    /**
     * 调用工具
     *
     * @param toolId  工具ID
     * @param params  输入参数
     * @param context 智能体上下文
     * @return 执行结果
     */
    ToolResult invoke(String toolId, Map<String, Object> params, AgentContext context);

    /**
     * 批量调用工具
     *
     * @param calls   工具调用列表
     * @param context 智能体上下文
     * @return 执行结果列表
     */
    List<ToolResult> invokeBatch(List<ToolCall> calls, AgentContext context);

    /**
     * 异步调用工具
     *
     * @param toolId  工具ID
     * @param params  输入参数
     * @param context 智能体上下文
     * @return 执行结果Future
     */
    default java.util.concurrent.CompletableFuture<ToolResult> invokeAsync(
            String toolId, Map<String, Object> params, AgentContext context) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> invoke(toolId, params, context));
    }

    // ==================== 执行器管理 ====================

    /**
     * 注册执行器
     *
     * @param executor 执行器实例
     */
    void registerExecutor(ToolExecutor executor);

    /**
     * 注销执行器
     *
     * @param executorType 执行器类型
     */
    void unregisterExecutor(String executorType);

    /**
     * 获取已注册的执行器类型
     *
     * @return 执行器类型列表
     */
    List<String> getRegisteredExecutorTypes();

    // ==================== 内部类型定义 ====================

    /**
     * 工具调用请求
     */
    record ToolCall(String toolId, Map<String, Object> params) {
        public static ToolCall of(String toolId, Map<String, Object> params) {
            return new ToolCall(toolId, params);
        }
    }
}
