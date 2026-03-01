package com.eazyai.ai.nexus.core.nlu;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于LLM的NLU引擎实现
 */
@Slf4j
@Component
public class LlmNluEngine implements NluEngine {

    @Autowired(required = false)
    private ChatLanguageModel chatModel;

    private static final String NLU_PROMPT = """
            分析以下用户输入，提取意图、实体和情感信息。
            
            用户输入：%s
            
            可用意图类型：%s
            
            请按以下JSON格式输出：
            {
              "intent": {
                "type": "意图类型",
                "confidence": 0.95,
                "description": "意图描述",
                "slots": {"提取的槽位": "值"}
              },
              "entities": [
                {"type": "实体类型", "value": "实体值", "startIndex": 0, "endIndex": 5, "confidence": 0.9}
              ],
              "sentiment": {
                "sentiment": "POSITIVE/NEGATIVE/NEUTRAL",
                "confidence": 0.9,
                "scores": {"POSITIVE": 0.1, "NEGATIVE": 0.1, "NEUTRAL": 0.8}
              },
              "normalizedInput": "规范化后的输入"
            }
            
            只输出JSON，不要其他文字。
            """;

    private static final List<String> DEFAULT_INTENTS = Arrays.asList(
            "chat", "query", "command", "question", "request", "feedback"
    );

    @Override
    public IntentResult recognizeIntent(String input, AgentContext context) {
        NluResult result = analyze(input, context);
        return result != null ? result.intent() : createDefaultIntent();
    }

    @Override
    public List<EntityResult> extractEntities(String input, AgentContext context) {
        NluResult result = analyze(input, context);
        return result != null ? result.entities() : Collections.emptyList();
    }

    @Override
    public SentimentResult analyzeSentiment(String input) {
        if (chatModel == null) {
            return new SentimentResult("NEUTRAL", 1.0, Map.of("NEUTRAL", 1.0));
        }

        try {
            String prompt = "分析以下文本的情感倾向（POSITIVE/NEGATIVE/NEUTRAL），只输出JSON格式：{\"sentiment\":\"...\",\"confidence\":0.9}\n\n文本：" + input;
            
            Response<AiMessage> response = chatModel.generate(
                    new SystemMessage("你是情感分析专家。"),
                    new UserMessage(prompt)
            );
            
            String content = response.content().text();
            Map<String, Object> result = JSON.parseObject(extractJson(content), new TypeReference<>() {});
            
            return new SentimentResult(
                    (String) result.getOrDefault("sentiment", "NEUTRAL"),
                    parseDouble(result.get("confidence")),
                    Map.of((String) result.getOrDefault("sentiment", "NEUTRAL"), 
                           parseDouble(result.get("confidence")))
            );
        } catch (Exception e) {
            log.warn("情感分析失败", e);
            return new SentimentResult("NEUTRAL", 1.0, Map.of("NEUTRAL", 1.0));
        }
    }

    @Override
    public NluResult analyze(String input, AgentContext context) {
        if (chatModel == null) {
            log.warn("ChatLanguageModel未配置，返回默认NLU结果");
            return createDefaultNluResult(input);
        }

        try {
            String prompt = String.format(NLU_PROMPT, input, String.join(", ", DEFAULT_INTENTS));
            
            Response<AiMessage> response = chatModel.generate(
                    new SystemMessage("你是NLU专家，擅长意图识别和实体提取。"),
                    new UserMessage(prompt)
            );
            
            String content = response.content().text();
            log.debug("NLU分析结果: {}", content);
            
            return parseNluResult(content, input);
        } catch (Exception e) {
            log.error("NLU分析失败", e);
            return createDefaultNluResult(input);
        }
    }

    private NluResult parseNluResult(String content, String input) {
        try {
            String json = extractJson(content);
            Map<String, Object> result = JSON.parseObject(json, new TypeReference<>() {});
            
            // 解析意图
            Map<String, Object> intentMap = (Map<String, Object>) result.getOrDefault("intent", new HashMap<>());
            IntentResult intent = new IntentResult(
                    (String) intentMap.getOrDefault("type", "chat"),
                    parseDouble(intentMap.get("confidence")),
                    (String) intentMap.getOrDefault("description", ""),
                    (Map<String, Object>) intentMap.getOrDefault("slots", new HashMap<>())
            );
            
            // 解析实体
            List<Map<String, Object>> entityList = (List<Map<String, Object>>) result.getOrDefault("entities", new ArrayList<>());
            List<EntityResult> entities = new ArrayList<>();
            for (Map<String, Object> e : entityList) {
                entities.add(new EntityResult(
                        (String) e.get("type"),
                        (String) e.get("value"),
                        e.get("startIndex") != null ? ((Number) e.get("startIndex")).intValue() : 0,
                        e.get("endIndex") != null ? ((Number) e.get("endIndex")).intValue() : 0,
                        parseDouble(e.get("confidence"))
                ));
            }
            
            // 解析情感
            Map<String, Object> sentimentMap = (Map<String, Object>) result.getOrDefault("sentiment", new HashMap<>());
            SentimentResult sentiment = new SentimentResult(
                    (String) sentimentMap.getOrDefault("sentiment", "NEUTRAL"),
                    parseDouble(sentimentMap.get("confidence")),
                    (Map<String, Double>) sentimentMap.getOrDefault("scores", new HashMap<>())
            );
            
            return new NluResult(
                    intent,
                    entities,
                    sentiment,
                    (String) result.getOrDefault("normalizedInput", input),
                    result
            );
        } catch (Exception e) {
            log.warn("解析NLU结果失败", e);
            return createDefaultNluResult(input);
        }
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        return start >= 0 && end > start ? content.substring(start, end + 1) : content;
    }

    private double parseDouble(Object value) {
        if (value == null) return 0.8;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.8;
        }
    }

    private IntentResult createDefaultIntent() {
        return new IntentResult("chat", 1.0, "默认对话意图", new HashMap<>());
    }

    private NluResult createDefaultNluResult(String input) {
        return new NluResult(
                createDefaultIntent(),
                Collections.emptyList(),
                new SentimentResult("NEUTRAL", 1.0, Map.of("NEUTRAL", 1.0)),
                input,
                new HashMap<>()
        );
    }
}
