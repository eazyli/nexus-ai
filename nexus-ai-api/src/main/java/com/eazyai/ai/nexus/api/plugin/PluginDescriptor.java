package com.eazyai.ai.nexus.api.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件描述符
 * 描述插件的元数据信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 插件ID（唯一标识）
     */
    private String id;

    /**
     * 插件名称
     */
    private String name;

    /**
     * 插件版本
     */
    private String version;

    /**
     * 插件类型
     */
    private String type;

    /**
     * 插件描述
     */
    private String description;

    /**
     * 插件作者
     */
    private String author;

    /**
     * 提供的能力列表
     */
    @Builder.Default
    private List<String> capabilities = new ArrayList<>();

    /**
     * 参数定义
     */
    @Builder.Default
    private List<ParameterDef> parameters = new ArrayList<>();

    /**
     * 依赖的插件列表
     */
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();

    /**
     * 插件配置
     */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterDef implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String type;
        private String description;
        private boolean required;
        private Object defaultValue;
    }
}
