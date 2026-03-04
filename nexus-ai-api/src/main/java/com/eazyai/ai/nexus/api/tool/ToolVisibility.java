package com.eazyai.ai.nexus.api.tool;

/**
 * 工具可见性枚举
 * 定义工具的访问范围
 */
public enum ToolVisibility {
    
    /**
     * 应用专属 - 仅所属应用可访问
     */
    PRIVATE,
    
    /**
     * 全局公共 - 所有应用均可访问
     */
    PUBLIC,
    
    /**
     * 指定共享 - 仅授权应用列表中的应用可访问
     */
    SHARED
}
