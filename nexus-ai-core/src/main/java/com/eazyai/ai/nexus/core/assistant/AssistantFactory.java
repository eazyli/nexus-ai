package com.eazyai.ai.nexus.core.assistant;

import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.core.memory.ChatMemoryStore;
import com.eazyai.ai.nexus.core.memory.PersistentChatMemoryManager;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Assistant 工厂
 * 负责创建和配置 LangChain4j AiServices
 * 
 * <p>核心功能：</p>
 * <ul>
 *   <li>自动发现 Spring Bean 中的 @Tool 方法</li>
 *   <li>将 PluginRegistry 中的插件适配为工具</li>
 *   <li>管理会话级 ChatMemory</li>
 * </ul>
 */
@Slf4j
@Component
public class AssistantFactory implements SmartInitializingSingleton {

    @Autowired(required = false)
    private ChatLanguageModel chatModel;

    @Autowired(required = false)
    private StreamingChatLanguageModel streamingChatModel;

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ToolBus toolBus;

    @Autowired
    private PersistentChatMemoryManager persistentChatMemoryManager;

    // 缓存的默认 Assistant 实例
    private AgentAssistant defaultAssistant;
    
    // 缓存的默认流式 Assistant 实例
    private StreamingAgentAssistant defaultStreamingAssistant;
    
    // Assistant实例缓存（按appId缓存，不带会话记忆）
    private final Map<String, AgentAssistant> assistantCache = new ConcurrentHashMap<>();
    
    // 流式Assistant实例缓存（按appId缓存，不带会话记忆）
    private final Map<String, StreamingAgentAssistant> streamingAssistantCache = new ConcurrentHashMap<>();

    // 工具实例缓存
    private List<Object> cachedTools;

    @Override
    public void afterSingletonsInstantiated() {
        this.cachedTools = discoverTools();
        
        if (chatModel != null) {
            this.defaultAssistant = createAssistant(null);
            log.info("AgentAssistant 初始化完成，已注册 {} 个工具", cachedTools.size());
        } else {
            log.warn("ChatLanguageModel 未配置，AgentAssistant 不可用");
        }
        
        if (streamingChatModel != null) {
            this.defaultStreamingAssistant = createStreamingAssistant(null);
            log.info("StreamingAgentAssistant 初始化完成");
        } else {
            log.warn("StreamingChatLanguageModel 未配置，流式功能不可用");
        }
    }

    /**
     * 获取默认 Assistant（无记忆）
     */
    public AgentAssistant getAssistant() {
        if (defaultAssistant == null) {
            throw new IllegalStateException("AgentAssistant 未初始化，请检查 ChatLanguageModel 配置");
        }
        return defaultAssistant;
    }

    // ================== 流式 Assistant 方法 ==================

    /**
     * 获取默认流式 Assistant（无记忆）
     */
    public StreamingAgentAssistant getStreamingAssistant() {
        if (defaultStreamingAssistant == null) {
            throw new IllegalStateException("StreamingAgentAssistant 未初始化，请检查 StreamingChatLanguageModel 配置");
        }
        return defaultStreamingAssistant;
    }

    /**
     * 获取带会话记忆的流式 Assistant
     * 使用持久化存储保存会话记忆
     */
    public StreamingAgentAssistant getStreamingAssistantWithMemory(String sessionId) {
        if (streamingChatModel == null) {
            throw new IllegalStateException("StreamingChatLanguageModel 未配置");
        }

        // 使用持久化存储的 ChatMemory
        ChatMemory memory = persistentChatMemoryManager.getOrCreateMemory(sessionId);

        return AiServices.builder(StreamingAgentAssistant.class)
            .streamingChatLanguageModel(streamingChatModel)
            .chatMemory(memory)
            .tools(cachedTools)
            .build();
    }

    /**
     * 创建流式 Assistant
     */
    public StreamingAgentAssistant createStreamingAssistant(ChatMemory memory) {
        if (streamingChatModel == null) {
            throw new IllegalStateException("StreamingChatLanguageModel 未配置");
        }

        var builder = AiServices.builder(StreamingAgentAssistant.class)
            .streamingChatLanguageModel(streamingChatModel)
            .tools(cachedTools);
        
        if (memory != null) {
            builder.chatMemory(memory);
        }
        
        return builder.build();
    }

    /**
     * 根据应用ID创建流式 Assistant
     */
    public StreamingAgentAssistant getStreamingAssistantByAppId(String appId) {
        return getStreamingAssistantByAppId(appId, null);
    }

    /**
     * 根据应用ID创建带会话记忆的流式 Assistant
     * 使用缓存机制优化性能
     */
    public StreamingAgentAssistant getStreamingAssistantByAppId(String appId, String sessionId) {
        if (streamingChatModel == null) {
            throw new IllegalStateException("StreamingChatLanguageModel 未配置");
        }

        log.info("[AssistantFactory] 开始为应用 {} 创建StreamingAssistant, sessionId={}", appId, sessionId);

        // 如果不需要会话记忆，使用缓存的实例
        if (sessionId == null) {
            return streamingAssistantCache.computeIfAbsent(appId, id -> {
                log.info("[AssistantFactory] 创建并缓存应用 {} 的StreamingAssistant", appId);
                return createStreamingAssistantByAppIdInternal(appId, null);
            });
        }

        // 需要会话记忆的，创建新实例但使用持久化的 ChatMemory
        ChatMemory memory = persistentChatMemoryManager.getOrCreateMemory(sessionId);
        StreamingAgentAssistant assistant = createStreamingAssistantByAppIdInternal(appId, null);
        
        // 重新构建带记忆的实例
        var builder = AiServices.builder(StreamingAgentAssistant.class)
            .streamingChatLanguageModel(streamingChatModel)
            .chatMemory(memory);
        
        // 获取应用关联的动态工具
        Map<ToolSpecification, ToolExecutor> dynamicTools = getAppDynamicTools(appId);
        if (!dynamicTools.isEmpty()) {
            builder.tools(dynamicTools);
        } else if (cachedTools != null && !cachedTools.isEmpty()) {
            builder.tools(cachedTools);
        }
        
        return builder.build();
    }

    /**
     * 内部方法：创建应用级别的流式 Assistant
     */
    private StreamingAgentAssistant createStreamingAssistantByAppIdInternal(String appId, ChatMemory memory) {
        var builder = AiServices.builder(StreamingAgentAssistant.class)
            .streamingChatLanguageModel(streamingChatModel);
        
        Map<ToolSpecification, ToolExecutor> dynamicTools = getAppDynamicTools(appId);
        log.info("[AssistantFactory] 应用 {} 查询到 {} 个动态工具", appId, dynamicTools.size());
        
        if (!dynamicTools.isEmpty()) {
            builder.tools(dynamicTools);
        } else if (cachedTools != null && !cachedTools.isEmpty()) {
            builder.tools(cachedTools);
            log.info("[AssistantFactory] 应用 {} 无专属工具，使用 {} 个默认工具", appId, cachedTools.size());
        }
        
        if (memory != null) {
            builder.chatMemory(memory);
        }
        
        return builder.build();
    }

    /**
     * 获取带会话记忆的 Assistant
     * 使用持久化存储保存会话记忆
     */
    public AgentAssistant getAssistantWithMemory(String sessionId) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatLanguageModel 未配置");
        }

        // 使用持久化存储的 ChatMemory（无 appId 上下文）
        ChatMemory memory = persistentChatMemoryManager.getOrCreateMemory(sessionId);

        return AiServices.builder(AgentAssistant.class)
            .chatLanguageModel(chatModel)
            .chatMemory(memory)
            .tools(cachedTools)
            .build();
    }

    /**
     * 获取带会话记忆的 Assistant（带应用上下文）
     * 使用持久化存储保存会话记忆
     */
    public AgentAssistant getAssistantWithMemory(String sessionId, String appId) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatLanguageModel 未配置");
        }

        // 使用持久化存储的 ChatMemory（带 appId 上下文）
        ChatMemoryStore.MemoryContext context = ChatMemoryStore.MemoryContext.of(appId, null);
        ChatMemory memory = persistentChatMemoryManager.getOrCreateMemory(sessionId, context);

        return AiServices.builder(AgentAssistant.class)
            .chatLanguageModel(chatModel)
            .chatMemory(memory)
            .tools(cachedTools)
            .build();
    }

    /**
     * 创建自定义配置的 Assistant
     */
    public AgentAssistant createAssistant(ChatMemory memory) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatLanguageModel 未配置");
        }

        var builder = AiServices.builder(AgentAssistant.class)
            .chatLanguageModel(chatModel)
            .tools(cachedTools);
        
        if (memory != null) {
            builder.chatMemory(memory);
        }
        
        return builder.build();
    }

    /**
     * 根据应用ID创建 Assistant
     * 只加载该应用关联的工具
     */
    public AgentAssistant getAssistantByAppId(String appId) {
        return getAssistantByAppId(appId, null);
    }

    /**
     * 根据应用ID创建带会话记忆的 Assistant
     * 使用缓存机制优化性能
     */
    public AgentAssistant getAssistantByAppId(String appId, String sessionId) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatLanguageModel 未配置");
        }

        log.info("[AssistantFactory] 开始为应用 {} 创建Assistant, sessionId={}", appId, sessionId);

        // 如果不需要会话记忆，使用缓存的实例
        if (sessionId == null) {
            return assistantCache.computeIfAbsent(appId, id -> {
                log.info("[AssistantFactory] 创建并缓存应用 {} 的Assistant", appId);
                return createAssistantByAppIdInternal(appId, null);
            });
        }

        // 需要会话记忆的，创建新实例但使用持久化的 ChatMemory（带 appId 上下文）
        ChatMemoryStore.MemoryContext context = ChatMemoryStore.MemoryContext.of(appId, null);
        ChatMemory memory = persistentChatMemoryManager.getOrCreateMemory(sessionId, context);

        AiServices<AgentAssistant> builder = AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(memory);

        // 获取应用关联的动态工具
        Map<ToolSpecification, ToolExecutor> dynamicTools = getAppDynamicTools(appId);
        if (!dynamicTools.isEmpty()) {
            builder.tools(dynamicTools);
            dynamicTools.keySet().forEach(spec -> 
                log.info("[AssistantFactory] 注册工具: {} - {}", spec.name(), spec.description()));
        } else if (cachedTools != null && !cachedTools.isEmpty()) {
            builder.tools(cachedTools);
            log.info("[AssistantFactory] 应用 {} 无专属工具，使用 {} 个默认工具", appId, cachedTools.size());
        } else {
            log.warn("[AssistantFactory] 应用 {} 没有任何可用工具!", appId);
        }
        
        return builder.build();
    }

    /**
     * 内部方法：创建应用级别的 Assistant
     */
    private AgentAssistant createAssistantByAppIdInternal(String appId, ChatMemory memory) {
        var builder = AiServices.builder(AgentAssistant.class)
            .chatLanguageModel(chatModel);
        
        Map<ToolSpecification, ToolExecutor> dynamicTools = getAppDynamicTools(appId);
        log.info("[AssistantFactory] 应用 {} 查询到 {} 个动态工具", appId, dynamicTools.size());
        
        if (!dynamicTools.isEmpty()) {
            builder.tools(dynamicTools);
        } else if (cachedTools != null && !cachedTools.isEmpty()) {
            builder.tools(cachedTools);
            log.info("[AssistantFactory] 应用 {} 无专属工具，使用 {} 个默认工具", appId, cachedTools.size());
        } else {
            log.warn("[AssistantFactory] 应用 {} 没有任何可用工具!", appId);
        }
        
        if (memory != null) {
            builder.chatMemory(memory);
        }
        
        return builder.build();
    }

    /**
     * 获取应用关联的动态工具
     */
    private Map<ToolSpecification, ToolExecutor> getAppDynamicTools(String appId) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();

        log.info("[AssistantFactory] 查询应用 {} 的工具...", appId);
        
        // 获取应用关联的工具
        List<ToolDescriptor> toolDescriptors = toolBus.findByAppId(appId);
        log.info("[AssistantFactory] ToolBus.findByAppId({}) 返回 {} 个工具", appId, toolDescriptors.size());
        
        for (ToolDescriptor descriptor : toolDescriptors) {
            DynamicToolAdapter adapter = new DynamicToolAdapter(descriptor, toolBus);
            tools.put(adapter.toToolSpecification(), adapter);
            log.info("[AssistantFactory] 加载应用 {} 的工具: {} ({})", 
                appId, descriptor.getName(), descriptor.getToolId());
        }

        // 如果应用没有专属工具，检查是否有全局工具
        if (tools.isEmpty()) {
            List<ToolDescriptor> allTools = toolBus.getAllTools();
            log.info("[AssistantFactory] 所有已注册工具数量: {}", allTools.size());
            allTools.forEach(t -> log.info("[AssistantFactory] 已注册工具: {} - appId={}, enabled={}", 
                t.getName(), t.getAppId(), t.getEnabled()));
        }

        return tools;
    }

    /**
     * 发现所有工具
     * 
     * 策略：
     * 1. 扫描 Spring Bean 中带有 @Tool 注解的方法
     * 2. 注册 PluginRegistry 中的插件作为工具
     */
    private List<Object> discoverTools() {
        List<Object> tools = new ArrayList<>();

        // 1. 扫描 @Tool 注解的 Bean
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(
            org.springframework.stereotype.Component.class);
        
        for (Object bean : beans.values()) {
            if (hasToolMethods(bean.getClass())) {
                tools.add(bean);
                log.debug("发现工具类: {}", bean.getClass().getSimpleName());
            }
        }

        // 2. 从 PluginRegistry 获取插件并转换为工具
        for (PluginDescriptor descriptor : pluginRegistry.getAllPlugins()) {
            if (descriptor.isEnabled()) {
                pluginRegistry.getPlugin(descriptor.getId()).ifPresent(plugin -> {
                    Object toolAdapter = adaptPluginToTool(plugin);
                    if (toolAdapter != null) {
                        tools.add(toolAdapter);
                        log.info("注册插件为工具: {} - {}", descriptor.getId(), descriptor.getName());
                    }
                });
            }
        }

        return tools;
    }

    /**
     * 检查类是否有 @Tool 注解的方法
     */
    private boolean hasToolMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class)) {
                return true;
            }
        }
        // 检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return hasToolMethods(superClass);
        }
        return false;
    }

    /**
     * 将 Plugin 适配为 LangChain4j Tool
     */
    private Object adaptPluginToTool(Plugin plugin) {
        // 如果插件已经是工具类（有 @Tool 方法），直接返回
        if (hasToolMethods(plugin.getClass())) {
            return plugin;
        }
        
        // 否则创建一个适配器
        return new PluginToolAdapter(plugin);
    }

    /**
     * 清理会话记忆
     * 同时清理内存缓存和持久化存储
     */
    public void clearSessionMemory(String sessionId) {
        persistentChatMemoryManager.removeMemory(sessionId);
        log.info("[AssistantFactory] 清理会话记忆: sessionId={}", sessionId);
    }

    /**
     * 清理所有会话记忆
     */
    public void clearAllMemories() {
        persistentChatMemoryManager.clearAll();
        log.info("[AssistantFactory] 清理所有会话记忆");
    }

    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return persistentChatMemoryManager.getActiveSessionCount();
    }

    /**
     * 刷新工具缓存（动态加载插件后调用）
     * 同时清理 Assistant 实例缓存
     */
    public void refreshTools() {
        this.cachedTools = discoverTools();
        this.defaultAssistant = createAssistant(null);
        // 清理应用级别的缓存
        this.assistantCache.clear();
        this.streamingAssistantCache.clear();
        log.info("工具缓存已刷新，当前工具数: {}", cachedTools.size());
    }

    /**
     * 刷新指定应用的缓存
     * 当应用的工具配置变更时调用
     */
    public void refreshAppCache(String appId) {
        assistantCache.remove(appId);
        streamingAssistantCache.remove(appId);
        log.info("[AssistantFactory] 刷新应用缓存: appId={}", appId);
    }

    /**
     * 获取缓存的Assistant数量（用于监控）
     */
    public int getCachedAssistantCount() {
        return assistantCache.size();
    }

    /**
     * 获取缓存的流式Assistant数量（用于监控）
     */
    public int getCachedStreamingAssistantCount() {
        return streamingAssistantCache.size();
    }

    /**
     * 检查会话是否存在
     */
    public boolean sessionExists(String sessionId) {
        return persistentChatMemoryManager.exists(sessionId);
    }
}
