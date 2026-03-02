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
        /**
         * 参数验证规则（如：min、max、pattern）
         */
        private Map<String, Object> validation;
        /**
         * 参数示例值
         */
        private Object example;
        /**
         * 参数可选值列表
         */
        private List<Object> options;
    }

    // ==================== 增强的工具指导信息 ====================

    /**
     * 触发条件
     * 描述何时应该使用此工具
     */
    private String triggerConditions;

    /**
     * 使用指导
     * 详细的使用说明和最佳实践
     */
    private String guidance;

    /**
     * 使用示例
     */
    @Builder.Default
    private List<UsageExample> examples = new ArrayList<>();

    /**
     * 前置工具
     * 使用此工具前应先调用的工具
     */
    @Builder.Default
    private List<String> preRequisiteTools = new ArrayList<>();

    /**
     * 后续工具
     * 调用此工具后通常会接着使用的工具
     */
    @Builder.Default
    private List<String> followUpTools = new ArrayList<>();

    /**
     * 错误处理建议
     */
    private String errorHandling;

    /**
     * 工具优先级（数值越大优先级越高）
     */
    @Builder.Default
    private int priority = 0;

    /**
     * 是否幂等（可重复调用）
     */
    @Builder.Default
    private boolean idempotent = true;

    /**
     * 估计执行时间（毫秒）
     */
    private Long estimatedDuration;

    /**
     * 使用示例
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageExample implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 示例场景描述
         */
        private String scenario;

        /**
         * 用户输入示例
         */
        private String userInput;

        /**
         * 工具调用参数
         */
        private Map<String, Object> toolArguments;

        /**
         * 预期输出
         */
        private String expectedOutput;

        /**
         * 使用说明
         */
        private String notes;
    }
}
