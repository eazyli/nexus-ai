package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.api.application.AppDescriptor;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.api.dto.AppToolBindRequest;
import com.eazyai.ai.nexus.api.dto.AppToolResponse;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.application.app.AppService;
import com.eazyai.ai.nexus.application.app.AppToolService;
import com.eazyai.ai.nexus.core.engine.ReActEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用管理控制器
 * 提供应用的CRUD操作REST接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/apps")
@RequiredArgsConstructor
@Tag(name = "应用管理接口", description = "应用的注册、查询、更新、删除等操作")
public class AppController {

    private final AppService appService;
    private final AppToolService appToolService;
    private final ReActEngine reActEngine;

    /**
     * 创建应用
     */
    @PostMapping
    @Operation(summary = "创建应用", description = "注册一个新的AI应用")
    public ResponseEntity<AppDescriptor> createApp(@Valid @RequestBody AppDescriptor descriptor) {
        log.info("创建应用: {}", descriptor.getName());
        AppDescriptor created = appService.registerApp(descriptor);
        return ResponseEntity.ok(created);
    }

    /**
     * 获取应用详情
     */
    @GetMapping("/{appId}")
    @Operation(summary = "获取应用详情", description = "根据应用ID获取应用详细信息")
    public ResponseEntity<AppDescriptor> getApp(@PathVariable("appId") String appId) {
        return appService.getApp(appId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有应用
     */
    @GetMapping
    @Operation(summary = "获取应用列表", description = "获取所有已注册的应用列表")
    public ResponseEntity<List<AppDescriptor>> listApps() {
        List<AppDescriptor> apps = appService.getAllApps();
        return ResponseEntity.ok(apps);
    }

    /**
     * 按协作模式获取应用
     */
    @GetMapping("/mode/{mode}")
    @Operation(summary = "按协作模式获取应用", description = "根据协作模式筛选应用")
    public ResponseEntity<List<AppDescriptor>> getAppsByMode(@PathVariable("mode") String mode) {
        List<AppDescriptor> apps = appService.getAppsByCollaborationMode(mode);
        return ResponseEntity.ok(apps);
    }

    /**
     * 按能力获取应用
     */
    @GetMapping("/capability/{capability}")
    @Operation(summary = "按能力获取应用", description = "根据能力标签筛选应用")
    public ResponseEntity<List<AppDescriptor>> getAppsByCapability(@PathVariable("capability") String capability) {
        List<AppDescriptor> apps = appService.getAppsByCapability(capability);
        return ResponseEntity.ok(apps);
    }

    /**
     * 更新应用
     */
    @PutMapping("/{appId}")
    @Operation(summary = "更新应用", description = "更新指定应用的配置信息")
    public ResponseEntity<AppDescriptor> updateApp(
            @PathVariable("appId") String appId,
            @Valid @RequestBody AppDescriptor descriptor) {
        log.info("更新应用: {}", appId);
        AppDescriptor updated = appService.updateApp(appId, descriptor);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/{appId}")
    @Operation(summary = "删除应用", description = "删除指定的应用")
    public ResponseEntity<Void> deleteApp(@PathVariable("appId") String appId) {
        log.info("删除应用: {}", appId);
        appService.deleteApp(appId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用应用
     */
    @PostMapping("/{appId}/enable")
    @Operation(summary = "启用应用", description = "启用指定的应用")
    public ResponseEntity<Void> enableApp(@PathVariable("appId") String appId) {
        log.info("启用应用: {}", appId);
        appService.enableApp(appId);
        return ResponseEntity.ok().build();
    }

    /**
     * 禁用应用
     */
    @PostMapping("/{appId}/disable")
    @Operation(summary = "禁用应用", description = "禁用指定的应用")
    public ResponseEntity<Void> disableApp(@PathVariable("appId") String appId) {
        log.info("禁用应用: {}", appId);
        appService.disableApp(appId);
        return ResponseEntity.ok().build();
    }

    // ==================== 工具绑定相关 ====================

    /**
     * 获取应用可用工具列表
     */
    @GetMapping("/{appId}/tools/available")
    @Operation(summary = "获取可用工具列表", description = "获取应用可用的所有工具列表")
    public ResponseEntity<List<ToolDescriptor>> getAvailableTools(@PathVariable("appId") String appId) {
        List<ToolDescriptor> tools = appToolService.getAvailableTools(appId);
        return ResponseEntity.ok(tools);
    }

    /**
     * 获取应用已绑定的工具列表
     */
    @GetMapping("/{appId}/tools")
    @Operation(summary = "获取已绑定工具列表", description = "获取应用已绑定的工具列表")
    public ResponseEntity<List<AppToolResponse>> getBoundTools(@PathVariable("appId") String appId) {
        List<AppToolResponse> tools = appToolService.getBoundTools(appId);
        return ResponseEntity.ok(tools);
    }

    /**
     * 绑定工具到应用
     */
    @PostMapping("/{appId}/tools")
    @Operation(summary = "绑定工具", description = "绑定工具到应用")
    public ResponseEntity<Void> bindTools(
            @PathVariable("appId") String appId,
            @RequestBody AppToolBindRequest request) {
        log.info("绑定工具到应用: {} -> {}", request.getToolIds(), appId);
        appToolService.bindTools(appId, request.getToolIds());
        return ResponseEntity.ok().build();
    }

    /**
     * 解绑工具
     */
    @DeleteMapping("/{appId}/tools/{toolId}")
    @Operation(summary = "解绑工具", description = "从应用解绑工具")
    public ResponseEntity<Void> unbindTool(
            @PathVariable("appId") String appId,
            @PathVariable("toolId") String toolId) {
        log.info("解绑工具: {} <- {}", appId, toolId);
        appToolService.unbindTool(appId, toolId);
        return ResponseEntity.ok().build();
    }

    /**
     * 更新工具绑定（全量替换）
     */
    @PutMapping("/{appId}/tools")
    @Operation(summary = "更新工具绑定", description = "全量更新应用的工具绑定")
    public ResponseEntity<Void> updateToolBindings(
            @PathVariable("appId") String appId,
            @RequestBody AppToolBindRequest request) {
        log.info("更新工具绑定: {} -> {}", request.getToolIds(), appId);
        appToolService.updateToolBindings(appId, request.getToolIds());
        return ResponseEntity.ok().build();
    }

    // ==================== 交互配置相关 ====================

    /**
     * 获取开场白和示例问题
     */
    @GetMapping("/{appId}/interaction")
    @Operation(summary = "获取交互配置", description = "获取应用的开场白和示例问题")
    public ResponseEntity<AppDescriptor.AppConfig> getInteraction(@PathVariable("appId") String appId) {
        return appService.getAppConfig(appId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新开场白和示例问题
     */
    @PutMapping("/{appId}/interaction")
    @Operation(summary = "更新交互配置", description = "更新应用的开场白和示例问题")
    public ResponseEntity<Void> updateInteraction(
            @PathVariable("appId") String appId,
            @RequestBody InteractionUpdateRequest request) {
        log.info("更新交互配置: {}", appId);
        appService.updateInteraction(appId, request.getGreeting(), request.getSampleQuestions());
        return ResponseEntity.ok().build();
    }



    // ==================== 调试相关 ====================

    /**
     * 调试智能体
     */
    @PostMapping("/{appId}/debug")
    @Operation(summary = "调试智能体", description = "调试智能体配置")
    public ResponseEntity<AgentResponse> debugAgent(
            @PathVariable("appId") String appId,
            @RequestBody DebugRequest request) {
        log.info("调试智能体: {} - {}", appId, request.getQuery());
        
        AgentRequest agentRequest = AgentRequest.builder()
                .query(request.getQuery())
                .appId(appId)
                .maxIterations(request.getMaxIterations() != null ? request.getMaxIterations() : 5)
                .timeout(request.getTimeout() != null ? request.getTimeout() : 30000)
                .params(request.getVariables() != null ? request.getVariables() : new HashMap<>())
                .build();
        
        AgentResponse response = reActEngine.execute(agentRequest);
        return ResponseEntity.ok(response);
    }

    // ==================== 内部DTO ====================

    /**
     * 交互配置更新请求
     */
    @lombok.Data
    public static class InteractionUpdateRequest {
        private String greeting;
        private List<String> sampleQuestions;
    }

    /**
     * 调试请求
     */
    @lombok.Data
    public static class DebugRequest {
        private String query;
        private Map<String, Object> variables;
        private Integer maxIterations;
        private Long timeout;
    }
}
