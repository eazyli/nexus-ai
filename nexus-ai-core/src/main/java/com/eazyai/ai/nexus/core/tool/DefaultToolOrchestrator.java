package com.eazyai.ai.nexus.core.tool;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolOrchestrator;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 默认工具编排器实现
 * 
 * <p>负责编排执行复杂工具流程，协调子工具的执行。</p>
 *
 * <h3>架构职责：</h3>
 * <pre>
 * ToolBus（工具注册与查找）
 *        ↓ 持有引用
 * DefaultToolOrchestrator（编排执行）
 *        ↓ 调用
 * ToolBus.invoke（原子工具执行）
 * </pre>
 *
 * <h3>设计说明：</h3>
 * <ul>
 *   <li>编排器持有ToolBus引用，用于执行子工具</li>
 *   <li>编排器不被ToolBus管理，打破循环依赖</li>
 *   <li>编排类执行器（FlowExecutor）依赖编排器而非ToolBus</li>
 * </ul>
 */
@Slf4j
@Component
public class DefaultToolOrchestrator implements ToolOrchestrator {

    private final ToolBus toolBus;

    public DefaultToolOrchestrator(ToolBus toolBus) {
        this.toolBus = toolBus;
        log.info("[DefaultToolOrchestrator] 初始化完成，绑定ToolBus: {}", toolBus.getClass().getSimpleName());
    }

    @Override
    public ToolResult invokeTool(String toolId, java.util.Map<String, Object> params, AgentContext context) {
        log.debug("[DefaultToolOrchestrator] 编排执行子工具: {}", toolId);
        return toolBus.invoke(toolId, params, context);
    }

    @Override
    public List<ToolResult> invokeTools(List<ToolBus.ToolCall> calls, AgentContext context) {
        log.debug("[DefaultToolOrchestrator] 批量编排执行 {} 个子工具", calls.size());
        return toolBus.invokeBatch(calls, context);
    }

    @Override
    public Optional<ToolDescriptor> getToolDescriptor(String toolId) {
        return toolBus.getTool(toolId);
    }
}
