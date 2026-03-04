package com.eazyai.ai.nexus.application.app;

import com.eazyai.ai.nexus.api.dto.AppToolResponse;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;

import java.util.List;

/**
 * 应用工具服务
 */
public interface AppToolService {

    /**
     * 获取应用可用工具列表（全局工具 + 应用专属工具）
     * 
     * @param appId 应用ID
     * @return 可用工具列表
     */
    List<ToolDescriptor> getAvailableTools(String appId);

    /**
     * 获取应用已绑定的工具
     * 
     * @param appId 应用ID
     * @return 已绑定工具列表
     */
    List<AppToolResponse> getBoundTools(String appId);

    /**
     * 绑定工具到应用
     * 
     * @param appId 应用ID
     * @param toolIds 工具ID列表
     */
    void bindTools(String appId, List<String> toolIds);

    /**
     * 解绑工具
     * 
     * @param appId 应用ID
     * @param toolId 工具ID
     */
    void unbindTool(String appId, String toolId);

    /**
     * 更新工具绑定（全量替换）
     * 
     * @param appId 应用ID
     * @param toolIds 工具ID列表
     */
    void updateToolBindings(String appId, List<String> toolIds);

    /**
     * 检查工具是否对应用可用
     * 
     * @param appId 应用ID
     * @param toolId 工具ID
     * @return 是否可用
     */
    boolean isToolAvailable(String appId, String toolId);
}
