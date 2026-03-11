package com.eazyai.ai.nexus.application.app;

import com.eazyai.ai.nexus.api.dto.AppToolResponse;
import com.eazyai.ai.nexus.api.tool.McpToolRepository;
import com.eazyai.ai.nexus.api.tool.McpToolRepository.ToolEntity;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.eazyai.ai.nexus.api.tool.ToolVisibility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具管理服务
 * 
 * <p>应用层服务，负责工具的业务编排</p>
 * <p>依赖 api 层定义的 Repository 接口，由 infra 层实现</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolService {

    private final McpToolRepository toolRepository;
    private final ToolBus toolBus;

    // ==================== 查询操作 ====================

    /**
     * 根据ID获取工具
     */
    public Optional<ToolDescriptor> getTool(String toolId) {
        return toolRepository.findById(toolId)
                .map(this::toDescriptor);
    }

    /**
     * 获取所有启用的工具
     */
    public List<ToolDescriptor> getAllTools() {
        return toolRepository.findAllEnabled().stream()
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取工具
     */
    public List<ToolDescriptor> getToolsByType(String type) {
        return toolRepository.findByToolType(type.toUpperCase()).stream()
                .filter(t -> t.status() != null && t.status() == 1)
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 根据应用ID获取工具
     */
    public List<ToolDescriptor> getToolsByAppId(String appId) {
        return toolRepository.findByAppId(appId).stream()
                .filter(t -> t.status() != null && t.status() == 1)
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    // ==================== 注册操作 ====================

    /**
     * 注册工具
     */
    @Transactional
    public ToolDescriptor registerTool(ToolDescriptor descriptor) {
        if (descriptor.getToolId() == null) {
            descriptor.setToolId(UUID.randomUUID().toString());
        }
        if (descriptor.getEnabled() == null) {
            descriptor.setEnabled(true);
        }

        ToolEntity entity = toEntity(descriptor, LocalDateTime.now(), LocalDateTime.now());
        toolRepository.save(entity);

        toolBus.registerTool(descriptor);
        log.info("注册工具: {} ({})", descriptor.getName(), descriptor.getToolId());

        return descriptor;
    }

    /**
     * 注册HTTP工具
     */
    @Transactional
    public ToolDescriptor registerHttpTool(String toolId, String appId, String name, String description,
                                           Map<String, Object> config, Integer retryTimes, Integer timeout) {
        String id = toolId != null ? toolId : UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        ToolEntity entity = new ToolEntity(
                id, name, "HTTP", description, config, appId,
                "PRIVATE", 1, null, retryTimes, null, timeout, now, now
        );
        toolRepository.save(entity);

        ToolDescriptor descriptor = toDescriptor(entity);
        toolBus.registerTool(descriptor);
        log.info("注册HTTP工具: {} ({})", name, id);

        return descriptor;
    }

    /**
     * 注册数据库工具
     */
    @Transactional
    public ToolDescriptor registerDbTool(String toolId, String appId, String name, String description,
                                         Map<String, Object> config, Integer retryTimes, Integer timeout) {
        String id = toolId != null ? toolId : UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        ToolEntity entity = new ToolEntity(
                id, name, "DB", description, config, appId,
                "PRIVATE", 1, null, retryTimes, null, timeout, now, now
        );
        toolRepository.save(entity);

        ToolDescriptor descriptor = toDescriptor(entity);
        toolBus.registerTool(descriptor);
        log.info("注册DB工具: {} ({})", name, id);

        return descriptor;
    }

    /**
     * 注册函数工具
     */
    @Transactional
    public ToolDescriptor registerFunctionTool(String toolId, String appId, String name, String description,
                                               Map<String, Object> config, List<String> authorizedApps,
                                               Long timeout, boolean enabled) {
        String id = toolId != null ? toolId : UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        ToolEntity entity = new ToolEntity(
                id, name, "FUNCTION", description, config, appId,
                "PRIVATE", enabled ? 1 : 0, authorizedApps != null ? String.join(",", authorizedApps) : null,
                null, null, timeout != null ? timeout.intValue() : null, now, now
        );
        toolRepository.save(entity);

        ToolDescriptor descriptor = toDescriptor(entity);
        toolBus.registerTool(descriptor);
        log.info("注册函数工具: {} ({})", name, id);

        return descriptor;
    }

    /**
     * 注册流程工具
     */
    @Transactional
    public ToolDescriptor registerFlowTool(String toolId, String appId, String name, String description,
                                           Map<String, Object> config, Integer retryTimes, Long timeout, boolean enabled) {
        String id = toolId != null ? toolId : "flow-" + UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        ToolEntity entity = new ToolEntity(
                id, name, "FLOW", description, config, appId,
                "PRIVATE", enabled ? 1 : 0, null,
                retryTimes, null, timeout != null ? timeout.intValue() : null, now, now
        );
        toolRepository.save(entity);

        ToolDescriptor descriptor = toDescriptor(entity);
        toolBus.registerTool(descriptor);
        log.info("注册流程工具: {} ({})", name, id);

        return descriptor;
    }

    // ==================== 更新操作 ====================

    /**
     * 更新工具
     */
    @Transactional
    public ToolDescriptor updateTool(String toolId, String name, String description, 
                                     Map<String, Object> config) {
        ToolEntity existing = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + toolId));

        ToolEntity updated = new ToolEntity(
                existing.toolId(),
                name != null ? name : existing.toolName(),
                existing.toolType(),
                description != null ? description : existing.description(),
                config != null ? config : existing.config(),
                existing.appId(),
                existing.visibility(),
                existing.status(),
                existing.permissionApps(),
                existing.retryTimes(),
                existing.retryInterval(),
                existing.timeout(),
                existing.createTime(),
                LocalDateTime.now()
        );
        toolRepository.update(updated);

        ToolDescriptor descriptor = toDescriptor(updated);
        toolBus.registerTool(descriptor);
        log.info("更新工具: {}", toolId);

        return descriptor;
    }

    // ==================== 删除操作 ====================

    /**
     * 删除工具
     */
    @Transactional
    public void deleteTool(String toolId) {
        toolRepository.deleteById(toolId);
        toolBus.unregisterTool(toolId);
        log.info("删除工具: {}", toolId);
    }

    // ==================== 执行操作 ====================

    /**
     * 执行工具
     */
    public ToolResult invokeTool(String toolId, Map<String, Object> params) {
        log.info("执行工具: {} - params: {}", toolId, params);
        return toolBus.invoke(toolId, params, null);
    }

    /**
     * 获取已注册的执行器类型
     */
    public List<String> getExecutorTypes() {
        return toolBus.getRegisteredExecutorTypes();
    }

    // ==================== 转换方法 ====================

    /**
     * ToolEntity 转 ToolDescriptor
     */
    private ToolDescriptor toDescriptor(ToolEntity entity) {
        if (entity == null) return null;
        return ToolDescriptor.builder()
                .toolId(entity.toolId())
                .appId(entity.appId())
                .name(entity.toolName())
                .description(entity.description())
                .executorType(entity.toolType().toLowerCase())
                .config(entity.config())
                .retryTimes(entity.retryTimes())
                .timeout(entity.timeout() != null ? entity.timeout().longValue() : null)
                .enabled(entity.status() != null && entity.status() == 1)
                .build();
    }

    /**
     * ToolDescriptor 转 ToolEntity
     */
    private ToolEntity toEntity(ToolDescriptor descriptor, LocalDateTime createTime, LocalDateTime updateTime) {
        return new ToolEntity(
                descriptor.getToolId(),
                descriptor.getName(),
                descriptor.getExecutorType() != null ? descriptor.getExecutorType().toUpperCase() : "HTTP",
                descriptor.getDescription(),
                descriptor.getConfig(),
                descriptor.getAppId(),
                descriptor.getVisibility() != null ? descriptor.getVisibility().name() : "PRIVATE",
                descriptor.getEnabled() != null && descriptor.getEnabled() ? 1 : 0,
                null,
                descriptor.getRetryTimes(),
                null,
                descriptor.getTimeout() != null ? descriptor.getTimeout().intValue() : null,
                createTime,
                updateTime
        );
    }
}
