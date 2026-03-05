package com.eazyai.ai.nexus.core.tool.config;

import com.eazyai.ai.nexus.core.tool.DefaultToolBus;
import com.eazyai.ai.nexus.core.tool.executor.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 工具执行器配置类
 * 
 * <p>显式注册原子执行器到ToolBus，避免循环依赖。</p>
 *
 * <h3>设计说明：</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │          ToolExecutorConfiguration（配置类）             │
 * │                                                        │
 * │  原子执行器：                                           │
 * │  - HttpToolExecutor   → DefaultToolBus                 │
 * │  - DbToolExecutor     → DefaultToolBus                 │
 * │  - FunctionToolExecutor → DefaultToolBus               │
 * │  - McpToolExecutor    → DefaultToolBus                 │
 * │                                                        │
 * │  编排执行器（不注册到ToolBus）：                          │
 * │  - FlowExecutor → 通过 ToolOrchestrator 编排           │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>为什么不在DefaultToolBus中自动注入所有ToolExecutor？</h3>
 * <ul>
 *   <li>FlowExecutor需要调用ToolBus来执行子工具</li>
 *   <li>如果ToolBus持有FlowExecutor，则形成循环依赖</li>
 *   <li>通过分离原子执行器和编排执行器，打破循环</li>
 * </ul>
 */
@Slf4j
@Configuration
public class ToolExecutorConfiguration {

    @Autowired
    private DefaultToolBus toolBus;

    @Autowired(required = false)
    private HttpToolExecutor httpToolExecutor;

    @Autowired(required = false)
    private DbToolExecutor dbToolExecutor;

    @Autowired(required = false)
    private FunctionToolExecutor functionToolExecutor;

    @Autowired(required = false)
    private McpToolExecutor mcpToolExecutor;

    /**
     * 注册原子执行器
     * FlowExecutor不在此注册，由ToolOrchestrator协调
     */
    @PostConstruct
    public void registerAtomicExecutors() {
        log.info("[ToolExecutorConfiguration] 开始注册原子执行器...");
        
        int count = 0;
        
        if (httpToolExecutor != null) {
            toolBus.registerAtomicExecutor(httpToolExecutor);
            count++;
            log.debug("[ToolExecutorConfiguration] 注册 HttpToolExecutor");
        }
        
        if (dbToolExecutor != null) {
            toolBus.registerAtomicExecutor(dbToolExecutor);
            count++;
            log.debug("[ToolExecutorConfiguration] 注册 DbToolExecutor");
        }
        
        if (functionToolExecutor != null) {
            toolBus.registerAtomicExecutor(functionToolExecutor);
            count++;
            log.debug("[ToolExecutorConfiguration] 注册 FunctionToolExecutor");
        }
        
        if (mcpToolExecutor != null) {
            toolBus.registerAtomicExecutor(mcpToolExecutor);
            count++;
            log.debug("[ToolExecutorConfiguration] 注册 McpToolExecutor");
        }
        
        log.info("[ToolExecutorConfiguration] 注册完成，共 {} 个原子执行器", count);
    }
}
