package com.eazyai.ai.nexus.api.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 能力描述符
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityDescriptor {

    /**
     * 能力ID
     */
    private String capabilityId;

    /**
     * 能力名称
     */
    private String name;

    /**
     * 能力描述
     */
    private String description;

    /**
     * 能力类型 (rag, tool_calling, nlu, generation, rule, memory)
     */
    private String type;

    /**
     * 能力参数定义
     */
    private List<ParamDefinition> params;

    /**
     * 能力配置
     */
    private Map<String, Object> config;

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
}
