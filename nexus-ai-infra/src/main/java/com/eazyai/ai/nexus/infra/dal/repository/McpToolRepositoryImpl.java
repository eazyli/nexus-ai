package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.api.tool.McpToolRepository;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import com.eazyai.ai.nexus.infra.dal.mapper.AiMcpToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MCP工具仓储实现
 * 
 * <p>实现 api 层定义的 McpToolRepository 接口</p>
 * <p>负责 ToolEntity 与 AiMcpTool Entity 之间的转换</p>
 */
@Repository
@RequiredArgsConstructor
public class McpToolRepositoryImpl implements McpToolRepository {

    private final AiMcpToolMapper mapper;

    @Override
    public Optional<ToolEntity> findById(String toolId) {
        return Optional.ofNullable(mapper.selectById(toolId))
                .map(this::toEntity);
    }

    @Override
    public List<ToolEntity> findByAppId(String appId) {
        LambdaQueryWrapper<AiMcpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpTool::getAppId, appId);
        return mapper.selectList(wrapper).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolEntity> findByToolType(String toolType) {
        LambdaQueryWrapper<AiMcpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpTool::getToolType, toolType);
        return mapper.selectList(wrapper).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolEntity> findAllEnabled() {
        LambdaQueryWrapper<AiMcpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpTool::getStatus, 1);
        return mapper.selectList(wrapper).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolEntity> findByIds(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(toolIds).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void save(ToolEntity tool) {
        AiMcpTool entity = toAiMcpTool(tool);
        entity.setCreateTime(java.time.LocalDateTime.now());
        mapper.insert(entity);
    }

    @Override
    public void update(ToolEntity tool) {
        AiMcpTool entity = toAiMcpTool(tool);
        entity.setUpdateTime(java.time.LocalDateTime.now());
        mapper.updateById(entity);
    }

    @Override
    public void deleteById(String toolId) {
        mapper.deleteById(toolId);
    }

    /**
     * AiMcpTool 转 ToolEntity
     */
    private ToolEntity toEntity(AiMcpTool tool) {
        if (tool == null) return null;
        return new ToolEntity(
                tool.getToolId(),
                tool.getToolName(),
                tool.getToolType(),
                tool.getDescription(),
                tool.getConfig(),
                tool.getAppId(),
                tool.getVisibility(),
                tool.getStatus(),
                tool.getPermissionApps(),
                tool.getRetryTimes(),
                tool.getRetryInterval(),
                tool.getTimeout(),
                tool.getCreateTime(),
                tool.getUpdateTime()
        );
    }

    /**
     * ToolEntity 转 AiMcpTool
     */
    private AiMcpTool toAiMcpTool(ToolEntity entity) {
        AiMcpTool tool = new AiMcpTool();
        tool.setToolId(entity.toolId());
        tool.setToolName(entity.toolName());
        tool.setToolType(entity.toolType());
        tool.setDescription(entity.description());
        tool.setConfig(entity.config());
        tool.setAppId(entity.appId());
        tool.setVisibility(entity.visibility());
        tool.setStatus(entity.status());
        tool.setPermissionApps(entity.permissionApps());
        tool.setRetryTimes(entity.retryTimes());
        tool.setRetryInterval(entity.retryInterval());
        tool.setTimeout(entity.timeout());
        tool.setCreateTime(entity.createTime());
        tool.setUpdateTime(entity.updateTime());
        return tool;
    }
}
