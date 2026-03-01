package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.api.application.SceneDescriptor;
import com.eazyai.ai.nexus.application.scene.SceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 场景管理控制器
 * 提供场景的CRUD操作REST接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scenes")
@RequiredArgsConstructor
@Tag(name = "场景管理接口", description = "场景的注册、查询、更新、删除等操作")
public class SceneController {

    private final SceneService sceneService;

    /**
     * 创建场景
     */
    @PostMapping
    @Operation(summary = "创建场景", description = "注册一个新的AI场景")
    public ResponseEntity<SceneDescriptor> createScene(@Valid @RequestBody SceneDescriptor descriptor) {
        log.info("创建场景: {}", descriptor.getName());
        SceneDescriptor created = sceneService.registerScene(descriptor);
        return ResponseEntity.ok(created);
    }

    /**
     * 获取场景详情
     */
    @GetMapping("/{sceneId}")
    @Operation(summary = "获取场景详情", description = "根据场景ID获取场景详细信息")
    public ResponseEntity<SceneDescriptor> getScene(@PathVariable String sceneId) {
        return sceneService.getScene(sceneId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有场景
     */
    @GetMapping
    @Operation(summary = "获取场景列表", description = "获取所有已注册的场景列表")
    public ResponseEntity<List<SceneDescriptor>> listScenes() {
        List<SceneDescriptor> scenes = sceneService.getAllScenes();
        return ResponseEntity.ok(scenes);
    }

    /**
     * 根据应用ID获取场景
     */
    @GetMapping("/app/{appId}")
    @Operation(summary = "根据应用获取场景", description = "获取指定应用下的所有场景")
    public ResponseEntity<List<SceneDescriptor>> getScenesByApp(@PathVariable String appId) {
        List<SceneDescriptor> scenes = sceneService.getScenesByApp(appId);
        return ResponseEntity.ok(scenes);
    }

    /**
     * 根据类型获取场景
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型获取场景", description = "获取指定类型的所有场景")
    public ResponseEntity<List<SceneDescriptor>> getScenesByType(@PathVariable String type) {
        List<SceneDescriptor> scenes = sceneService.getScenesByType(type);
        return ResponseEntity.ok(scenes);
    }

    /**
     * 更新场景
     */
    @PutMapping("/{sceneId}")
    @Operation(summary = "更新场景", description = "更新指定场景的配置信息")
    public ResponseEntity<SceneDescriptor> updateScene(
            @PathVariable String sceneId,
            @Valid @RequestBody SceneDescriptor descriptor) {
        log.info("更新场景: {}", sceneId);
        SceneDescriptor updated = sceneService.updateScene(sceneId, descriptor);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除场景
     */
    @DeleteMapping("/{sceneId}")
    @Operation(summary = "删除场景", description = "删除指定的场景")
    public ResponseEntity<Void> deleteScene(@PathVariable String sceneId) {
        log.info("删除场景: {}", sceneId);
        sceneService.deleteScene(sceneId);
        return ResponseEntity.noContent().build();
    }
}
