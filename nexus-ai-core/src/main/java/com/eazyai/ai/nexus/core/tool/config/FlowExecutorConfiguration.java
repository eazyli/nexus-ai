package com.eazyai.ai.nexus.core.tool.config;

import com.eazyai.ai.nexus.core.tool.DefaultToolBus;
import com.eazyai.ai.nexus.core.tool.executor.FlowExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 流程执行器配置类
 * 
 * <p>显式注册FlowExecutor到ToolBus，解决循环依赖问题。</p>
 *
 * <h3>依赖顺序：</h3>
 * <pre>
 * 1. ToolBus 创建
 * 2. ToolOrchestrator 创建（依赖 ToolBus）
 * 3. FlowExecutor 创建（依赖 ToolOrchestrator）
 * 4. FlowExecutor 注册到 ToolBus（通过此配置类）
 * </pre>
 *
 * <h3>为什么需要单独的配置类？</h3>
 * <ul>
 *   <li>FlowExecutor 实现 ToolExecutor 接口，需要注册到 ToolBus</li>
 *   <li>FlowExecutor 依赖 ToolOrchestrator（间接依赖 ToolBus）</li>
 *   <li>如果通过自动注入，Spring 无法解决构造器循环依赖</li>
 *   <li>通过配置类显式注册，确保依赖顺序正确</li>
 * </ul>
 */
@Slf4j
@Configuration
public class FlowExecutorConfiguration {

    @Autowired
    private DefaultToolBus toolBus;

    @Autowired
    private FlowExecutor flowExecutor;

    /**
     * 注册 FlowExecutor
     * 在所有Bean创建完成后，显式注册到ToolBus
     */
    @PostConstruct
    public void registerFlowExecutor() {
        toolBus.registerExecutor(flowExecutor);
        log.info("[FlowExecutorConfiguration] FlowExecutor 已注册到 ToolBus，执行器类型: {}", 
                flowExecutor.getExecutorType());
    }
}
