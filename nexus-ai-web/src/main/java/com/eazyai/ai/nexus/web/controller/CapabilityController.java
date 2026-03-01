package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.api.application.CapabilityDescriptor;
import com.eazyai.ai.nexus.application.capability.CapabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 能力管理控制器
 * 提供AI能力的CRUD操作REST接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/capabilities")
@RequiredArgsConstructor
@Tag(name = "能力管理接口", description = "AI能力的注册、查询、更新、删除等操作")
public class CapabilityController {

    private final CapabilityService capabilityService;

    /**
     * 创建能力
     */
    @PostMapping
    @Operation(summary = "创建能力", description = "注册一个新的AI能力")
    public ResponseEntity<CapabilityDescriptor> createCapability(@Valid @RequestBody CapabilityDescriptor descriptor) {
        log.info("创建能力: {}", descriptor.getName());
        CapabilityDescriptor created = capabilityService.registerCapability(descriptor);
        return ResponseEntity.ok(created);
    }

    /**
     * 获取能力详情
     */
    @GetMapping("/{capabilityId}")
    @Operation(summary = "获取能力详情", description = "根据能力ID获取能力详细信息")
    public ResponseEntity<CapabilityDescriptor> getCapability(@PathVariable String capabilityId) {
        return capabilityService.getCapability(capabilityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有能力
     */
    @GetMapping
    @Operation(summary = "获取能力列表", description = "获取所有已注册的能力列表")
    public ResponseEntity<List<CapabilityDescriptor>> listCapabilities() {
        List<CapabilityDescriptor> capabilities = capabilityService.getAllCapabilities();
        return ResponseEntity.ok(capabilities);
    }

    /**
     * 根据类型获取能力
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型获取能力", description = "获取指定类型的所有能力")
    public ResponseEntity<List<CapabilityDescriptor>> getCapabilitiesByType(@PathVariable String type) {
        List<CapabilityDescriptor> capabilities = capabilityService.getCapabilitiesByType(type);
        return ResponseEntity.ok(capabilities);
    }

    /**
     * 更新能力
     */
    @PutMapping("/{capabilityId}")
    @Operation(summary = "更新能力", description = "更新指定能力的配置信息")
    public ResponseEntity<CapabilityDescriptor> updateCapability(
            @PathVariable String capabilityId,
            @Valid @RequestBody CapabilityDescriptor descriptor) {
        log.info("更新能力: {}", capabilityId);
        CapabilityDescriptor updated = capabilityService.updateCapability(capabilityId, descriptor);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除能力
     */
    @DeleteMapping("/{capabilityId}")
    @Operation(summary = "删除能力", description = "删除指定的能力")
    public ResponseEntity<Void> deleteCapability(@PathVariable String capabilityId) {
        log.info("删除能力: {}", capabilityId);
        capabilityService.deleteCapability(capabilityId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用能力
     */
    @PostMapping("/{capabilityId}/enable")
    @Operation(summary = "启用能力", description = "启用指定的能力")
    public ResponseEntity<Void> enableCapability(@PathVariable String capabilityId) {
        log.info("启用能力: {}", capabilityId);
        capabilityService.enableCapability(capabilityId);
        return ResponseEntity.ok().build();
    }

    /**
     * 禁用能力
     */
    @PostMapping("/{capabilityId}/disable")
    @Operation(summary = "禁用能力", description = "禁用指定的能力")
    public ResponseEntity<Void> disableCapability(@PathVariable String capabilityId) {
        log.info("禁用能力: {}", capabilityId);
        capabilityService.disableCapability(capabilityId);
        return ResponseEntity.ok().build();
    }
}
