package com.eazyai.ai.nexus.core.tool;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolExecutor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.eazyai.ai.nexus.api.tool.ToolVisibility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认工具总线实现
 * 统一工具注册、发现、调用的核心枢纽
 *
 * <h3>架构设计：</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │                     协议适配层                           │
 * │  McpProtocolAdapter / OpenAiProtocolAdapter / ...      │
 * └─────────────────────────┬───────────────────────────────┘
 *                           ↓
 * ┌─────────────────────────────────────────────────────────┐
 * │                   DefaultToolBus                        │
 * │  ┌─────────────────┐  ┌─────────────────────────────┐  │
 * │  │   工具注册表     │  │       执行器注册表           │  │
 * │  │  toolRegistry   │  │    executorRegistry         │  │
 * │  └─────────────────┘  └─────────────────────────────┘  │
 * │                                                        │
 * │  策略模式：根据 executorType 自动路由到对应执行器        │
 * └─────────────────────────┬───────────────────────────────┘
 *                           ↓
 * ┌─────────────────────────────────────────────────────────┐
 * │                    执行器层                             │
 * │  HttpToolExecutor / DbToolExecutor / FunctionExecutor  │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 */
@Slf4j
@Component
public class DefaultToolBus implements ToolBus {

    /**
     * 工具注册表：toolId -> ToolDescriptor
     */
    private final Map<String, ToolDescriptor> toolRegistry = new ConcurrentHashMap<>();

    /**
     * 执行器注册表：executorType -> ToolExecutor
     */
    private final Map<String, ToolExecutor> executorRegistry = new ConcurrentHashMap<>();

    /**
     * 工具使用历史服务
     */
    @Autowired(required = false)
    private ToolUsageHistoryService historyService;

    /**
     * 通过Spring自动注入所有ToolExecutor实现
     */
    @Autowired(required = false)
    public void setExecutors(List<ToolExecutor> executors) {
        if (executors != null) {
            for (ToolExecutor executor : executors) {
                registerExecutor(executor);
            }
            log.info("[DefaultToolBus] 自动注册 {} 个执行器: {}", 
                    executors.size(), executorRegistry.keySet());
        }
    }

    // ==================== 工具注册管理 ====================

    @Override
    public void registerTool(ToolDescriptor descriptor) {
        if (descriptor == null || descriptor.getToolId() == null) {
            log.warn("[DefaultToolBus] 注册工具失败：描述符或toolId为空");
            return;
        }
        
        toolRegistry.put(descriptor.getToolId(), descriptor);
        log.info("[DefaultToolBus] 注册工具: {} ({}) - 类型: {}, 协议: {}", 
                descriptor.getName(), descriptor.getToolId(), 
                descriptor.getExecutorType(), descriptor.getProtocol());
    }

    @Override
    public void unregisterTool(String toolId) {
        ToolDescriptor removed = toolRegistry.remove(toolId);
        if (removed != null) {
            log.info("[DefaultToolBus] 注销工具: {}", toolId);
        }
    }

    @Override
    public Optional<ToolDescriptor> getTool(String toolId) {
        return Optional.ofNullable(toolRegistry.get(toolId));
    }

    @Override
    public List<ToolDescriptor> getAllTools() {
        return new ArrayList<>(toolRegistry.values());
    }

    // ==================== 工具发现 ====================

    @Override
    public List<ToolDescriptor> findByCapability(String capability) {
        return toolRegistry.values().stream()
                .filter(t -> t.getCapabilities() != null && t.getCapabilities().contains(capability))
                .filter(ToolDescriptor::isEnabled)
                .toList();
    }

    /**
     * 按应用ID查找工具（已废弃，请使用findAccessibleTools）
     * 此方法仅返回appId精确匹配的工具
     */
    @Override
    public List<ToolDescriptor> findByAppId(String appId) {
        return toolRegistry.values().stream()
                .filter(t -> appId != null && appId.equals(t.getAppId()))
                .filter(ToolDescriptor::isEnabled)
                .toList();
    }

    /**
     * 查找应用可访问的所有工具
     * 根据工具可见性规则返回：
     * - PUBLIC: 所有应用可访问
     * - PRIVATE: 仅所属应用可访问
     * - SHARED: 仅授权应用列表中的应用可访问
     *
     * @param appId 应用ID（可为null，仅返回PUBLIC工具）
     * @return 可访问的工具列表
     */
    public List<ToolDescriptor> findAccessibleTools(String appId) {
        return toolRegistry.values().stream()
                .filter(ToolDescriptor::isEnabled)
                .filter(tool -> isAccessible(tool, appId))
                .toList();
    }

    /**
     * 判断工具对指定应用是否可访问
     */
    private boolean isAccessible(ToolDescriptor tool, String appId) {
        ToolVisibility visibility = tool.getVisibility();
        
        // 如果未设置可见性，根据appId判断：无appId为公共工具，有appId为应用专属
        if (visibility == null) {
            return tool.getAppId() == null || tool.getAppId().equals(appId);
        }
        
        return switch (visibility) {
            case PUBLIC -> true;
            case PRIVATE -> appId != null && appId.equals(tool.getAppId());
            case SHARED -> {
                if (appId == null) {
                    yield false;
                }
                List<String> authorizedApps = tool.getAuthorizedApps();
                yield authorizedApps != null && authorizedApps.contains(appId);
            }
        };
    }

    @Override
    public List<ToolDescriptor> findByExecutorType(String executorType) {
        return toolRegistry.values().stream()
                .filter(t -> executorType != null && executorType.equalsIgnoreCase(t.getExecutorType()))
                .filter(ToolDescriptor::isEnabled)
                .toList();
    }

    @Override
    public List<ToolDescriptor> findByProtocol(String protocol) {
        return toolRegistry.values().stream()
                .filter(t -> protocol != null && protocol.equalsIgnoreCase(t.getProtocol()))
                .filter(ToolDescriptor::isEnabled)
                .toList();
    }

    // ==================== 工具调用 ====================

    @Override
    public ToolResult invoke(String toolId, Map<String, Object> params, AgentContext context) {
        long startTime = System.currentTimeMillis();

        // 1. 查找工具
        ToolDescriptor descriptor = toolRegistry.get(toolId);
        if (descriptor == null) {
            ToolResult error = ToolResult.error(toolId, "TOOL_NOT_FOUND", "工具不存在: " + toolId);
            recordUsage(toolId, context, error, 0L, 0);
            return error;
        }

        // 2. 检查工具状态
        if (!descriptor.isEnabled()) {
            ToolResult error = ToolResult.error(toolId, "TOOL_DISABLED", "工具已禁用: " + toolId);
            recordUsage(toolId, context, error, System.currentTimeMillis() - startTime, 0);
            return error;
        }

        // 3. 参数校验
        String validationError = validateParams(descriptor, params);
        if (validationError != null) {
            ToolResult error = ToolResult.error(toolId, "INVALID_PARAMS", validationError);
            recordUsage(toolId, context, error, System.currentTimeMillis() - startTime, 0);
            return error;
        }

        // 4. 获取执行器
        ToolExecutor executor = executorRegistry.get(descriptor.getExecutorType().toLowerCase());
        if (executor == null) {
            ToolResult error = ToolResult.error(toolId, "EXECUTOR_NOT_FOUND", 
                    "未找到执行器: " + descriptor.getExecutorType());
            recordUsage(toolId, context, error, System.currentTimeMillis() - startTime, 0);
            return error;
        }

        // 5. 执行工具（带重试）
        int retryCount = 0;
        int maxRetries = Optional.ofNullable(descriptor.getRetryTimes()).orElse(0);
        Exception lastException = null;
        ToolResult result = null;

        while (retryCount <= maxRetries) {
            try {
                log.info("[DefaultToolBus] 执行工具: {} (类型: {}, 尝试: {}/{})", 
                        descriptor.getName(), descriptor.getExecutorType(), retryCount, maxRetries);
                
                result = executor.execute(descriptor, params, context);
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                result.setRetryCount(retryCount);
                
                if (result.isSuccess() || !result.shouldRetry()) {
                    recordUsage(toolId, context, result, result.getExecutionTime(), retryCount);
                    return result;
                }
                
                // 可重试错误
                lastException = new RuntimeException(result.getErrorMessage());
                
            } catch (Exception e) {
                lastException = e;
                log.warn("[DefaultToolBus] 工具执行异常: {} - {}", toolId, e.getMessage());
            }

            retryCount++;
            if (retryCount <= maxRetries) {
                try {
                    // 指数退避
                    Thread.sleep(1000L * (1L << (retryCount - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 执行失败
        result = ToolResult.builder()
                .toolId(toolId)
                .success(false)
                .errorCode("EXECUTION_ERROR")
                .errorMessage(lastException != null ? lastException.getMessage() : "执行失败")
                .exception(lastException)
                .executionTime(System.currentTimeMillis() - startTime)
                .retryCount(retryCount)
                .build();
        
        recordUsage(toolId, context, result, result.getExecutionTime(), retryCount);
        return result;
    }

    /**
     * 记录工具使用日志
     */
    private void recordUsage(String toolId, AgentContext context, ToolResult result, 
                             Long executionTime, Integer retryCount) {
        if (historyService != null) {
            historyService.recordUsage(
                    toolId,
                    context != null ? context.getAppId() : null,
                    context != null ? context.getSessionId() : null,
                    context != null ? context.getUserId() : null,
                    context != null ? context.getRequestId() : null,
                    result.isSuccess(),
                    result.getErrorCode(),
                    result.getErrorMessage(),
                    executionTime,
                    retryCount
            );
        }
    }

    @Override
    public List<ToolResult> invokeBatch(List<ToolCall> calls, AgentContext context) {
        return calls.stream()
                .map(call -> invoke(call.toolId(), call.params(), context))
                .toList();
    }

    // ==================== 执行器管理 ====================

    @Override
    public void registerExecutor(ToolExecutor executor) {
        String type = executor.getExecutorType().toLowerCase();
        executorRegistry.put(type, executor);
        log.info("[DefaultToolBus] 注册执行器: {} -> {}", type, executor.getClass().getSimpleName());
    }

    @Override
    public void unregisterExecutor(String executorType) {
        ToolExecutor removed = executorRegistry.remove(executorType.toLowerCase());
        if (removed != null) {
            log.info("[DefaultToolBus] 注销执行器: {}", executorType);
        }
    }

    @Override
    public List<String> getRegisteredExecutorTypes() {
        return new ArrayList<>(executorRegistry.keySet());
    }

    // ==================== 私有方法 ====================

    /**
     * 参数校验
     */
    private String validateParams(ToolDescriptor descriptor, Map<String, Object> params) {
        if (descriptor.getParameters() == null) {
            return null;
        }

        for (ToolDescriptor.ParamDefinition param : descriptor.getParameters()) {
            if (Boolean.TRUE.equals(param.getRequired())) {
                if (params == null || !params.containsKey(param.getName()) || 
                    params.get(param.getName()) == null) {
                    return "参数[" + param.getName() + "]为必填项";
                }
            }
        }
        
        return null;
    }
}
