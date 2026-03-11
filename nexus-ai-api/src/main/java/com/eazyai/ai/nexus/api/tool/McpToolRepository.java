package com.eazyai.ai.nexus.api.tool;

import java.util.List;
import java.util.Optional;

/**
 * MCP工具仓储接口
 * 
 * <p>定义MCP工具数据访问的抽象接口</p>
 * <p>由 infra 层实现具体的存储访问</p>
 */
public interface McpToolRepository {

    /**
     * 工具实体（简化版，不依赖MyBatis）
     */
    record ToolEntity(
            String toolId,
            String toolName,
            String toolType,
            String description,
            java.util.Map<String, Object> config,
            String appId,
            String visibility,
            Integer status,
            String permissionApps,
            Integer retryTimes,
            Integer retryInterval,
            Integer timeout,
            java.time.LocalDateTime createTime,
            java.time.LocalDateTime updateTime
    ) {}

    /**
     * 根据ID查找工具
     */
    Optional<ToolEntity> findById(String toolId);

    /**
     * 根据应用ID查找工具列表
     */
    List<ToolEntity> findByAppId(String appId);

    /**
     * 根据工具类型查找工具列表
     */
    List<ToolEntity> findByToolType(String toolType);

    /**
     * 查找所有启用的工具
     */
    List<ToolEntity> findAllEnabled();

    /**
     * 根据ID列表查找工具
     */
    List<ToolEntity> findByIds(List<String> toolIds);

    /**
     * 保存工具
     */
    void save(ToolEntity tool);

    /**
     * 更新工具
     */
    void update(ToolEntity tool);

    /**
     * 删除工具
     */
    void deleteById(String toolId);
}
