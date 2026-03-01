package com.eazyai.ai.nexus.core.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP工具描述符
 */
@Data
@Builder
public class McpToolDescriptor {

    /**
     * 工具ID
     */
    private String toolId;

    /**
     * 所属应用ID
     */
    private String appId;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具类型 (http, db, dubbo, function, third_party)
     */
    private String type;

    /**
     * 工具能力标签
     */
    private List<String> capabilities;

    /**
     * 参数定义
     */
    private List<ParamDefinition> parameters;

    /**
     * 返回值定义
     */
    private ReturnDefinition returns;

    /**
     * 工具配置
     */
    private Map<String, Object> config;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 超时时间(毫秒)
     */
    private Long timeout;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParamDefinition {
        private String name;
        private String type;
        private String description;
        private Boolean required;
        private Object defaultValue;
        private List<String> options;
    }

    /**
     * 返回值定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnDefinition {
        private String type;
        private String description;
        private Map<String, Object> schema;
    }
}
