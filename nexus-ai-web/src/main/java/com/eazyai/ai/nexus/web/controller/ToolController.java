package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.eazyai.ai.nexus.api.tool.ToolVisibility;
import com.eazyai.ai.nexus.application.app.ToolService;
import com.eazyai.ai.nexus.core.tool.McpProtocolAdapter;
import com.eazyai.ai.nexus.web.dto.DbToolRegisterRequest;
import com.eazyai.ai.nexus.web.dto.FunctionToolRegisterRequest;
import com.eazyai.ai.nexus.web.dto.HttpToolRegisterRequest;
import com.eazyai.ai.nexus.web.dto.McpToolRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具管理控制器
 * 提供动态工具注册和管理REST接口
 * 
 * <p>依赖 application 层的 ToolService，不再直接依赖 infra 层</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
@Tag(name = "工具管理接口", description = "动态工具的注册、查询、执行等操作")
public class ToolController {

    private final ToolBus toolBus;
    private final ToolService toolService;
    private final McpProtocolAdapter mcpProtocolAdapter;

    /**
     * 注册HTTP工具
     */
    @PostMapping("/http")
    @Operation(summary = "注册HTTP工具", description = "动态注册一个HTTP类型的工具，用于调用外部REST API")
    public ResponseEntity<ToolDescriptor> registerHttpTool(@Valid @RequestBody HttpToolRegisterRequest request) {
        log.info("注册HTTP工具: {}", request.getName());
        
        String toolId = request.getToolId() != null ? request.getToolId() : UUID.randomUUID().toString();
        Map<String, Object> config = buildHttpConfig(request);
        
        ToolDescriptor descriptor = toolService.registerHttpTool(
                toolId,
                request.getAppId(),
                request.getName(),
                request.getDescription(),
                config,
                request.getRetryTimes(),
                request.getTimeout() != null ? request.getTimeout().intValue() : null
        );
        
        return ResponseEntity.ok(descriptor);
    }

    /**
     * 注册数据库工具
     */
    @PostMapping("/db")
    @Operation(summary = "注册数据库工具", description = "动态注册一个数据库类型的工具，用于执行SQL查询")
    public ResponseEntity<ToolDescriptor> registerDbTool(@Valid @RequestBody DbToolRegisterRequest request) {
        log.info("注册数据库工具: {}", request.getName());
        
        String toolId = request.getToolId() != null ? request.getToolId() : UUID.randomUUID().toString();
        Map<String, Object> config = buildDbConfig(request);
        
        ToolDescriptor descriptor = toolService.registerDbTool(
                toolId,
                request.getAppId(),
                request.getName(),
                request.getDescription(),
                config,
                request.getRetryTimes(),
                request.getTimeout() != null ? request.getTimeout().intValue() : null
        );
        
        return ResponseEntity.ok(descriptor);
    }

    /**
     * 注册通用工具
     */
    @PostMapping
    @Operation(summary = "注册工具", description = "动态注册一个通用工具")
    public ResponseEntity<ToolDescriptor> registerTool(@Valid @RequestBody ToolDescriptor descriptor) {
        log.info("注册工具: {} (类型: {})", descriptor.getName(), descriptor.getExecutorType());
        ToolDescriptor registered = toolService.registerTool(descriptor);
        return ResponseEntity.ok(registered);
    }

    /**
     * 注册MCP工具（自动发现）
     */
    @PostMapping("/mcp/discover")
    @Operation(summary = "发现并注册MCP工具", description = "连接MCP服务器，自动发现工具并注册")
    public ResponseEntity<Map<String, Object>> discoverMcpTools(@Valid @RequestBody McpToolRegisterRequest request) {
        log.info("发现MCP工具: serverUrl={}, appId={}", request.getServerUrl(), request.getAppId());
        
        Map<String, Object> config = new HashMap<>();
        config.put("serverUrl", request.getServerUrl());
        config.put("transport", request.getTransport());
        config.put("headers", request.getHeaders());
        config.put("timeout", request.getTimeout());
        
        // 发现并注册工具
        List<String> toolIds = mcpProtocolAdapter.discoverAndRegister(
                request.getServerUrl(), 
                request.getAppId(), 
                config);
        
        Map<String, Object> result = new HashMap<>();
        result.put("serverUrl", request.getServerUrl());
        result.put("discoveredCount", toolIds.size());
        result.put("toolIds", toolIds);
        result.put("success", true);
        result.put("message", "成功发现并注册 " + toolIds.size() + " 个MCP工具");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 注册函数工具
     */
    @PostMapping("/function")
    @Operation(summary = "注册函数工具", description = "动态注册一个函数类型的工具，支持Spring Bean方法、脚本、反射调用")
    public ResponseEntity<ToolDescriptor> registerFunctionTool(@Valid @RequestBody FunctionToolRegisterRequest request) {
        log.info("注册函数工具: {} (类型: {})", request.getName(), request.getFunctionType());
        
        String toolId = UUID.randomUUID().toString();
        Map<String, Object> config = buildFunctionConfig(request);
        
        ToolDescriptor descriptor = toolService.registerFunctionTool(
                toolId,
                request.getAppId(),
                request.getName(),
                request.getDescription(),
                config,
                request.getAuthorizedApps(),
                request.getTimeout(),
                true
        );
        
        return ResponseEntity.ok(descriptor);
    }

    /**
     * 刷新MCP服务器工具
     */
    @PostMapping("/mcp/{serverUrl}/refresh")
    @Operation(summary = "刷新MCP工具", description = "刷新指定MCP服务器的工具列表")
    public ResponseEntity<Map<String, Object>> refreshMcpTools(
            @PathVariable("serverUrl") String serverUrl,
            @RequestParam(required = false) String appId) {
        log.info("刷新MCP工具: serverUrl={}", serverUrl);
        
        // URL解码
        String decodedUrl = java.net.URLDecoder.decode(serverUrl, java.nio.charset.StandardCharsets.UTF_8);
        
        List<String> toolIds = mcpProtocolAdapter.refresh(decodedUrl, appId, new HashMap<>());
        
        Map<String, Object> result = new HashMap<>();
        result.put("serverUrl", decodedUrl);
        result.put("toolCount", toolIds.size());
        result.put("toolIds", toolIds);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 注销MCP服务器工具
     */
    @DeleteMapping("/mcp/{serverUrl}")
    @Operation(summary = "注销MCP服务器工具", description = "注销指定MCP服务器的所有工具")
    public ResponseEntity<Void> unregisterMcpServer(@PathVariable("serverUrl") String serverUrl) {
        log.info("注销MCP服务器工具: serverUrl={}", serverUrl);
        
        String decodedUrl = java.net.URLDecoder.decode(serverUrl, java.nio.charset.StandardCharsets.UTF_8);
        mcpProtocolAdapter.unregister(decodedUrl);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取已注册的MCP服务器列表
     */
    @GetMapping("/mcp/servers")
    @Operation(summary = "获取MCP服务器列表", description = "获取所有已注册的MCP服务器")
    public ResponseEntity<List<Map<String, Object>>> getMcpServers() {
        List<Map<String, Object>> servers = new ArrayList<>();
        
        for (String serverUrl : mcpProtocolAdapter.getRegisteredServers()) {
            Map<String, Object> server = new HashMap<>();
            server.put("serverUrl", serverUrl);
            server.put("toolCount", mcpProtocolAdapter.getToolCount(serverUrl));
            servers.add(server);
        }
        
        return ResponseEntity.ok(servers);
    }

    /**
     * 获取已注册的执行器类型
     */
    @GetMapping("/executors")
    @Operation(summary = "获取执行器类型列表", description = "获取所有已注册的执行器类型")
    public ResponseEntity<List<String>> getExecutorTypes() {
        return ResponseEntity.ok(toolService.getExecutorTypes());
    }

    /**
     * 获取工具详情
     */
    @GetMapping("/{toolId}")
    @Operation(summary = "获取工具详情", description = "根据工具ID获取工具详细信息")
    public ResponseEntity<ToolDescriptor> getTool(@PathVariable("toolId") String toolId) {
        return toolService.getTool(toolId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有工具
     */
    @GetMapping
    @Operation(summary = "获取工具列表", description = "获取所有已注册的工具列表")
    public ResponseEntity<List<ToolDescriptor>> listTools() {
        List<ToolDescriptor> tools = toolService.getAllTools();
        return ResponseEntity.ok(tools);
    }

    /**
     * 根据类型查找工具
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型查找工具", description = "查找指定类型的工具")
    public ResponseEntity<List<ToolDescriptor>> findByType(@PathVariable("type") String type) {
        List<ToolDescriptor> tools = toolService.getToolsByType(type);
        return ResponseEntity.ok(tools);
    }

    /**
     * 根据应用ID查找工具
     */
    @GetMapping("/app/{appId}")
    @Operation(summary = "根据应用ID查找工具", description = "查找指定应用下的所有工具")
    public ResponseEntity<List<ToolDescriptor>> findByAppId(@PathVariable("appId") String appId) {
        List<ToolDescriptor> tools = toolService.getToolsByAppId(appId);
        return ResponseEntity.ok(tools);
    }

    /**
     * 注销工具
     */
    @DeleteMapping("/{toolId}")
    @Operation(summary = "注销工具", description = "注销指定的工具")
    public ResponseEntity<Void> unregisterTool(@PathVariable("toolId") String toolId) {
        log.info("注销工具: {}", toolId);
        toolService.deleteTool(toolId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 执行工具
     */
    @PostMapping("/{toolId}/invoke")
    @Operation(summary = "执行工具", description = "执行指定工具并返回结果")
    public ResponseEntity<ToolResult> invokeTool(
            @PathVariable("toolId") String toolId,
            @RequestBody Map<String, Object> params) {
        ToolResult result = toolService.invokeTool(toolId, params);
        return ResponseEntity.ok(result);
    }

    /**
     * 构建HTTP工具配置
     */
    private Map<String, Object> buildHttpConfig(HttpToolRegisterRequest request) {
        Map<String, Object> config = new HashMap<>();
        config.put("url", request.getUrl());
        config.put("method", request.getMethod() != null ? request.getMethod() : "GET");
        config.put("headers", request.getHeaders());
        config.put("authType", request.getAuthType());
        config.put("authConfig", request.getAuthConfig());
        config.put("responsePath", request.getResponsePath());
        if (request.getCapabilities() != null) {
            config.put("capabilities", request.getCapabilities());
        }
        if (request.getParameters() != null) {
            config.put("parameters", request.getParameters());
        }
        return config;
    }

    /**
     * 构建数据库工具配置
     */
    private Map<String, Object> buildDbConfig(DbToolRegisterRequest request) {
        Map<String, Object> config = new HashMap<>();
        config.put("datasourceId", request.getDatasourceId());
        config.put("sqlTemplate", request.getSqlTemplate());
        config.put("queryType", request.getQueryType() != null ? request.getQueryType() : "SELECT");
        if (request.getCapabilities() != null) {
            config.put("capabilities", request.getCapabilities());
        }
        if (request.getParameters() != null) {
            config.put("parameters", request.getParameters());
        }
        return config;
    }

    /**
     * 构建函数工具配置
     */
    private Map<String, Object> buildFunctionConfig(FunctionToolRegisterRequest request) {
        Map<String, Object> config = new HashMap<>();
        config.put("functionType", request.getFunctionType());
        
        // Bean方法调用配置
        if ("bean".equalsIgnoreCase(request.getFunctionType())) {
            config.put("beanName", request.getBeanName());
            config.put("methodName", request.getMethodName());
        }
        // 反射调用配置
        else if ("reflection".equalsIgnoreCase(request.getFunctionType())) {
            config.put("className", request.getClassName());
            config.put("methodName", request.getMethodName());
            config.put("staticMethod", Boolean.TRUE.equals(request.getStaticMethod()));
        }
        // 脚本执行配置
        else if ("script".equalsIgnoreCase(request.getFunctionType())) {
            config.put("scriptLanguage", request.getScriptLanguage());
            config.put("script", request.getScript());
        }
        
        // 参数映射
        if (request.getParamMapping() != null) {
            config.put("paramMapping", request.getParamMapping());
        }
        
        return config;
    }

    /**
     * 转换参数定义
     */
    private List<ToolDescriptor.ParamDefinition> convertParameters(List<FunctionToolRegisterRequest.ParamDefinition> params) {
        if (params == null) {
            return null;
        }
        return params.stream()
                .map(p -> ToolDescriptor.ParamDefinition.builder()
                        .name(p.getName())
                        .type(p.getType())
                        .description(p.getDescription())
                        .required(p.getRequired())
                        .defaultValue(p.getDefaultValue())
                        .options(p.getOptions())
                        .build())
                .toList();
    }
}
