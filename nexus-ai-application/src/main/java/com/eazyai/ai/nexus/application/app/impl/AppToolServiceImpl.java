package com.eazyai.ai.nexus.application.app.impl;

import com.eazyai.ai.nexus.api.dto.AppToolResponse;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.application.app.AppToolService;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import com.eazyai.ai.nexus.infra.dal.repository.AiMcpToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用工具服务实现
 * 
 * <p>工具与应用的两种关联关系：</p>
 * <ul>
 *   <li>app_id - 工具的所属/创建者应用</li>
 *   <li>permission_apps - 可调用该工具的应用列表（逗号分隔）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppToolServiceImpl implements AppToolService {

    private final AiMcpToolRepository toolRepository;

    @Override
    public List<ToolDescriptor> getAvailableTools(String appId) {
        // 获取所有启用的工具
        List<AiMcpTool> allTools = toolRepository.findAllEnabled();
        
        return allTools.stream()
                .filter(tool -> isToolAvailableForApp(appId, tool))
                .map(this::toToolDescriptor)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppToolResponse> getBoundTools(String appId) {
        List<AiMcpTool> allTools = toolRepository.findAllEnabled();
        
        return allTools.stream()
                .filter(tool -> isToolBoundToApp(appId, tool))
                .map(tool -> AppToolResponse.builder()
                        .toolId(tool.getToolId())
                        .toolName(tool.getToolName())
                        .toolType(tool.getToolType())
                        .description(tool.getDescription())
                        .enabled(tool.getStatus() != null && tool.getStatus() == 1)
                        .bindTime(tool.getUpdateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void bindTools(String appId, List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return;
        }
        
        for (String toolId : toolIds) {
            AiMcpTool tool = toolRepository.findById(toolId)
                    .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + toolId));
            
            // 将应用添加到权限列表
            Set<String> allowedApps = parsePermissionApps(tool.getPermissionApps());
            
            if (!allowedApps.contains(appId)) {
                allowedApps.add(appId);
                tool.setPermissionApps(String.join(",", allowedApps));
                tool.setUpdateTime(LocalDateTime.now());
                toolRepository.updateById(tool);
                log.info("绑定工具 {} 到应用 {}", toolId, appId);
            }
        }
    }

    @Override
    @Transactional
    public void unbindTool(String appId, String toolId) {
        AiMcpTool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + toolId));
        
        // 从权限列表中移除应用
        Set<String> allowedApps = parsePermissionApps(tool.getPermissionApps());
        
        if (allowedApps.remove(appId)) {
            tool.setPermissionApps(allowedApps.isEmpty() ? null : String.join(",", allowedApps));
            tool.setUpdateTime(LocalDateTime.now());
            toolRepository.updateById(tool);
            log.info("解绑工具 {} 从应用 {}", toolId, appId);
        }
    }

    @Override
    @Transactional
    public void updateToolBindings(String appId, List<String> toolIds) {
        // 获取当前绑定的工具
        List<AppToolResponse> currentBindings = getBoundTools(appId);
        Set<String> currentToolIds = currentBindings.stream()
                .map(AppToolResponse::getToolId)
                .collect(Collectors.toSet());
        
        Set<String> newToolIds = new HashSet<>(toolIds != null ? toolIds : Collections.emptyList());
        
        // 需要解绑的工具
        Set<String> toUnbind = new HashSet<>(currentToolIds);
        toUnbind.removeAll(newToolIds);
        for (String toolId : toUnbind) {
            unbindTool(appId, toolId);
        }
        
        // 需要绑定的工具
        Set<String> toBind = new HashSet<>(newToolIds);
        toBind.removeAll(currentToolIds);
        bindTools(appId, new ArrayList<>(toBind));
    }

    @Override
    public boolean isToolAvailable(String appId, String toolId) {
        AiMcpTool tool = toolRepository.findById(toolId).orElse(null);
        if (tool == null || tool.getStatus() == null || tool.getStatus() != 1) {
            return false;
        }
        return isToolAvailableForApp(appId, tool);
    }

    /**
     * 检查工具是否对应用可用
     * 
     * <p>可用条件：</p>
     * <ul>
     *   <li>工具是全局工具（app_id为空，permission_apps为空）</li>
     *   <li>工具属于该应用（app_id = appId）</li>
     *   <li>应用在权限列表中（permission_apps包含appId）</li>
     * </ul>
     */
    private boolean isToolAvailableForApp(String appId, AiMcpTool tool) {
        // 工具禁用则不可用
        if (tool.getStatus() == null || tool.getStatus() != 1) {
            return false;
        }
        
        // 工具属于该应用
        if (appId != null && appId.equals(tool.getAppId())) {
            return true;
        }
        
        // 检查权限列表
        String permissionApps = tool.getPermissionApps();
        if (permissionApps == null || permissionApps.isEmpty()) {
            // 全局工具，所有应用可用
            return true;
        }
        
        Set<String> allowedApps = parsePermissionApps(permissionApps);
        return allowedApps.contains(appId);
    }

    /**
     * 检查工具是否已绑定到应用（有使用权限）
     */
    private boolean isToolBoundToApp(String appId, AiMcpTool tool) {
        // 工具属于该应用
        if (appId != null && appId.equals(tool.getAppId())) {
            return true;
        }
        
        // 检查权限列表
        String permissionApps = tool.getPermissionApps();
        if (permissionApps == null || permissionApps.isEmpty()) {
            // 全局工具
            return true;
        }
        
        Set<String> allowedApps = parsePermissionApps(permissionApps);
        return allowedApps.contains(appId);
    }

    /**
     * 解析权限应用列表
     */
    private Set<String> parsePermissionApps(String permissionApps) {
        if (permissionApps == null || permissionApps.isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(permissionApps.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 实体转描述符
     */
    private ToolDescriptor toToolDescriptor(AiMcpTool tool) {
        return ToolDescriptor.builder()
                .toolId(tool.getToolId())
                .appId(tool.getAppId())
                .name(tool.getToolName())
                .description(tool.getDescription())
                .executorType(tool.getToolType())
                .config(tool.getConfig())
                .retryTimes(tool.getRetryTimes())
                .timeout(tool.getTimeout() != null ? tool.getTimeout().longValue() : null)
                .enabled(tool.getStatus() != null && tool.getStatus() == 1)
                .build();
    }
}
