package com.eazyai.ai.nexus.core.assistant;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.StreamEvent;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.eazyai.ai.nexus.core.event.StreamingEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态工具适配器
 * 将统一工具描述符适配为LangChain4j可识别的工具
 *
 * <h3>架构位置：</h3>
 * <pre>
 * LangChain4j (LLM工具调用)
 *        ↓
 * DynamicToolAdapter (当前类) ← LangChain4j适配层
 *        ↓
 * 统一工具总线 (ToolBus)
 *        ↓
 * 执行器层 (ToolExecutor)
 * </pre>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>支持统一工具描述符(ToolDescriptor)</li>
 *   <li>委托给统一工具总线(ToolBus)执行</li>
 *   <li>支持多协议扩展</li>
 *   <li>统一的结果格式处理</li>
 * </ul>
 */
@Slf4j
public class DynamicToolAdapter implements ToolExecutor {

    private static StreamingEventBus eventBus;

    @Autowired
    public void setEventBus(StreamingEventBus eventBus) {
        DynamicToolAdapter.eventBus = eventBus;
    }

    private final ToolDescriptor descriptor;
    private final ToolBus toolBus;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 当前请求ID（在流式执行时设置）
     */
    private static final ThreadLocal<String> currentRequestId = new ThreadLocal<>();

    /**
     * 当前智能体上下文（在流式执行时设置）
     */
    private static final ThreadLocal<AgentContext> currentContext = new ThreadLocal<>();

    /**
     * 设置当前请求ID（用于事件发布）
     */
    public static void setCurrentRequestId(String requestId) {
        currentRequestId.set(requestId);
    }

    /**
     * 清除当前请求ID
     */
    public static void clearCurrentRequestId() {
        currentRequestId.remove();
    }

    /**
     * 设置当前智能体上下文
     */
    public static void setCurrentContext(AgentContext context) {
        currentContext.set(context);
    }

    /**
     * 清除当前智能体上下文
     */
    public static void clearCurrentContext() {
        currentContext.remove();
    }

    /**
     * 构造函数：使用统一工具描述符
     */
    public DynamicToolAdapter(ToolDescriptor descriptor, ToolBus toolBus) {
        this.descriptor = descriptor;
        this.toolBus = toolBus;
    }

    /**
     * 构建ToolSpecification
     * 用于向LLM描述工具的能力和参数
     */
    public ToolSpecification toToolSpecification() {
        ToolSpecification.Builder builder = ToolSpecification.builder()
                .name(sanitizeToolName(descriptor.getName()))
                .description(buildDescription());

        // 使用 JsonObjectSchema 构建参数定义
        if (descriptor.getParameters() != null && !descriptor.getParameters().isEmpty()) {
            builder.parameters(buildJsonObjectSchema());
        }

        return builder.build();
    }

    /**
     * 构建工具描述
     */
    private String buildDescription() {
        StringBuilder desc = new StringBuilder(descriptor.getDescription());

        if (descriptor.getParameters() != null && !descriptor.getParameters().isEmpty()) {
            desc.append("\n\n参数说明:");
            for (ToolDescriptor.ParamDefinition param : descriptor.getParameters()) {
                desc.append("\n- ").append(param.getName())
                        .append(" (").append(param.getType()).append(")")
                        .append(Boolean.TRUE.equals(param.getRequired()) ? " [必填]" : "")
                        .append(": ").append(param.getDescription() != null ? param.getDescription() : "");
            }
        }

        return desc.toString();
    }

    /**
     * 构建JsonObjectSchema参数定义
     */
    private JsonObjectSchema buildJsonObjectSchema() {
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();

        for (ToolDescriptor.ParamDefinition param : descriptor.getParameters()) {
            String name = param.getName();
            String description = param.getDescription();

            // 根据类型添加属性
            String type = param.getType() != null ? param.getType().toLowerCase() : "string";
            switch (type) {
                case "integer", "int", "long" -> schemaBuilder.addIntegerProperty(name, description);
                case "number", "float", "double" -> schemaBuilder.addNumberProperty(name, description);
                case "boolean", "bool" -> schemaBuilder.addBooleanProperty(name, description);
                case "array", "list" -> schemaBuilder.addProperty(name,
                        JsonArraySchema.builder()
                                .description(description)
                                .items(JsonObjectSchema.builder()
                                        .description("数组元素")
                                        .build())
                                .build());
                case "object" -> schemaBuilder.addProperty(name,
                        JsonObjectSchema.builder()
                                .description(description)
                                .build());
                default -> schemaBuilder.addStringProperty(name, description);
            }

            // 处理枚举值
            if (param.getOptions() != null && !param.getOptions().isEmpty()) {
                schemaBuilder.addEnumProperty(name, param.getOptions(), description);
            }

            // 设置必填参数
            if (Boolean.TRUE.equals(param.getRequired())) {
                schemaBuilder.required(name);
            }
        }

        return schemaBuilder.build();
    }

    /**
     * 清理工具名称（只允许字母、数字、下划线、连字符）
     */
    private String sanitizeToolName(String name) {
        if (name == null) return "unknown_tool";
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * 执行工具
     * ToolExecutor接口方法
     */
    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        log.info("[DynamicToolAdapter] 执行工具: {} - 参数: {}", descriptor.getName(), toolExecutionRequest.arguments());

        long startTime = System.currentTimeMillis();
        Object input = toolExecutionRequest.arguments();
        String resultJson;
        boolean success = false;
        String errorMessage = null;
        String requestId = currentRequestId.get();
        AgentContext context = currentContext.get();

        // 发布工具调用开始事件
        if (eventBus != null && requestId != null) {
            eventBus.publishToolCallStart(requestId, descriptor.getName(),
                    descriptor.getDescription(), input);
        }

        try {
            // 解析参数
            Map<String, Object> params = new HashMap<>();
            String arguments = toolExecutionRequest.arguments();
            if (arguments != null && !arguments.isEmpty() && !arguments.equals("{}")) {
                params = objectMapper.readValue(arguments, Map.class);
            }

            // 通过统一工具总线调用
            ToolResult result = toolBus.invoke(descriptor.getToolId(), params, context);

            // 返回结果
            if (result.isSuccess()) {
                resultJson = objectMapper.writeValueAsString(result.getData());
                success = true;
            } else {
                resultJson = "工具执行失败: " + result.getErrorMessage();
                errorMessage = result.getErrorMessage();
            }
        } catch (Exception e) {
            log.error("[DynamicToolAdapter] 工具执行异常: {}", e.getMessage(), e);
            resultJson = "工具执行异常: " + e.getMessage();
            errorMessage = e.getMessage();
        }

        // 记录工具调用到上下文
        long executionTime = System.currentTimeMillis() - startTime;
        ToolExecutionContext.current().recordToolCall(
                descriptor.getName(),
                descriptor.getDescription(),
                input,
                resultJson,
                executionTime,
                success,
                errorMessage
        );

        // 发布工具调用结束事件
        if (eventBus != null && requestId != null) {
            eventBus.publishToolCallEnd(requestId, descriptor.getName(),
                    descriptor.getDescription(), input, resultJson, executionTime, success, errorMessage);
        }

        return resultJson;
    }

    /**
     * 获取工具描述符
     */
    public ToolDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * 获取工具名称
     */
    public String getName() {
        return descriptor.getName();
    }

    /**
     * 获取工具ID
     */
    public String getToolId() {
        return descriptor.getToolId();
    }
}
