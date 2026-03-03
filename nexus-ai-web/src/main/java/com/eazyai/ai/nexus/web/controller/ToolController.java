package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.infra.converter.ToolConverter;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import com.eazyai.ai.nexus.infra.dal.repository.AiMcpToolRepository;
import com.eazyai.ai.nexus.web.dto.DbToolRegisterRequest;
import com.eazyai.ai.nexus.web.dto.HttpToolRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具管理控制器
 * 提供动态工具注册和管理REST接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
@Tag(name = "工具管理接口", description = "动态工具的注册、查询、执行等操作")
public class ToolController {

    private final ToolBus toolBus;
    private final AiMcpToolRepository aiMcpToolRepository;
    private final ToolConverter toolConverter;

    /**
     * 注册HTTP工具
     */
    @PostMapping("/http")
    @Operation(summary = "注册HTTP工具", description = "动态注册一个HTTP类型的工具，用于调用外部REST API")
    public ResponseEntity<ToolDescriptor> registerHttpTool(@Valid @RequestBody HttpToolRegisterRequest request) {
        log.info("注册HTTP工具: {}", request.getName());
        
        String toolId = request.getToolId() != null ? request.getToolId() : UUID.randomUUID().toString();
        Map<String, Object> config = buildHttpConfig(request);
        
        // 保存到数据库
        AiMcpTool entity = new AiMcpTool();
        entity.setToolId(toolId);
        entity.setAppId(request.getAppId());
        entity.setToolName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setToolType("HTTP");
        entity.setConfig(config);
        entity.setRetryTimes(request.getRetryTimes());
        entity.setTimeout(request.getTimeout() != null ? request.getTimeout().intValue() : null);
        entity.setStatus(1);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        aiMcpToolRepository.insert(entity);
        
        // 注册到内存
        ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
        toolBus.registerTool(descriptor);
        
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
        
        // 保存到数据库
        AiMcpTool entity = new AiMcpTool();
        entity.setToolId(toolId);
        entity.setAppId(request.getAppId());
        entity.setToolName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setToolType("DB");
        entity.setConfig(config);
        entity.setRetryTimes(request.getRetryTimes());
        entity.setTimeout(request.getTimeout() != null ? request.getTimeout().intValue() : null);
        entity.setStatus(1);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        aiMcpToolRepository.insert(entity);
        
        // 注册到内存
        ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
        toolBus.registerTool(descriptor);
        
        return ResponseEntity.ok(descriptor);
    }

    /**
     * 注册通用工具
     */
    @PostMapping
    @Operation(summary = "注册工具", description = "动态注册一个通用工具")
    public ResponseEntity<ToolDescriptor> registerTool(@Valid @RequestBody ToolDescriptor descriptor) {
        log.info("注册工具: {} (类型: {})", descriptor.getName(), descriptor.getExecutorType());
        
        if (descriptor.getToolId() == null) {
            descriptor.setToolId(UUID.randomUUID().toString());
        }
        if (descriptor.getEnabled() == null) {
            descriptor.setEnabled(true);
        }
        
        // 保存到数据库
        AiMcpTool entity = toolConverter.toEntity(descriptor);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        aiMcpToolRepository.insert(entity);
        
        // 注册到内存
        toolBus.registerTool(descriptor);
        return ResponseEntity.ok(descriptor);
    }

    /**
     * 获取工具详情
     */
    @GetMapping("/{toolId}")
    @Operation(summary = "获取工具详情", description = "根据工具ID获取工具详细信息")
    public ResponseEntity<ToolDescriptor> getTool(@PathVariable("toolId") String toolId) {
        return aiMcpToolRepository.findById(toolId)
                .map(toolConverter::toDescriptor)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有工具
     */
    @GetMapping
    @Operation(summary = "获取工具列表", description = "获取所有已注册的工具列表")
    public ResponseEntity<List<ToolDescriptor>> listTools() {
        List<ToolDescriptor> tools = toolConverter.toDescriptorList(aiMcpToolRepository.findAllEnabled());
        return ResponseEntity.ok(tools);
    }

    /**
     * 根据类型查找工具
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型查找工具", description = "查找指定类型的工具")
    public ResponseEntity<List<ToolDescriptor>> findByType(@PathVariable("type") String type) {
        List<ToolDescriptor> tools = aiMcpToolRepository.findByToolType(type.toUpperCase()).stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                .map(toolConverter::toDescriptor)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tools);
    }

    /**
     * 根据应用ID查找工具
     */
    @GetMapping("/app/{appId}")
    @Operation(summary = "根据应用ID查找工具", description = "查找指定应用下的所有工具")
    public ResponseEntity<List<ToolDescriptor>> findByAppId(@PathVariable("appId") String appId) {
        List<ToolDescriptor> tools = aiMcpToolRepository.findByAppId(appId).stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                .map(toolConverter::toDescriptor)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tools);
    }

    /**
     * 注销工具
     */
    @DeleteMapping("/{toolId}")
    @Operation(summary = "注销工具", description = "注销指定的工具")
    public ResponseEntity<Void> unregisterTool(@PathVariable("toolId") String toolId) {
        log.info("注销工具: {}", toolId);
        
        // 从数据库删除
        aiMcpToolRepository.deleteById(toolId);
        // 从内存注销
        toolBus.unregisterTool(toolId);
        
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
        log.info("执行工具: {} - params: {}", toolId, params);
        ToolResult result = toolBus.invoke(toolId, params, null);
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
}
