package com.eazyai.ai.nexus.core.intent;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.intent.IntentAnalyzer;
import com.eazyai.ai.nexus.api.intent.IntentResult;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
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
import java.util.stream.Collectors;

/**
 * LLM意图分析器
 *
 * <p>优化点：</p>
 * <ul>
 *   <li>结合可用插件能力进行分析</li>
 *   <li>提取更精确的实体信息</li>
 *   <li>输出结构化JSON便于后续路由</li>
 * </ul>
 */
@Slf4j
@Component
public class LlmIntentAnalyzer implements IntentAnalyzer {

    @Autowired(required = false)
    private ChatLanguageModel chatModel;

    @Autowired
    private PluginRegistry pluginRegistry;

    private static final String INTENT_PROMPT = """
            分析用户输入，提取关键信息。

            ## 用户输入
            %s

            ## 可用工具能力
            %s

            ## 输出格式（JSON）
            {
              "intent_type": "意图类型",
              "confidence": 0.95,
              "entities": {
                "提取的关键实体": "对应值"
              },
              "suggested_plugins": ["可能用到的插件ID"],
              "reasoning": "简要分析原因",
              "sentiment": "POSITIVE/NEGATIVE/NEUTRAL",
              "is_complex": false
            }

            要求：
            1. entities提取用户输入中的关键信息（如时间、地点、数量等）
            2. suggested_plugins根据用户需求推测可能需要的插件
            3. is_complex判断是否需要多步骤处理
            4. 只返回JSON，不要其他文字
            """;

    @Override
    public IntentResult analyze(AgentRequest request, AgentContext context) {
        if (chatModel == null) {
            log.warn("ChatLanguageModel不可用，返回默认意图");
            return createDefaultResult(request);
        }

        try {
            // 获取可用插件描述
            String capabilities = getPluginCapabilities();

            // 构建提示词
            String prompt = String.format(INTENT_PROMPT, request.getQuery(), capabilities);

            // 调用LLM
            Response<AiMessage> response = chatModel.generate(
                    new SystemMessage("你是意图分析专家，擅长理解用户需求并提取关键信息。"),
                    new UserMessage(prompt)
            );

            String content = response.content().text();
            log.debug("LLM意图分析响应: {}", content);

            // 解析结果
            return parseResult(content, request);

        } catch (Exception e) {
            log.error("意图分析失败", e);
            return createDefaultResult(request);
        }
    }

    /**
     * 获取插件能力描述
     */
    private String getPluginCapabilities() {
        List<PluginDescriptor> plugins = pluginRegistry.getAllPlugins();
        if (plugins.isEmpty()) {
            return "无可用工具";
        }

        return plugins.stream()
                .filter(PluginDescriptor::isEnabled)
                .map(p -> String.format("%s: %s", p.getId(), p.getDescription()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 解析LLM返回的JSON
     */
    private IntentResult parseResult(String content, AgentRequest request) {
        try {
            // 提取JSON部分
            String json = extractJson(content);
            Map<String, Object> result = JSON.parseObject(json, new TypeReference<>() {});

            return IntentResult.builder()
                    .intentType((String) result.getOrDefault("intent_type", "general"))
                    .confidence(parseConfidence(result.get("confidence")))
                    .entities((Map<String, Object>) result.getOrDefault("entities", new HashMap<>()))
                    .sentiment(parseSentiment((String) result.get("sentiment")))
                    .suggestedTaskType(String.join(",", 
                            (List<String>) result.getOrDefault("suggested_plugins", Collections.emptyList())))
                    .isAmbiguous((Boolean) result.getOrDefault("is_complex", false))
                    .rawResult(result)
                    .build();

        } catch (Exception e) {
            log.warn("解析意图JSON失败，使用默认值: {}", e.getMessage());
            return createDefaultResult(request);
        }
    }

    /**
     * 提取JSON内容
     */
    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    /**
     * 解析置信度
     */
    private double parseConfidence(Object value) {
        if (value == null) return 0.8;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.8;
        }
    }

    /**
     * 解析情感
     */
    private IntentResult.Sentiment parseSentiment(String value) {
        if (value == null) return IntentResult.Sentiment.NEUTRAL;
        try {
            return IntentResult.Sentiment.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return IntentResult.Sentiment.NEUTRAL;
        }
    }

    /**
     * 创建默认结果
     */
    private IntentResult createDefaultResult(AgentRequest request) {
        return IntentResult.builder()
                .intentType("general")
                .confidence(0.8)
                .sentiment(IntentResult.Sentiment.NEUTRAL)
                .suggestedTaskType("chat")
                .isAmbiguous(false)
                .build();
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
