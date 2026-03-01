package com.eazyai.ai.nexus.api.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 场景描述符
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneDescriptor {

    /**
     * 场景ID
     */
    private String sceneId;

    /**
     * 场景名称
     */
    private String name;

    /**
     * 场景描述
     */
    private String description;

    /**
     * 所属应用ID
     */
    private String appId;

    /**
     * 场景类型 (chat, rag, tool_calling, workflow, multi_agent)
     */
    private String type;

    /**
     * 关联的能力ID列表
     */
    private List<String> capabilityIds;

    /**
     * 关联的工具ID列表
     */
    private List<String> toolIds;

    /**
     * 关联的知识库ID列表
     */
    private List<String> knowledgeBaseIds;

    /**
     * 场景配置
     */
    private SceneConfig config;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 场景配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneConfig {
        /**
         * 提示词模板
         */
        private String promptTemplate;

        /**
         * 意图识别配置
         */
        private NluConfig nlu;

        /**
         * RAG配置
         */
        private RagConfig rag;

        /**
         * Agent配置
         */
        private AgentConfig agent;

        /**
         * 输出配置
         */
        private OutputConfig output;

        /**
         * 扩展配置
         */
        private Map<String, Object> extra;
    }

    /**
     * NLU配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NluConfig {
        private Boolean enabled;
        private List<String> intentTypes;
        private List<String> entityTypes;
    }

    /**
     * RAG配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagConfig {
        private Boolean enabled;
        private Integer topK;
        private Double scoreThreshold;
        private String rerankModel;
    }

    /**
     * Agent配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentConfig {
        private String mode; // react, plan_and_execute, supervisor
        private Integer maxIterations;
        private Integer maxSteps;
    }

    /**
     * 输出配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputConfig {
        private String format; // text, json, markdown
        private String jsonSchema;
        private Boolean streaming;
    }
}
