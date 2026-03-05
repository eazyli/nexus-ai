package com.eazyai.ai.nexus.core.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.intent.IntentAnalyzer;
import com.eazyai.ai.nexus.api.intent.IntentResult;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.core.tool.DefaultToolBus;
import com.eazyai.ai.nexus.core.tool.ToolUsageHistoryService;
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
 * <p>核心功能：</p>
 * <ul>
 *   <li>深度理解用户意图</li>
 *   <li>提取关键实体信息</li>
 *   <li>基于工具能力描述和历史使用数据，智能推荐工具</li>
 *   <li>输出结构化JSON，包含推荐工具及其参数建议</li>
 * </ul>
 */
@Slf4j
@Component
public class LlmIntentAnalyzer implements IntentAnalyzer {

    @Autowired(required = false)
    private ChatLanguageModel chatModel;

    @Autowired
    private DefaultToolBus toolBus;

    @Autowired
    private ToolUsageHistoryService historyService;

    private static final String INTENT_ANALYSIS_PROMPT = """
            你是一个专业的意图分析专家。请分析用户输入，理解用户意图，并从可用工具中选择最合适的工具。

            ## 用户输入
            %s

            ## 应用上下文
            应用ID: %s
            用户ID: %s
            会话历史: %s

            ## 可用工具列表
            %s

            ## 工具使用历史统计
            %s

            ## 分析要求
            1. 深度理解用户真实意图，不要只看表面关键词
            2. 从可用工具中选择最匹配的1-3个工具
            3. 对于每个推荐工具，说明推荐理由，并给出建议参数
            4. 优先推荐历史成功率高的工具
            5. 如果用户意图不明确，提出澄清问题
            6. 提取用户输入中的关键实体（时间、地点、人物、数量等）

            ## 输出格式（JSON）
            ```json
            {
              "intent_type": "意图类型（如：query/search/action/chat）",
              "intent_description": "意图详细描述",
              "confidence": 0.95,
              "entities": {
                "实体名": "实体值"
              },
              "sentiment": "POSITIVE/NEGATIVE/NEUTRAL",
              "is_ambiguous": false,
              "clarification_question": "如果不明确，需要澄清的问题",
              "reasoning": "分析推理过程",
              "recommended_tools": [
                {
                  "tool_id": "工具ID",
                  "tool_name": "工具名称",
                  "reason": "推荐理由",
                  "score": 0.95,
                  "suggested_params": {
                    "参数名": "建议值"
                  }
                }
              ]
            }
            ```

            只返回JSON，不要其他文字。
            """;

    @Override
    public IntentResult analyze(AgentRequest request, AgentContext context) {
        if (chatModel == null) {
            log.warn("[LlmIntentAnalyzer] ChatLanguageModel 不可用，返回默认意图, appId={}, query={}", 
                    context != null ? context.getAppId() : "null", 
                    request.getQuery() != null && request.getQuery().length() > 50 
                            ? request.getQuery().substring(0, 50) + "..." : request.getQuery());
            return createDefaultResult(request);
        }

        try {
            // 获取应用可访问的工具
            String appId = context != null ? context.getAppId() : null;
            List<ToolDescriptor> accessibleTools = toolBus.findAccessibleTools(appId);

            if (accessibleTools.isEmpty()) {
                log.warn("[LlmIntentAnalyzer] 应用无可访问工具, appId={}, query={}", 
                        appId, 
                        request.getQuery() != null && request.getQuery().length() > 50 
                                ? request.getQuery().substring(0, 50) + "..." : request.getQuery());
                return createDefaultResult(request);
            }

            // 构建工具描述
            String toolDescriptions = buildToolDescriptions(accessibleTools);

            // 获取工具使用历史
            String historyStats = historyService.formatToolStatsForLLM(appId);

            // 构建会话历史摘要
            String sessionSummary = buildSessionSummary(context);

            // 构建提示词
            String prompt = String.format(INTENT_ANALYSIS_PROMPT,
                    request.getQuery(),
                    appId != null ? appId : "无",
                    request.getUserId() != null ? request.getUserId() : "匿名",
                    sessionSummary,
                    toolDescriptions,
                    historyStats);

            // 调用LLM
            Response<AiMessage> response = chatModel.generate(
                    new SystemMessage("你是意图分析专家，擅长深度理解用户需求并智能推荐最合适的工具。"),
                    new UserMessage(prompt)
            );

            String content = response.content().text();
            log.debug("LLM意图分析响应: {}", content);

            // 解析结果
            return parseResult(content, request, accessibleTools);

        } catch (Exception e) {
            log.error("[LlmIntentAnalyzer] 意图分析失败, appId={}, query={}", 
                    context != null ? context.getAppId() : "null", 
                    request.getQuery(), e);
            return createDefaultResult(request);
        }
    }

    /**
     * 构建工具描述
     */
    private String buildToolDescriptions(List<ToolDescriptor> tools) {
        StringBuilder sb = new StringBuilder();
        
        // 获取历史成功率
        Map<String, Double> successRates = historyService.getAllSuccessRates();

        for (int i = 0; i < tools.size(); i++) {
            ToolDescriptor tool = tools.get(i);
            sb.append(String.format("%d. 工具ID: %s\n", i + 1, tool.getToolId()));
            sb.append(String.format("   名称: %s\n", tool.getName()));
            sb.append(String.format("   描述: %s\n", tool.getDescription()));
            sb.append(String.format("   能力标签: %s\n", 
                    tool.getCapabilities() != null ? String.join(", ", tool.getCapabilities()) : "无"));
            
            // 历史成功率
            Double rate = successRates.get(tool.getToolId());
            if (rate != null) {
                sb.append(String.format("   历史成功率: %.1f%%\n", rate * 100));
            }
            
            // 参数说明
            if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                sb.append("   参数:\n");
                for (ToolDescriptor.ParamDefinition param : tool.getParameters()) {
                    sb.append(String.format("     - %s (%s): %s%s\n",
                            param.getName(),
                            param.getType(),
                            param.getDescription(),
                            Boolean.TRUE.equals(param.getRequired()) ? " [必填]" : ""));
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建会话历史摘要
     */
    private String buildSessionSummary(AgentContext context) {
        if (context == null || context.getSessionHistory() == null || context.getSessionHistory().isEmpty()) {
            return "无历史对话";
        }
        
        // 只取最近3轮对话
        int size = Math.min(3, context.getSessionHistory().size());
        List<String> recentMessages = context.getSessionHistory().subList(
                Math.max(0, context.getSessionHistory().size() - size),
                context.getSessionHistory().size());

        return recentMessages.stream()
                .map(msg -> {
                    if (msg.length() > 100) {
                        return msg.substring(0, 100) + "...";
                    }
                    return msg;
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 解析LLM返回的JSON
     */
    private IntentResult parseResult(String content, AgentRequest request, List<ToolDescriptor> tools) {
        try {
            // 提取JSON部分
            String json = extractJson(content);
            JSONObject result = JSON.parseObject(json);

            // 解析推荐工具
            List<IntentResult.ToolRecommendation> recommendations = new ArrayList<>();
            JSONArray toolsArray = result.getJSONArray("recommended_tools");
            Map<String, Double> successRates = historyService.getAllSuccessRates();

            if (toolsArray != null) {
                for (int i = 0; i < toolsArray.size(); i++) {
                    JSONObject toolJson = toolsArray.getJSONObject(i);
                    String toolId = toolJson.getString("tool_id");

                    IntentResult.ToolRecommendation rec = IntentResult.ToolRecommendation.builder()
                            .toolId(toolId)
                            .toolName(toolJson.getString("tool_name"))
                            .reason(toolJson.getString("reason"))
                            .score(toolJson.getDoubleValue("score"))
                            .historicalSuccessRate(successRates.get(toolId))
                            .suggestedParams(toolJson.getJSONObject("suggested_params") != null ?
                                    toolJson.getJSONObject("suggested_params").getInnerMap() : new HashMap<>())
                            .build();

                    recommendations.add(rec);
                }
            }

            return IntentResult.builder()
                    .intentType(result.getString("intent_type"))
                    .confidence(result.getDoubleValue("confidence"))
                    .entities(result.getJSONObject("entities") != null ?
                            result.getJSONObject("entities").getInnerMap() : new HashMap<>())
                    .sentiment(parseSentiment(result.getString("sentiment")))
                    .isAmbiguous(result.getBooleanValue("is_ambiguous"))
                    .clarificationQuestion(result.getString("clarification_question"))
                    .reasoning(result.getString("reasoning"))
                    .recommendedTools(recommendations)
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
                .reasoning("LLM不可用或无可用工具，返回默认意图")
                .build();
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
