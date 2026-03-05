package com.eazyai.ai.nexus.core.tool;

import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.core.tool.executor.McpToolExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP协议适配器
 * 负责发现MCP Server上的工具并自动注册到工具总线
 *
 * <h3>架构位置：</h3>
 * <pre>
 * MCP Server
 *     ↓ (发现工具)
 * McpProtocolAdapter ← 协议适配层
 *     ↓ (转换为ToolDescriptor)
 * ToolBus (统一工具总线)
 *     ↓
 * McpToolExecutor (执行MCP工具调用)
 * </pre>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>自动发现MCP Server上的工具</li>
 *   <li>将MCP工具格式转换为统一描述符</li>
 *   <li>支持批量注册和注销</li>
 *   <li>支持工具同步更新</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpProtocolAdapter {

    private final ToolBus toolBus;
    private final McpToolExecutor mcpToolExecutor;

    /**
     * 已注册的MCP服务器工具映射
     * key: serverUrl, value: toolId列表
     */
    private final Map<String, List<String>> registeredTools = new HashMap<>();

    /**
     * 发现并注册MCP Server上的所有工具
     *
     * @param serverUrl MCP服务器地址
     * @param appId     应用ID
     * @param config    连接配置（headers等）
     * @return 注册的工具ID列表
     */
    public List<String> discoverAndRegister(String serverUrl, String appId, Map<String, Object> config) {
        log.info("[McpProtocolAdapter] 发现MCP工具: serverUrl={}, appId={}", serverUrl, appId);

        try {
            // 发现工具
            JsonNode tools = mcpToolExecutor.discoverTools(serverUrl, config);
            if (tools == null || !tools.isArray()) {
                log.warn("[McpProtocolAdapter] 未发现任何MCP工具: {}", serverUrl);
                return List.of();
            }

            List<String> registeredToolIds = new ArrayList<>();

            // 注册每个工具
            for (JsonNode tool : tools) {
                try {
                    ToolDescriptor descriptor = mcpToolExecutor.convertToDescriptor(tool, serverUrl, appId, config);
                    
                    // 设置可见性
                    descriptor.setVisibility(com.eazyai.ai.nexus.api.tool.ToolVisibility.PRIVATE);
                    descriptor.setEnabled(true);

                    // 注册到工具总线
                    toolBus.registerTool(descriptor);
                    registeredToolIds.add(descriptor.getToolId());

                    log.info("[McpProtocolAdapter] 注册MCP工具: {} ({})", 
                            descriptor.getName(), descriptor.getToolId());

                } catch (Exception e) {
                    log.error("[McpProtocolAdapter] 注册MCP工具失败: {}", tool.get("name"), e);
                }
            }

            // 记录已注册的工具
            registeredTools.put(serverUrl, registeredToolIds);

            return registeredToolIds;

        } catch (Exception e) {
            log.error("[McpProtocolAdapter] 发现MCP工具失败: {}", serverUrl, e);
            return List.of();
        }
    }

    /**
     * 注销MCP Server上的所有工具
     *
     * @param serverUrl MCP服务器地址
     */
    public void unregister(String serverUrl) {
        List<String> toolIds = registeredTools.remove(serverUrl);
        if (toolIds != null) {
            for (String toolId : toolIds) {
                toolBus.unregisterTool(toolId);
                log.info("[McpProtocolAdapter] 注销MCP工具: {}", toolId);
            }
        }
        mcpToolExecutor.clearSession(serverUrl);
    }

    /**
     * 刷新MCP Server的工具（先注销再重新发现注册）
     *
     * @param serverUrl MCP服务器地址
     * @param appId     应用ID
     * @param config    连接配置
     * @return 注册的工具ID列表
     */
    public List<String> refresh(String serverUrl, String appId, Map<String, Object> config) {
        log.info("[McpProtocolAdapter] 刷新MCP工具: {}", serverUrl);
        unregister(serverUrl);
        return discoverAndRegister(serverUrl, appId, config);
    }

    /**
     * 获取已注册的MCP服务器列表
     */
    public List<String> getRegisteredServers() {
        return new ArrayList<>(registeredTools.keySet());
    }

    /**
     * 获取指定服务器的已注册工具数量
     */
    public int getToolCount(String serverUrl) {
        List<String> tools = registeredTools.get(serverUrl);
        return tools != null ? tools.size() : 0;
    }

    /**
     * 获取所有已注册工具总数
     */
    public int getTotalToolCount() {
        return registeredTools.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
