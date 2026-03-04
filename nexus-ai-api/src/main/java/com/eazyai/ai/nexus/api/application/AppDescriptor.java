package com.eazyai.ai.nexus.api.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 应用描述符
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDescriptor {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用名称
     */
    private String name;

    /**
     * 应用描述
     */
    private String description;

    /**
     * 应用类型 (chatbot, assistant, workflow, agent)
     */
    private String type;

    /**
     * 所属租户
     */
    private String tenantId;

    /**
     * 关联的场景ID列表
     */
    private List<String> sceneIds;

    /**
     * 应用配置
     */
    private AppConfig config;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 能力标签列表（用于多智能体匹配）
     */
    private List<String> capabilities;

    /**
     * 协作模式: single/react/plan_execute/supervisor
     */
    @Builder.Default
    private String collaborationMode = "single";

    /**
     * Agent执行配置
     */
    private Map<String, Object> executionConfig;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 图标URL
     */
    private String icon;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;

    /**
     * 应用配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppConfig {
        /**
         * 默认模型ID
         */
        private String defaultModelId;

        /**
         * 系统提示词
         */
        private String systemPrompt;

        /**
         * 温度参数
         */
        private Double temperature;

        /**
         * 最大token数
         */
        private Integer maxTokens;

        /**
         * 限流配置
         */
        private RateLimitConfig rateLimit;

        /**
         * 变量定义
         */
        private Map<String, VariableDefinition> variables;

        /**
         * 开场白（支持变量注入）
         */
        private String greeting;

        /**
         * 示例问题列表
         */
        private List<String> sampleQuestions;

        /**
         * 扩展配置
         */
        private Map<String, Object> extra;
    }

    /**
     * 限流配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitConfig {
        /**
         * 每秒请求数
         */
        private Integer requestsPerSecond;

        /**
         * 每日请求总数
         */
        private Integer requestsPerDay;

        /**
         * token配额
         */
        private Long tokenQuota;
    }

    /**
     * 变量定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableDefinition implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * 变量名
         */
        private String name;
        
        /**
         * 变量类型：string, number, boolean, object, array
         */
        @Builder.Default
        private String type = "string";
        
        /**
         * 默认值
         */
        private Object defaultValue;
        
        /**
         * 是否必填
         */
        @Builder.Default
        private Boolean required = false;
        
        /**
         * 描述
         */
        private String description;
        
        /**
         * 校验规则
         */
        private ValidationRule validation;
        
        /**
         * 校验规则
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ValidationRule implements Serializable {
            private static final long serialVersionUID = 1L;
            
            /**
             * 最小长度/值
             */
            private Integer minLength;
            
            /**
             * 最大长度/值
             */
            private Integer maxLength;
            
            /**
             * 正则表达式
             */
            private String pattern;
            
            /**
             * 枚举值
             */
            private List<String> enumValues;
        }
    }
}
