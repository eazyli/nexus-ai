package com.eazyai.ai.nexus.starter;

import com.eazyai.ai.nexus.api.executor.PluginExecutor;
import com.eazyai.ai.nexus.api.integrator.ResultIntegrator;
import com.eazyai.ai.nexus.api.intent.IntentAnalyzer;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import com.eazyai.ai.nexus.api.scheduler.PluginScheduler;
import com.eazyai.ai.nexus.core.config.NexusProperties;
import com.eazyai.ai.nexus.core.engine.AgentEngine;
import com.eazyai.ai.nexus.core.executor.DefaultPluginExecutor;
import com.eazyai.ai.nexus.core.integrator.DefaultResultIntegrator;
import com.eazyai.ai.nexus.core.intent.LlmIntentAnalyzer;
import com.eazyai.ai.nexus.core.registry.DefaultPluginRegistry;
import com.eazyai.ai.nexus.core.scheduler.DefaultPluginScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * AI Agent 自动配置类
 * 
 * <p>自动装配核心组件：</p>
 * <ul>
 *   <li>NexusProperties - 配置属性</li>
 *   <li>PluginRegistry - 插件注册中心</li>
 *   <li>PluginScheduler - 插件调度器</li>
 *   <li>PluginExecutor - 插件执行器</li>
 *   <li>ResultIntegrator - 结果整合器</li>
 *   <li>AgentEngine - Agent 引擎</li>
 * </ul>
 * 
 * <p>通过 @ComponentScan 自动扫描的组件：</p>
 * <ul>
 *   <li>AssistantFactory - Assistant 工厂</li>
 *   <li>HybridPlanner - 混合规划器（高级模式）</li>
 *   <li>LangChain4jAgent - 基于 AiServices 的 Agent</li>
 *   <li>LangChain4jMemoryManager - 记忆管理器</li>
 *   <li>HttpClientConfig - HTTP客户端配置</li>
 * </ul>
 */
@Configuration
@ComponentScan(basePackages = {
        "com.eazyai.ai.nexus.core",
        "com.eazyai.ai.nexus.plugin.manager"
})
@EnableConfigurationProperties(NexusProperties.class)
public class AgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PluginRegistry.class)
    public PluginRegistry pluginRegistry() {
        return new DefaultPluginRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(PluginScheduler.class)
    public PluginScheduler pluginScheduler() {
        return new DefaultPluginScheduler();
    }

    @Bean
    @ConditionalOnMissingBean(PluginExecutor.class)
    public PluginExecutor pluginExecutor() {
        return new DefaultPluginExecutor();
    }

    @Bean
    @ConditionalOnMissingBean(ResultIntegrator.class)
    public ResultIntegrator resultIntegrator() {
        return new DefaultResultIntegrator();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentAnalyzer llmIntentAnalyzer() {
        return new LlmIntentAnalyzer();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentEngine agentEngine() {
        return new AgentEngine();
    }
}
