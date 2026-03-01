package com.eazyai.ai.nexus.core.nlu;

import com.eazyai.ai.nexus.api.dto.AgentContext;

import java.util.List;
import java.util.Map;

/**
 * NLU引擎接口
 * 自然语言理解引擎
 */
public interface NluEngine {

    /**
     * 识别意图
     *
     * @param input   用户输入
     * @param context 上下文
     * @return 意图识别结果
     */
    IntentResult recognizeIntent(String input, AgentContext context);

    /**
     * 提取实体
     *
     * @param input   用户输入
     * @param context 上下文
     * @return 实体提取结果
     */
    List<EntityResult> extractEntities(String input, AgentContext context);

    /**
     * 分析情感
     *
     * @param input 用户输入
     * @return 情感分析结果
     */
    SentimentResult analyzeSentiment(String input);

    /**
     * 综合分析
     *
     * @param input   用户输入
     * @param context 上下文
     * @return 综合分析结果
     */
    NluResult analyze(String input, AgentContext context);

    /**
     * 意图识别结果
     */
    record IntentResult(
        String intentType,
        double confidence,
        String description,
        Map<String, Object> slots
    ) {}

    /**
     * 实体提取结果
     */
    record EntityResult(
        String type,
        String value,
        int startIndex,
        int endIndex,
        double confidence
    ) {}

    /**
     * 情感分析结果
     */
    record SentimentResult(
        String sentiment, // POSITIVE, NEGATIVE, NEUTRAL
        double confidence,
        Map<String, Double> scores
    ) {}

    /**
     * NLU综合结果
     */
    record NluResult(
        IntentResult intent,
        List<EntityResult> entities,
        SentimentResult sentiment,
        String normalizedInput,
        Map<String, Object> metadata
    ) {}
}
