package com.eazyai.ai.nexus.web.service;

import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.application.app.ToolService;
import com.eazyai.ai.nexus.web.dto.BatchToolImportRequest;
import com.eazyai.ai.nexus.web.dto.BatchToolImportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 工具导入服务
 * 
 * <p>依赖 application 层的 ToolService，不再直接依赖 infra 层</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolImportService {

    private final ToolService toolService;

    /**
     * 批量导入工具
     */
    public BatchToolImportResponse batchImport(BatchToolImportRequest request) {
        log.info("批量导入工具到应用: {}, 工具数量: {}", request.getTargetAppId(),
                request.getTools() != null ? request.getTools().size() : 0);

        List<String> importedToolIds = new ArrayList<>();
        List<BatchToolImportResponse.FailedTool> failedTools = new ArrayList<>();

        if (request.getTools() == null || request.getTools().isEmpty()) {
            return BatchToolImportResponse.builder()
                    .success(true)
                    .importedCount(0)
                    .failedCount(0)
                    .importedToolIds(importedToolIds)
                    .failedTools(failedTools)
                    .message("没有需要导入的工具")
                    .build();
        }

        for (BatchToolImportRequest.ToolDefinition toolDef : request.getTools()) {
            try {
                String toolId = importTool(request.getTargetAppId(), toolDef,
                        Boolean.TRUE.equals(request.getOverwrite()));
                importedToolIds.add(toolId);
                log.info("成功导入工具: {} -> {}", toolDef.getName(), toolId);
            } catch (Exception e) {
                log.error("导入工具失败: {}", toolDef.getName(), e);
                failedTools.add(BatchToolImportResponse.FailedTool.builder()
                        .name(toolDef.getName())
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BatchToolImportResponse.builder()
                .success(failedTools.isEmpty())
                .importedCount(importedToolIds.size())
                .failedCount(failedTools.size())
                .importedToolIds(importedToolIds)
                .failedTools(failedTools)
                .message(String.format("导入完成: 成功%d个, 失败%d个",
                        importedToolIds.size(), failedTools.size()))
                .build();
    }

    /**
     * 导入单个工具
     */
    private String importTool(String appId, BatchToolImportRequest.ToolDefinition toolDef, boolean overwrite) {
        String toolId = UUID.randomUUID().toString();
        String toolName = toolDef.getName();

        // 检查是否已存在同名工具
        List<ToolDescriptor> existingTools = toolService.getToolsByAppId(appId);
        Optional<ToolDescriptor> existing = existingTools.stream()
                .filter(t -> toolName.equals(t.getName()))
                .findFirst();

        if (existing.isPresent() && !overwrite) {
            throw new RuntimeException("工具已存在: " + toolName);
        }

        if (existing.isPresent() && overwrite) {
            // 删除旧工具
            toolService.deleteTool(existing.get().getToolId());
        }

        // 构建配置
        Map<String, Object> config = new HashMap<>();
        config.put("url", toolDef.getUrl());
        config.put("method", StringUtils.hasText(toolDef.getMethod()) ? toolDef.getMethod() : "GET");
        config.put("headers", toolDef.getHeaders());
        config.put("responsePath", toolDef.getResponsePath());
        if (toolDef.getParameters() != null) {
            config.put("parameters", toolDef.getParameters());
        }
        if (toolDef.getCapabilities() != null) {
            config.put("capabilities", toolDef.getCapabilities());
        }

        // 注册工具
        ToolDescriptor descriptor = toolService.registerHttpTool(
                toolId,
                appId,
                toolName,
                toolDef.getDescription(),
                config,
                null,
                null
        );

        return descriptor.getToolId();
    }
}
