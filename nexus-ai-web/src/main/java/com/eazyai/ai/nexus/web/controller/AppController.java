package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.api.application.AppDescriptor;
import com.eazyai.ai.nexus.application.app.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

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
}
