package com.eazyai.ai.nexus.starter;

import com.eazyai.ai.nexus.api.intent.IntentAnalyzer;
import com.eazyai.ai.nexus.core.config.NexusProperties;
import com.eazyai.ai.nexus.core.intent.LlmIntentAnalyzer;
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
 *   <li>ToolBus - 工具总线（统一工具管理）</li>
 *   <li>ReActEngine - ReAct 执行引擎（统一入口）</li>
 * </ul>
 * 
 * <p>通过 @ComponentScan 自动扫描的组件：</p>
 * <ul>
 *   <li>AssistantFactory - Assistant 工厂</li>
 *   <li>ReActEngine - ReAct 执行引擎</li>
 *   <li>InternalOrchestrator - 内部编排器</li>
 *   <li>LangChain4jAgent - 基于 AiServices 的 Agent</li>
 *   <li>LangChain4jMemoryManager - 记忆管理器</li>
 *   <li>HttpClientConfig - HTTP客户端配置</li>
 * </ul>
 * 
 * <p>架构说明（v3.0）：</p>
 * <ul>
 *   <li>统一使用 ToolBus 管理所有工具</li>
 *   <li>通过 ThoughtEvent 回调暴露思考过程</li>
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
    @ConditionalOnMissingBean
    public IntentAnalyzer llmIntentAnalyzer() {
        return new LlmIntentAnalyzer();
    }

    // ReActEngine 由 @ComponentScan 自动扫描注册
    // ToolBus 由 @ComponentScan 自动扫描注册
}
