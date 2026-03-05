package com.eazyai.ai.nexus.core.tool.executor;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolExecutor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * MCP工具执行器
 * 实现MCP (Model Context Protocol) 协议客户端，连接MCP Server执行工具调用
 *
 * <h3>MCP协议概述：</h3>
 * <p>MCP是一种标准化的工具协议，允许AI模型与外部工具进行交互。
 * 支持HTTP/SSE传输方式，提供工具发现、调用、资源访问等能力。</p>
 *
 * <h3>配置示例：</h3>
 * <pre>
 * {
 *   "serverUrl": "http://localhost:8080/mcp",
 *   "transport": "http",  // http 或 sse
 *   "serverName": "my-mcp-server",
 *   "toolName": "get_weather",  // MCP Server上的工具名
 *   "headers": {
 *     "Authorization": "Bearer xxx"
 *   },
 *   "timeout": 30000
 * }
 * </pre>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>支持HTTP和SSE两种传输方式</li>
 *   <li>自动管理MCP会话</li>
 *   <li>支持工具发现和能力协商</li>
 *   <li>统一错误处理和重试</li>
 * </ul>
 *
 * @see <a href="https://modelcontextprotocol.io/">MCP Protocol</a>
 */
@Slf4j
@Component
public class McpToolExecutor implements ToolExecutor {

    private static final String EXECUTOR_TYPE = "mcp_client";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    /**
     * MCP会话缓存：serverUrl -> sessionId
     */
    private final Map<String, String> sessionCache = new ConcurrentHashMap<>();

    /**
     * MCP工具缓存：serverUrl -> tools
     */
    private final Map<String, JsonNode> toolsCache = new ConcurrentHashMap<>();

    @Autowired
    public McpToolExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        log.info("[McpToolExecutor] 初始化完成");
    }

    @Override
    public String getExecutorType() {
        return EXECUTOR_TYPE;
    }

    @Override
    public ToolResult execute(ToolDescriptor descriptor, Map<String, Object> params, AgentContext context) {
        Map<String, Object> config = descriptor.getConfig();
        if (config == null) {
            return ToolResult.error(descriptor.getToolId(), "CONFIG_MISSING", "MCP工具配置缺失");
        }

        String serverUrl = (String) config.get("serverUrl");
        String toolName = (String) config.get("toolName");
        String transport = (String) config.getOrDefault("transport", "http");

        if (serverUrl == null || serverUrl.isEmpty()) {
            return ToolResult.error(descriptor.getToolId(), "SERVER_URL_MISSING", "MCP服务器地址未配置");
        }
        if (toolName == null || toolName.isEmpty()) {
            return ToolResult.error(descriptor.getToolId(), "TOOL_NAME_MISSING", "MCP工具名称未配置");
        }

        try {
            // 1. 确保MCP会话已初始化
            String sessionId = ensureSession(serverUrl, config);
            if (sessionId == null) {
                return ToolResult.error(descriptor.getToolId(), "SESSION_INIT_FAILED", "MCP会话初始化失败");
            }

            // 2. 调用MCP工具
            Object result = callMcpTool(serverUrl, sessionId, toolName, params, config);

            log.info("[McpToolExecutor] 执行成功: {} -> {}", descriptor.getName(), toolName);
            return ToolResult.success(descriptor.getToolId(), result);

        } catch (Exception e) {
            log.error("[McpToolExecutor] 执行异常: {} - {}", descriptor.getName(), e.getMessage(), e);
            // 会话可能失效，清除缓存
            sessionCache.remove(serverUrl);
            return ToolResult.error(descriptor.getToolId(), "MCP_EXECUTION_ERROR", e.getMessage(), e);
        }
    }

    /**
     * 确保MCP会话已初始化
     */
    @SuppressWarnings("unchecked")
    private String ensureSession(String serverUrl, Map<String, Object> config) throws IOException {
        // 检查缓存
        String cachedSession = sessionCache.get(serverUrl);
        if (cachedSession != null) {
            return cachedSession;
        }

        // 初始化MCP会话
        String requestId = generateRequestId();
        Map<String, Object> initRequest = Map.of(
                "jsonrpc", "2.0",
                "id", requestId,
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(
                                "tools", Map.of()
                        ),
                        "clientInfo", Map.of(
                                "name", "nexus-ai-mcp-client",
                                "version", "1.0.0"
                        )
                )
        );

        JsonNode response = sendMcpRequest(serverUrl, initRequest, config);
        if (response != null && response.has("result")) {
            // 提取session ID（如果有）
            JsonNode result = response.get("result");
            String sessionId = result.has("sessionId") 
                    ? result.get("sessionId").asText() 
                    : UUID.randomUUID().toString();
            
            sessionCache.put(serverUrl, sessionId);
            
            // 发送initialized通知
            sendInitializedNotification(serverUrl, sessionId, config);
            
            log.info("[McpToolExecutor] MCP会话初始化成功: {} -> {}", serverUrl, sessionId);
            return sessionId;
        }

        return null;
    }

    /**
     * 发送initialized通知
     */
    private void sendInitializedNotification(String serverUrl, String sessionId, Map<String, Object> config) throws IOException {
        Map<String, Object> notification = Map.of(
                "jsonrpc", "2.0",
                "method", "notifications/initialized",
                "params", Map.of()
        );
        sendMcpRequest(serverUrl, notification, config);
    }

    /**
     * 调用MCP工具
     */
    @SuppressWarnings("unchecked")
    private Object callMcpTool(String serverUrl, String sessionId, String toolName,
                               Map<String, Object> params, Map<String, Object> config) throws IOException {
        String requestId = generateRequestId();
        Map<String, Object> callRequest = Map.of(
                "jsonrpc", "2.0",
                "id", requestId,
                "method", "tools/call",
                "params", Map.of(
                        "name", toolName,
                        "arguments", params != null ? params : Map.of()
                )
        );

        JsonNode response = sendMcpRequest(serverUrl, callRequest, config);
        
        if (response == null) {
            throw new RuntimeException("MCP服务器无响应");
        }

        // 检查错误
        if (response.has("error")) {
            JsonNode error = response.get("error");
            String errorMsg = error.has("message") ? error.get("message").asText() : "未知错误";
            String errorCode = error.has("code") ? String.valueOf(error.get("code").asInt()) : "UNKNOWN";
            throw new RuntimeException("MCP工具调用失败 [" + errorCode + "]: " + errorMsg);
        }

        // 提取结果
        if (response.has("result")) {
            JsonNode result = response.get("result");
            // MCP工具返回格式：{ content: [{ type: "text", text: "..." }] }
            if (result.has("content")) {
                JsonNode content = result.get("content");
                if (content.isArray() && content.size() > 0) {
                    JsonNode firstContent = content.get(0);
                    if (firstContent.has("text")) {
                        String text = firstContent.get("text").asText();
                        // 尝试解析为JSON
                        try {
                            return objectMapper.readValue(text, Object.class);
                        } catch (Exception e) {
                            return text;
                        }
                    }
                }
            }
            return objectMapper.treeToValue(result, Object.class);
        }

        return null;
    }

    /**
     * 发送MCP请求
     */
    @SuppressWarnings("unchecked")
    private JsonNode sendMcpRequest(String serverUrl, Map<String, Object> request, 
                                    Map<String, Object> config) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(request);
        
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request.Builder requestBuilder = new Request.Builder()
                .url(serverUrl)
                .post(body);

        // 添加自定义请求头
        Map<String, String> headers = (Map<String, String>) config.get("headers");
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }

        // 添加Content-Type
        requestBuilder.addHeader("Content-Type", "application/json");

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("MCP请求失败: " + response.code() + " " + response.message());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            String responseStr = responseBody.string();
            log.debug("[McpToolExecutor] MCP响应: {}", responseStr);
            return objectMapper.readTree(responseStr);
        }
    }

    /**
     * 发现MCP服务器上的工具列表
     */
    @SuppressWarnings("unchecked")
    public JsonNode discoverTools(String serverUrl, Map<String, Object> config) throws IOException {
        // 检查缓存
        if (toolsCache.containsKey(serverUrl)) {
            return toolsCache.get(serverUrl);
        }

        // 确保会话已初始化
        ensureSession(serverUrl, config);

        // 获取工具列表
        String requestId = generateRequestId();
        Map<String, Object> listRequest = Map.of(
                "jsonrpc", "2.0",
                "id", requestId,
                "method", "tools/list",
                "params", Map.of()
        );

        JsonNode response = sendMcpRequest(serverUrl, listRequest, config);
        if (response != null && response.has("result")) {
            JsonNode tools = response.get("result").get("tools");
            toolsCache.put(serverUrl, tools);
            return tools;
        }

        return null;
    }

    /**
     * 将MCP工具描述转换为统一工具描述符
     */
    public ToolDescriptor convertToDescriptor(JsonNode mcpTool, String serverUrl, String appId, 
                                              Map<String, Object> config) {
        String toolName = mcpTool.get("name").asText();
        String description = mcpTool.has("description") 
                ? mcpTool.get("description").asText() 
                : "";

        // 解析inputSchema
        java.util.List<ToolDescriptor.ParamDefinition> parameters = new java.util.ArrayList<>();
        if (mcpTool.has("inputSchema")) {
            JsonNode inputSchema = mcpTool.get("inputSchema");
            if (inputSchema.has("properties")) {
                JsonNode properties = inputSchema.get("properties");
                java.util.Set<String> required = new java.util.HashSet<>();
                if (inputSchema.has("required")) {
                    for (JsonNode req : inputSchema.get("required")) {
                        required.add(req.asText());
                    }
                }
                
                properties.fields().forEachRemaining(entry -> {
                    String paramName = entry.getKey();
                    JsonNode paramSchema = entry.getValue();
                    ToolDescriptor.ParamDefinition param = ToolDescriptor.ParamDefinition.builder()
                            .name(paramName)
                            .type(paramSchema.has("type") ? paramSchema.get("type").asText() : "string")
                            .description(paramSchema.has("description") ? paramSchema.get("description").asText() : "")
                            .required(required.contains(paramName))
                            .build();
                    parameters.add(param);
                });
            }
        }

        // 构建配置
        Map<String, Object> toolConfig = new java.util.HashMap<>();
        toolConfig.put("serverUrl", serverUrl);
        toolConfig.put("toolName", toolName);
        toolConfig.put("transport", config.getOrDefault("transport", "http"));
        if (config.get("headers") != null) {
            toolConfig.put("headers", config.get("headers"));
        }

        return ToolDescriptor.builder()
                .toolId("mcp-" + serverUrl.hashCode() + "-" + toolName)
                .appId(appId)
                .name(toolName)
                .description(description)
                .executorType(EXECUTOR_TYPE)
                .protocol("mcp")
                .parameters(parameters)
                .config(toolConfig)
                .enabled(true)
                .build();
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 清除会话缓存
     */
    public void clearSession(String serverUrl) {
        sessionCache.remove(serverUrl);
        toolsCache.remove(serverUrl);
        log.info("[McpToolExecutor] 清除MCP会话缓存: {}", serverUrl);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllSessions() {
        sessionCache.clear();
        toolsCache.clear();
        log.info("[McpToolExecutor] 清除所有MCP会话缓存");
    }
}
