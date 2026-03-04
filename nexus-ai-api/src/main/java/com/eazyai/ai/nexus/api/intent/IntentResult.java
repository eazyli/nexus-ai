package com.eazyai.ai.nexus.api.intent;

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
 * 意图分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 意图类型
     */
    private String intentType;

    /**
     * 置信度 (0-1)
     */
    private double confidence;

    /**
     * 提取的实体
     */
    @Builder.Default
    private Map<String, Object> entities = new HashMap<>();

    /**
     * 情感倾向
     */
    private Sentiment sentiment;

    /**
     * 建议的任务类型
     */
    private String suggestedTaskType;

    /**
     * 是否明确
     */
    private boolean isAmbiguous;

    /**
     * 澄清问题（当意图不明确时）
     */
    private String clarificationQuestion;

    /**
     * 相关意图列表（用于歧义处理）
     */
    private List<String> relatedIntents;

    /**
     * 推荐的工具列表（按优先级排序）
     */
    @Builder.Default
    private List<ToolRecommendation> recommendedTools = new ArrayList<>();

    /**
     * 推理过程（LLM分析过程）
     */
    private String reasoning;

    /**
     * 原始分析结果
     */
    private Object rawResult;

    /**
     * 工具推荐
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolRecommendation implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 工具ID
         */
        private String toolId;

        /**
         * 工具名称
         */
        private String toolName;

        /**
         * 推荐理由
         */
        private String reason;

        /**
         * 匹配分数 (0-1)
         */
        private double score;

        /**
         * 历史成功率
         */
        private Double historicalSuccessRate;

        /**
         * 推荐的参数值
         */
        private Map<String, Object> suggestedParams;
    }

    /**
     * 情感枚举
     */
    public enum Sentiment {
        POSITIVE,   // 积极
        NEGATIVE,   // 消极
        NEUTRAL     // 中性
    }
}
