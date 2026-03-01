package com.eazyai.ai.nexus.core.mcp;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.core.mcp.executor.DbToolExecutor;
import com.eazyai.ai.nexus.core.mcp.executor.HttpToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认MCP工具总线实现
 */
@Slf4j
@Component
public class DefaultMcpToolBus implements McpToolBus {

    private final Map<String, McpToolDescriptor> toolRegistry = new ConcurrentHashMap<>();
    private final Map<String, McpToolExecutor> executorRegistry = new ConcurrentHashMap<>();
    
    private final HttpToolExecutor httpToolExecutor;
    private final DbToolExecutor dbToolExecutor;
    
    @Autowired
    public DefaultMcpToolBus(HttpToolExecutor httpToolExecutor, DbToolExecutor dbToolExecutor) {
        this.httpToolExecutor = httpToolExecutor;
        this.dbToolExecutor = dbToolExecutor;
    }

    @Override
    public void registerTool(McpToolDescriptor descriptor) {
        toolRegistry.put(descriptor.getToolId(), descriptor);
        log.info("注册MCP工具: {} - {} (类型: {})", 
                descriptor.getToolId(), descriptor.getName(), descriptor.getType());
    }

    @Override
    public void unregisterTool(String toolId) {
        McpToolDescriptor removed = toolRegistry.remove(toolId);
        if (removed != null) {
            log.info("注销MCP工具: {}", toolId);
        }
    }

    @Override
    public Optional<McpToolDescriptor> getTool(String toolId) {
        return Optional.ofNullable(toolRegistry.get(toolId));
    }

    @Override
    public List<McpToolDescriptor> getAllTools() {
        return new ArrayList<>(toolRegistry.values());
    }

    @Override
    public List<McpToolDescriptor> findByCapability(String capability) {
        return toolRegistry.values().stream()
                .filter(t -> t.getCapabilities() != null && t.getCapabilities().contains(capability))
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .toList();
    }

    @Override
    public List<McpToolDescriptor> findByAppId(String appId) {
        return toolRegistry.values().stream()
                .filter(t -> appId != null && appId.equals(t.getAppId()))
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .toList();
    }

    @Override
    public McpToolResult invoke(String toolId, Map<String, Object> params, AgentContext context) {
        long startTime = System.currentTimeMillis();
        
        McpToolDescriptor descriptor = toolRegistry.get(toolId);
        if (descriptor == null) {
            return McpToolResult.error(toolId, "TOOL_NOT_FOUND", "工具不存在: " + toolId);
        }

        if (!Boolean.TRUE.equals(descriptor.getEnabled())) {
            return McpToolResult.error(toolId, "TOOL_DISABLED", "工具已禁用: " + toolId);
        }

        // 参数校验
        String validationError = validateParams(descriptor, params);
        if (validationError != null) {
            return McpToolResult.error(toolId, "INVALID_PARAMS", validationError);
        }

        // 执行工具
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= Optional.ofNullable(descriptor.getRetryTimes()).orElse(0)) {
            try {
                McpToolResult result = executeTool(descriptor, params, context);
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                result.setRetryCount(retryCount);
                return result;
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                log.warn("工具执行失败 (第{}次): {}", retryCount, toolId);
                
                if (retryCount <= Optional.ofNullable(descriptor.getRetryTimes()).orElse(0)) {
                    try {
                        Thread.sleep(1000L * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return McpToolResult.builder()
                .toolId(toolId)
                .success(false)
                .errorMessage(lastException != null ? lastException.getMessage() : "执行失败")
                .errorCode("EXECUTION_ERROR")
                .executionTime(System.currentTimeMillis() - startTime)
                .retryCount(retryCount)
                .build();
    }

    @Override
    public List<McpToolResult> invokeBatch(List<ToolCall> calls, AgentContext context) {
        return calls.stream()
                .map(call -> invoke(call.toolId(), call.params(), context))
                .toList();
    }

    /**
     * 执行工具
     */
    private McpToolResult executeTool(McpToolDescriptor descriptor, 
                                      Map<String, Object> params, 
                                      AgentContext context) {
        String type = descriptor.getType();
        if (type == null) {
            return McpToolResult.error(descriptor.getToolId(), "TYPE_MISSING", "工具类型未指定");
        }
        
        return switch (type.toLowerCase()) {
            case "http" -> executeHttpTool(descriptor, params);
            case "function" -> executeFunctionTool(descriptor, params, context);
            case "db" -> executeDbTool(descriptor, params);
            default -> McpToolResult.error(descriptor.getToolId(), 
                    "UNSUPPORTED_TYPE", "不支持的工具类型: " + descriptor.getType());
        };
    }

    /**
     * 执行HTTP工具
     */
    private McpToolResult executeHttpTool(McpToolDescriptor descriptor, Map<String, Object> params) {
        log.info("执行HTTP工具: {} - params: {}", descriptor.getName(), params);
        return httpToolExecutor.execute(descriptor, params);
    }

    /**
     * 执行函数工具
     */
    private McpToolResult executeFunctionTool(McpToolDescriptor descriptor, 
                                              Map<String, Object> params, 
                                              AgentContext context) {
        // TODO: 通过反射调用函数
        log.info("执行函数工具: {} - params: {}", descriptor.getName(), params);
        return McpToolResult.success(descriptor.getToolId(), params);
    }

    /**
     * 执行数据库工具
     */
    private McpToolResult executeDbTool(McpToolDescriptor descriptor, Map<String, Object> params) {
        log.info("执行数据库工具: {} - params: {}", descriptor.getName(), params);
        return dbToolExecutor.execute(descriptor, params);
    }

    /**
     * 参数校验
     */
    private String validateParams(McpToolDescriptor descriptor, Map<String, Object> params) {
        if (descriptor.getParameters() == null) {
            return null;
        }

        for (McpToolDescriptor.ParamDefinition param : descriptor.getParameters()) {
            if (Boolean.TRUE.equals(param.getRequired())) {
                if (params == null || !params.containsKey(param.getName()) || 
                    params.get(param.getName()) == null) {
                    return "参数[" + param.getName() + "]为必填项";
                }
            }
        }
        
        return null;
    }

    /**
     * 注册执行器
     */
    public void registerExecutor(String toolType, McpToolExecutor executor) {
        executorRegistry.put(toolType, executor);
    }

    /**
     * 工具执行器接口
     */
    @FunctionalInterface
    public interface McpToolExecutor {
        McpToolResult execute(McpToolDescriptor descriptor, Map<String, Object> params);
    }
}
