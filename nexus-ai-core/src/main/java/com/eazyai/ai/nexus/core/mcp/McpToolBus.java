package com.eazyai.ai.nexus.core.mcp;

import com.eazyai.ai.nexus.api.dto.AgentContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP工具总线接口
 * 
 * <p>MCP (Model Context Protocol) 工具调用总线</p>
 * <ul>
 *   <li>统一工具注册与发现</li>
 *   <li>标准化工具调用协议</li>
 *   <li>支持HTTP/DB/Dubbo/第三方API等所有工具类型</li>
 * </ul>
 */
public interface McpToolBus {

    /**
     * 注册工具
     *
     * @param descriptor 工具描述符
     */
    void registerTool(McpToolDescriptor descriptor);

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
    Optional<McpToolDescriptor> getTool(String toolId);

    /**
     * 获取所有工具
     *
     * @return 工具列表
     */
    List<McpToolDescriptor> getAllTools();

    /**
     * 根据能力查找工具
     *
     * @param capability 能力名称
     * @return 工具列表
     */
    List<McpToolDescriptor> findByCapability(String capability);

    /**
     * 根据应用ID查找工具
     *
     * @param appId 应用ID
     * @return 工具列表
     */
    List<McpToolDescriptor> findByAppId(String appId);

    /**
     * 调用工具
     *
     * @param toolId  工具ID
     * @param params  参数
     * @param context 上下文
     * @return 调用结果
     */
    McpToolResult invoke(String toolId, Map<String, Object> params, AgentContext context);

    /**
     * 批量调用工具
     *
     * @param calls   调用列表
     * @param context 上下文
     * @return 调用结果列表
     */
    List<McpToolResult> invokeBatch(List<ToolCall> calls, AgentContext context);

    /**
     * 工具调用请求
     */
    record ToolCall(
        String toolId,
        Map<String, Object> params
    ) {}
}
