package com.eazyai.ai.nexus.core.executor;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.executor.ExecutionResult;
import com.eazyai.ai.nexus.api.executor.PluginExecutor;
import com.eazyai.ai.nexus.api.plugin.Plugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 默认插件执行器
 * 提供同步和异步执行能力，支持超时控制
 */
@Slf4j
@Component
public class DefaultPluginExecutor implements PluginExecutor {

    @Override
    public ExecutionResult execute(Plugin plugin, Map<String, Object> params, AgentContext context) {
        long startTime = System.currentTimeMillis();
        long timeout = context.getAttribute("timeout") != null ?
                (Long) context.getAttribute("timeout") : 30000;

        try {
            // 检查插件是否支持该参数
            if (!plugin.supports(params)) {
                return ExecutionResult.error("Plugin does not support the provided parameters", null);
            }

            // 设置超时执行
            ExecutionResult result = CompletableFuture
                    .supplyAsync(() -> plugin.execute(params, context))
                    .orTimeout(timeout, TimeUnit.MILLISECONDS)
                    .join();

            result.setExecutionTime(System.currentTimeMillis() - startTime);
            return result;

        } catch (Exception e) {
            log.error("Plugin execution failed: {}", plugin.getDescriptor().getName(), e);
            return ExecutionResult.error(e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<ExecutionResult> executeAsync(Plugin plugin, Map<String, Object> params, AgentContext context) {
        return CompletableFuture.supplyAsync(() -> execute(plugin, params, context));
    }
}
