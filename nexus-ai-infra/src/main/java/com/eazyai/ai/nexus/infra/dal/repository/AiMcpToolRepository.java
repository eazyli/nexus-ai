package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import com.eazyai.ai.nexus.infra.dal.mapper.AiMcpToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MCP工具 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiMcpToolRepository {

    private final AiMcpToolMapper mapper;

    public Optional<AiMcpTool> findById(String toolId) {
        return Optional.ofNullable(mapper.selectById(toolId));
    }

    public List<AiMcpTool> findByAppId(String appId) {
        LambdaQueryWrapper<AiMcpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpTool::getAppId, appId);
        return mapper.selectList(wrapper);
    }

    public List<AiMcpTool> findByToolType(String toolType) {
        LambdaQueryWrapper<AiMcpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpTool::getToolType, toolType);
        return mapper.selectList(wrapper);
    }

    public List<AiMcpTool> findAllEnabled() {
        LambdaQueryWrapper<AiMcpTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMcpTool::getStatus, 1);
        return mapper.selectList(wrapper);
    }

    public List<AiMcpTool> findByIds(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(toolIds);
    }

    public int insert(AiMcpTool tool) {
        return mapper.insert(tool);
    }

    public int updateById(AiMcpTool tool) {
        return mapper.updateById(tool);
    }

    public int deleteById(String toolId) {
        return mapper.deleteById(toolId);
    }
}
