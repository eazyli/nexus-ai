package com.eazyai.ai.nexus.api.intent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
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
     * 原始分析结果
     */
    private Object rawResult;

    /**
     * 情感枚举
     */
    public enum Sentiment {
        POSITIVE,   // 积极
        NEGATIVE,   // 消极
        NEUTRAL     // 中性
    }
}
