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
import dev.langchain4j.rag.content.retriever.ContentRetriever;
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
import java.util.function.Function;

/**
 * Assistant 工厂 - 重构版
 * 
 * <p>核心改进：</p>
 * <ul>
 *   <li>使用 systemMessageProvider 动态注入系统消息（支持意图上下文）</li>
 *   <li>使用 chatMemoryProvider 自动管理会话记忆（配合 @MemoryId）</li>
 *   <li>根据意图分析结果智能过滤工具</li>
 *   <li>简化缓存策略，避免缓存失效问题</li>
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

    // 默认工具实例缓存
    private List<Object> cachedDefaultTools;
    
    // 按应用缓存的工具映射
    private final Map<String, Map<ToolSpecification, ToolExecutor>> appToolsCache = new ConcurrentHashMap<>();

    // 默认系统消息模板
    private static final String DEFAULT_SYSTEM_MESSAGE = """
        你是一个智能助手，可以使用工具完成任务。
        
        工具使用规则：
        1. 当用户请求需要工具能力时，自动调用相应的工具
        2. 工具执行完成后，基于结果回答用户
        3. 如果不需要工具，直接回答用户问题
        4. 用中文回答，简洁专业
        """;

    @Override
    public void afterSingletonsInstantiated() {
        this.cachedDefaultTools = discoverTools();
        log.info("[AssistantFactory] 初始化完成，发现 {} 个默认工具", cachedDefaultTools.size());
        
        if (chatModel == null) {
            log.warn("[AssistantFactory] ChatLanguageModel 未配置，同步对话功能不可用");
        }
        if (streamingChatModel == null) {
            log.warn("[AssistantFactory] StreamingChatLanguageModel 未配置，流式功能不可用");
        }
    }

    // ================== 同步 Assistant ==================

    /**
     * 创建 Assistant（无会话记忆，无意图上下文）
     */
    public AgentAssistant createAssistant() {
        return createAssistant(null, null);
    }

    /**
     * 创建 Assistant（带意图上下文）
     * 
     * @param intentResult 意图分析结果，用于注入系统消息和过滤工具
     */
    public AgentAssistant createAssistant(IntentResult intentResult) {
        return createAssistant(null, intentResult);
    }

    /**
     * 创建 Assistant（带会话记忆和意图上下文）
     * 
     * <p>使用 LangChain4j 的：</p>
     * <ul>
     *   <li>systemMessageProvider - 动态注入意图上下文</li>
     *   <li>chatMemoryProvider - 自动管理会话记忆</li>
     * </ul>
     * 
     * @param appId 应用ID（可选，用于工具过滤）
     * @param intentResult 意图分析结果（可选，用于系统消息注入）
     */
    public AgentAssistant createAssistant(String appId, IntentResult intentResult) {
        return createAssistant(appId, intentResult, null);
    }

    /**
     * 创建 Assistant（带会话记忆、意图上下文和 RAG）
     * 
     * <p>使用 LangChain4j 的：</p>
     * <ul>
     *   <li>systemMessageProvider - 动态注入意图上下文</li>
     *   <li>chatMemoryProvider - 自动管理会话记忆</li>
     *   <li>contentRetriever - 自动进行 RAG 检索</li>
     * </ul>
     * 
     * @param appId 应用ID
     * @param intentResult 意图分析结果
     * @param contentRetriever RAG 内容检索器（可选）
     */
    public AgentAssistant createAssistant(String appId, IntentResult intentResult, 
                                           ContentRetriever contentRetriever) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatLanguageModel 未配置");
        }

        log.debug("[AssistantFactory] 创建 Assistant: appId={}, hasIntent={}, hasRag={}", 
                appId, intentResult != null, contentRetriever != null);

        var builder = AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatModel)
                // 动态系统消息提供者 - 支持意图上下文注入
                .systemMessageProvider(createSystemMessageProvider(intentResult))
                // 会话记忆提供者 - 配合 @MemoryId 自动管理
                .chatMemoryProvider(memoryId -> {
                    String sessionId = memoryId != null ? memoryId.toString() : UUID.randomUUID().toString();
                    ChatMemoryStore.MemoryContext context = appId != null 
                            ? ChatMemoryStore.MemoryContext.of(appId, null) 
                            : null;
                    return persistentChatMemoryManager.getOrCreateMemory(sessionId, context);
                });

        // 配置 RAG 内容检索器
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
            log.info("[AssistantFactory] 配置 RAG 内容检索器");
        }

        // 注册工具
        registerTools(builder, appId, intentResult);

        return builder.build();
    }

    // ================== 流式 Assistant ==================

    /**
     * 创建流式 Assistant（无会话记忆）
     */
    public StreamingAgentAssistant createStreamingAssistant() {
        return createStreamingAssistant(null, null);
    }

    /**
     * 创建流式 Assistant（带意图上下文）
     */
    public StreamingAgentAssistant createStreamingAssistant(IntentResult intentResult) {
        return createStreamingAssistant(null, intentResult);
    }

    /**
     * 创建流式 Assistant（带会话记忆和意图上下文）
     */
    public StreamingAgentAssistant createStreamingAssistant(String appId, IntentResult intentResult) {
        return createStreamingAssistant(appId, intentResult, null);
    }

    /**
     * 创建流式 Assistant（带会话记忆、意图上下文和 RAG）
     * 
     * <p>使用 LangChain4j 的：</p>
     * <ul>
     *   <li>systemMessageProvider - 动态注入意图上下文</li>
     *   <li>chatMemoryProvider - 自动管理会话记忆</li>
     *   <li>contentRetriever - 自动进行 RAG 检索</li>
     * </ul>
     * 
     * @param appId 应用ID
     * @param intentResult 意图分析结果
     * @param contentRetriever RAG 内容检索器（可选）
     */
    public StreamingAgentAssistant createStreamingAssistant(String appId, IntentResult intentResult,
                                                             ContentRetriever contentRetriever) {
        if (streamingChatModel == null) {
            throw new IllegalStateException("StreamingChatLanguageModel 未配置");
        }

        log.debug("[AssistantFactory] 创建 StreamingAssistant: appId={}, hasIntent={}, hasRag={}", 
                appId, intentResult != null, contentRetriever != null);

        var builder = AiServices.builder(StreamingAgentAssistant.class)
                .streamingChatLanguageModel(streamingChatModel)
                .systemMessageProvider(createSystemMessageProvider(intentResult))
                .chatMemoryProvider(memoryId -> {
                    String sessionId = memoryId != null ? memoryId.toString() : UUID.randomUUID().toString();
                    ChatMemoryStore.MemoryContext context = appId != null 
                            ? ChatMemoryStore.MemoryContext.of(appId, null) 
                            : null;
                    return persistentChatMemoryManager.getOrCreateMemory(sessionId, context);
                });

        // 配置 RAG 内容检索器
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
            log.info("[AssistantFactory] 配置 RAG 内容检索器");
        }

        registerTools(builder, appId, intentResult);

        return builder.build();
    }

    // ================== 系统消息提供者 ==================

    /**
     * 创建动态系统消息提供者
     * 
     * <p>LangChain4j 原生支持动态系统消息注入</p>
     * <p>返回 String 类型，LangChain4j 会自动包装为 SystemMessage</p>
     */
    private Function<Object, String> createSystemMessageProvider(IntentResult intentResult) {
        return memoryId -> {
            StringBuilder systemPrompt = new StringBuilder(DEFAULT_SYSTEM_MESSAGE);
            
            // 注入意图分析上下文
            if (intentResult != null) {
                String intentContext = buildIntentPrompt(intentResult);
                systemPrompt.append("\n\n").append(intentContext);
                log.debug("[AssistantFactory] 注入意图上下文到系统消息");
            }
            
            return systemPrompt.toString();
        };
    }

    /**
     * 构建意图上下文提示词
     */
    private String buildIntentPrompt(IntentResult intentResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("[意图分析上下文]\n");
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
            sb.append("提示: 优先使用推荐工具，如不满足需求可使用其他工具。\n");
        }
        
        if (intentResult.isAmbiguous() && intentResult.getClarificationQuestion() != null) {
            sb.append("注意: 用户意图可能不明确，建议先澄清: ").append(intentResult.getClarificationQuestion()).append("\n");
        }
        
        return sb.toString();
    }

    // ================== 工具注册 ==================

    /**
     * 注册工具到 AiServices Builder
     * 
     * <p>支持根据意图分析结果智能过滤工具</p>
     */
    private void registerTools(AiServices<?> builder, String appId, IntentResult intentResult) {
        Map<ToolSpecification, ToolExecutor> tools = getTools(appId, intentResult);
        
        if (!tools.isEmpty()) {
            builder.tools(tools);
            log.info("[AssistantFactory] 注册 {} 个工具", tools.size());
        } else if (!cachedDefaultTools.isEmpty()) {
            builder.tools(cachedDefaultTools);
            log.info("[AssistantFactory] 使用 {} 个默认工具", cachedDefaultTools.size());
        } else {
            log.warn("[AssistantFactory] 没有可用工具!");
        }
    }

    /**
     * 获取工具（支持意图过滤）
     * 
     * <p>如果意图分析结果包含推荐工具，优先使用推荐的高分工具</p>
     */
    private Map<ToolSpecification, ToolExecutor> getTools(String appId, IntentResult intentResult) {
        // 获取应用可访问的工具
        Map<ToolSpecification, ToolExecutor> allTools = getAppTools(appId);
        
        // 如果有意图分析结果，且包含推荐工具，进行过滤
        if (intentResult != null && !intentResult.getRecommendedTools().isEmpty()) {
            Set<String> recommendedToolNames = intentResult.getRecommendedTools().stream()
                    .map(IntentResult.ToolRecommendation::getToolName)
                    .collect(java.util.stream.Collectors.toSet());
            
            // 过滤：保留推荐工具 + 必要的基础工具（如时间、计算等）
            Map<ToolSpecification, ToolExecutor> filteredTools = new LinkedHashMap<>();
            for (Map.Entry<ToolSpecification, ToolExecutor> entry : allTools.entrySet()) {
                String toolName = entry.getKey().name();
                // 保留推荐工具或基础工具
                if (recommendedToolNames.contains(toolName) || isEssentialTool(toolName)) {
                    filteredTools.put(entry.getKey(), entry.getValue());
                }
            }
            
            log.info("[AssistantFactory] 意图过滤: {} -> {} 个工具", allTools.size(), filteredTools.size());
            return filteredTools;
        }
        
        return allTools;
    }

    /**
     * 判断是否为必要的基础工具（不应被过滤）
     */
    private boolean isEssentialTool(String toolName) {
        // 基础工具：时间、计算等
        return toolName.contains("time") || toolName.contains("calculate") || toolName.contains("format");
    }

    /**
     * 获取应用可访问的工具
     */
    private Map<ToolSpecification, ToolExecutor> getAppTools(String appId) {
        if (appId == null) {
            return new LinkedHashMap<>();
        }
        
        return appToolsCache.computeIfAbsent(appId, id -> {
            Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
            
            List<ToolDescriptor> accessibleTools;
            if (defaultToolBus != null) {
                accessibleTools = defaultToolBus.findAccessibleTools(id);
            } else {
                accessibleTools = toolBus.findByAppId(id);
            }
            
            for (ToolDescriptor descriptor : accessibleTools) {
                DynamicToolAdapter adapter = new DynamicToolAdapter(descriptor, toolBus);
                tools.put(adapter.toToolSpecification(), adapter);
            }
            
            log.debug("[AssistantFactory] 应用 {} 有 {} 个可访问工具", id, tools.size());
            return tools;
        });
    }

    // ================== 工具发现 ==================

    /**
     * 发现所有 Spring Bean 中的 @Tool 方法
     */
    private List<Object> discoverTools() {
        List<Object> tools = new ArrayList<>();
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(
            org.springframework.stereotype.Component.class);
        
        for (Object bean : beans.values()) {
            if (hasToolMethods(bean.getClass())) {
                tools.add(bean);
                log.debug("[AssistantFactory] 发现工具类: {}", bean.getClass().getSimpleName());
            }
        }
        
        return tools;
    }

    private boolean hasToolMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class)) {
                return true;
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return hasToolMethods(superClass);
        }
        return false;
    }

    // ================== 会话管理 ==================

    /**
     * 清理会话记忆
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
     * 检查会话是否存在
     */
    public boolean sessionExists(String sessionId) {
        return persistentChatMemoryManager.exists(sessionId);
    }

    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return persistentChatMemoryManager.getActiveSessionCount();
    }

    // ================== 缓存管理 ==================

    /**
     * 刷新工具缓存
     */
    public void refreshTools() {
        this.cachedDefaultTools = discoverTools();
        this.appToolsCache.clear();
        log.info("[AssistantFactory] 工具缓存已刷新，默认工具数: {}", cachedDefaultTools.size());
    }

    /**
     * 刷新指定应用的工具缓存
     */
    public void refreshAppCache(String appId) {
        appToolsCache.remove(appId);
        log.info("[AssistantFactory] 刷新应用工具缓存: appId={}", appId);
    }
}
