package com.eazyai.ai.nexus.api.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
