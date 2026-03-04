package com.eazyai.ai.nexus.core.assistant;

import com.eazyai.ai.nexus.api.intent.IntentResult;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.core.memory.ChatMemoryStore;
import com.eazyai.ai.nexus.core.memory.PersistentChatMemoryManager;
import com.eazyai.ai.nexus.core.tool.DefaultToolBus;

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
 *   <li>通过 ToolBus 获取工具并适配</li>
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
    private ApplicationContext applicationContext;

    @Autowired
    private ToolBus toolBus;

    @Autowired(required = false)
    private DefaultToolBus defaultToolBus;

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
        
        // 获取应用可访问的动态工具
        Map<ToolSpecification, ToolExecutor> dynamicTools = getAccessibleTools(appId);
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
        
        Map<ToolSpecification, ToolExecutor> dynamicTools = getAccessibleTools(appId);
        log.info("[AssistantFactory] 应用 {} 查询到 {} 个可访问工具", appId, dynamicTools.size());
        
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
        return getAssistantByAppId(appId, sessionId, null);
    }

    /**
     * 根据应用ID创建带会话记忆和意图上下文的 Assistant
     * 
     * @param appId 应用ID
     * @param sessionId 会话ID
     * @param intentResult 意图分析结果（可选），用于注入系统提示和优化工具选择
     */
    public AgentAssistant getAssistantByAppId(String appId, String sessionId, IntentResult intentResult) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatLanguageModel 未配置");
        }

        log.info("[AssistantFactory] 开始为应用 {} 创建Assistant, sessionId={}, hasIntent={}", 
                appId, sessionId, intentResult != null);

        // 需要会话记忆的，创建新实例但使用持久化的 ChatMemory（带 appId 上下文）
        ChatMemoryStore.MemoryContext context = ChatMemoryStore.MemoryContext.of(appId, null);
        ChatMemory memory = persistentChatMemoryManager.getOrCreateMemory(sessionId, context);

        AiServices<AgentAssistant> builder = AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(memory);

        // 获取应用可访问的工具（使用可见性规则）
        Map<ToolSpecification, ToolExecutor> dynamicTools = getAccessibleTools(appId);
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
        
        // 注入意图分析上下文作为系统消息前缀
        if (intentResult != null) {
            String intentPrompt = buildIntentPrompt(intentResult);
            log.debug("[AssistantFactory] 注入意图上下文: {}", intentPrompt);
            // 注意：LangChain4j 的 @SystemMessage 是静态的，动态上下文需要在用户消息中处理
            // 这里通过在首次消息中添加意图上下文的方式实现
        }
        
        return builder.build();
    }

    /**
     * 构建意图上下文提示词
     */
    private String buildIntentPrompt(IntentResult intentResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n[意图分析上下文]\n");
        sb.append("用户意图: ").append(intentResult.getIntentType()).append("\n");
        sb.append("置信度: ").append(String.format("%.0f%%", intentResult.getConfidence() * 100)).append("\n");
        
        if (intentResult.getReasoning() != null) {
            sb.append("分析推理: ").append(intentResult.getReasoning()).append("\n");
        }
        
        if (!intentResult.getRecommendedTools().isEmpty()) {
            sb.append("推荐工具:\n");
            for (IntentResult.ToolRecommendation rec : intentResult.getRecommendedTools()) {
                sb.append("  - ").append(rec.getToolName())
                  .append(" (匹配度: ").append(String.format("%.0f%%", rec.getScore() * 100)).append(")")
                  .append(": ").append(rec.getReason()).append("\n");
            }
        }
        
        if (intentResult.isAmbiguous() && intentResult.getClarificationQuestion() != null) {
            sb.append("注意: 用户意图可能不明确，建议先澄清: ").append(intentResult.getClarificationQuestion()).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 内部方法：创建应用级别的 Assistant
     */
    private AgentAssistant createAssistantByAppIdInternal(String appId, ChatMemory memory) {
        var builder = AiServices.builder(AgentAssistant.class)
            .chatLanguageModel(chatModel);
        
        Map<ToolSpecification, ToolExecutor> dynamicTools = getAccessibleTools(appId);
        log.info("[AssistantFactory] 应用 {} 查询到 {} 个可访问工具", appId, dynamicTools.size());
        
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
     * 获取应用可访问的动态工具
     * 使用可见性规则：PUBLIC、PRIVATE、SHARED
     */
    private Map<ToolSpecification, ToolExecutor> getAccessibleTools(String appId) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();

        log.info("[AssistantFactory] 查询应用 {} 的可访问工具...", appId);

        // 使用 DefaultToolBus 的 findAccessibleTools 方法
        List<ToolDescriptor> accessibleTools;
        if (defaultToolBus != null) {
            accessibleTools = defaultToolBus.findAccessibleTools(appId);
        } else {
            // 兼容旧的 findByAppId 方法
            accessibleTools = toolBus.findByAppId(appId);
        }
        
        log.info("[AssistantFactory] 应用 {} 可访问 {} 个工具", appId, accessibleTools.size());

        for (ToolDescriptor descriptor : accessibleTools) {
            DynamicToolAdapter adapter = new DynamicToolAdapter(descriptor, toolBus);
            tools.put(adapter.toToolSpecification(), adapter);
            log.debug("[AssistantFactory] 加载工具: {} ({}) - visibility: {}", 
                descriptor.getName(), descriptor.getToolId(), descriptor.getVisibility());
        }

        // 如果没有可访问工具，记录警告
        if (tools.isEmpty()) {
            List<ToolDescriptor> allTools = toolBus.getAllTools();
            log.warn("[AssistantFactory] 应用 {} 没有可访问的工具！所有已注册工具: {}", appId, 
                    allTools.stream().map(t -> t.getName() + "(" + t.getVisibility() + ")").toList());
        }

        return tools;
    }

    /**
     * 获取应用关联的动态工具（已废弃，请使用 getAccessibleTools）
     * @deprecated 使用 {@link #getAccessibleTools(String)} 替代
     */
    @Deprecated
    private Map<ToolSpecification, ToolExecutor> getAppDynamicTools(String appId) {
        return getAccessibleTools(appId);
    }

    /**
     * 发现所有工具
     * 
     * 策略：扫描 Spring Bean 中带有 @Tool 注解的方法
     */
    private List<Object> discoverTools() {
        List<Object> tools = new ArrayList<>();

        // 扫描 @Tool 注解的 Bean
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(
            org.springframework.stereotype.Component.class);
        
        for (Object bean : beans.values()) {
            if (hasToolMethods(bean.getClass())) {
                tools.add(bean);
                log.debug("发现工具类: {}", bean.getClass().getSimpleName());
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
