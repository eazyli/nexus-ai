package com.eazyai.ai.nexus.api.tool;

import com.eazyai.ai.nexus.api.dto.AgentContext;

import java.util.List;
import java.util.Map;

/**
 * 工具编排器接口
 * 负责复杂工具（如流程、工作流）的编排执行
 *
 * <h3>分层架构：</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │                     应用层/协议层                         │
 * └─────────────────────────┬───────────────────────────────┘
 *                           ↓
 * ┌─────────────────────────────────────────────────────────┐
 * │              ToolBus（工具注册与查找）                     │
 * │  - 工具注册/注销                                         │
 * │  - 工具发现/查询                                         │
 * │  - 执行器管理                                           │
 * └─────────────────────────┬───────────────────────────────┘
 *                           ↓
 * ┌─────────────────────────────────────────────────────────┐
 * │           ToolOrchestrator（工具编排执行）                │
 * │  - 流程编排（串行/并行/条件/循环）                        │
 * │  - 子工具调度                                           │
 * │  - 执行上下文管理                                        │
 * └─────────────────────────┬───────────────────────────────┘
 *                           ↓
 * ┌─────────────────────────────────────────────────────────┐
 * │              ToolExecutor（原子工具执行）                 │
 * │  HttpToolExecutor / DbToolExecutor / FunctionExecutor  │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>职责分离：</h3>
 * <ul>
 *   <li>ToolBus：工具注册、发现、执行器路由（不包含编排逻辑）</li>
 *   <li>ToolOrchestrator：编排执行，支持流程、工作流等复杂执行模式</li>
 *   <li>ToolExecutor：原子工具执行，与外部系统交互</li>
 * </ul>
 *
 * @see ToolBus 工具总线
 * @see ToolExecutor 工具执行器
 */
public interface ToolOrchestrator {

    /**
     * 编排执行子工具
     * 
     * <p>由编排类执行器（如FlowExecutor）调用，执行其子步骤中的工具。</p>
     *
     * @param toolId  子工具ID
     * @param params  执行参数
     * @param context 智能体上下文
     * @return 执行结果
     */
    ToolResult invokeTool(String toolId, Map<String, Object> params, AgentContext context);

    /**
     * 批量编排执行子工具
     *
     * @param calls   工具调用列表
     * @param context 智能体上下文
     * @return 执行结果列表
     */
    List<ToolResult> invokeTools(List<ToolBus.ToolCall> calls, AgentContext context);

    /**
     * 获取工具描述符
     *
     * @param toolId 工具ID
     * @return 工具描述符
     */
    default java.util.Optional<ToolDescriptor> getToolDescriptor(String toolId) {
        return java.util.Optional.empty();
    }
}
