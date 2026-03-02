package com.eazyai.ai.nexus.core.reflection;

import com.eazyai.ai.nexus.api.react.ReActContext;
import com.eazyai.ai.nexus.api.react.ReActStep;
import com.eazyai.ai.nexus.api.react.ReflectionResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 反思型智能体
 * 在执行后进行反思，评估执行效果并提供改进建议
 */
@Slf4j
@Component
public class ReflectionAgent {

    private static final String REFLECTION_PROMPT = """
            你是一个智能代理的反思模块。请分析刚才的执行过程，评估执行效果。
            
            ## 用户请求
            %s
            
            ## 执行过程
            %s
            
            ## 最终答案
            %s
            
            请回答以下问题：
            1. 工具选择是否最优？
            2. 参数传递是否正确？
            3. 是否有更高效的执行路径？
            4. 结果是否符合预期？
            
            请以JSON格式输出反思结果：
            ```json
            {
              "success": true或false,
              "confidence": 0.0到1.0之间的置信度,
              "issues": ["发现的问题1", "发现的问题2"],
              "improvements": ["改进建议1", "改进建议2"],
              "shouldRetry": true或false,
              "alternativeApproach": "如果有更好的方法，描述替代方案",
              "summary": "执行过程总结"
            }
            ```
            
            只输出JSON，不要包含其他内容。
            """;

    @Autowired(required = false)
    private ChatLanguageModel chatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行反思
     * 
     * @param context ReAct 执行上下文
     * @return 反思结果
     */
    public ReflectionResult reflect(ReActContext context) {
        if (chatModel == null) {
            log.warn("[ReflectionAgent] ChatModel 未配置，跳过反思");
            return ReflectionResult.success("ChatModel 未配置，无法反思", 0.5);
        }

        log.info("[ReflectionAgent] 开始反思，迭代次数: {}, 工具调用: {}", 
                context.getCurrentIteration(), context.getUsedTools());

        try {
            String history = context.formatHistory();
            String prompt = String.format(REFLECTION_PROMPT, 
                    context.getUserInput(),
                    history,
                    context.getFinalAnswer() != null ? context.getFinalAnswer() : "无");

            String response = chatModel.generate(prompt);
            log.debug("[ReflectionAgent] LLM 反思响应: {}", response);

            return parseReflectionResult(response);

        } catch (Exception e) {
            log.error("[ReflectionAgent] 反思过程失败", e);
            return ReflectionResult.failure("反思过程异常: " + e.getMessage(), false);
        }
    }

    /**
     * 执行反思并判断是否需要重试
     * 
     * @param context ReAct 执行上下文
     * @param maxRetries 最大重试次数
     * @return 是否建议重试
     */
    public boolean shouldRetry(ReActContext context, int maxRetries) {
        ReflectionResult result = reflect(context);
        
        if (!result.isSuccess() && result.isShouldRetry()) {
            // 检查是否还有重试机会
            return context.getCurrentIteration() < maxRetries;
        }
        
        return false;
    }

    /**
     * 获取改进建议
     * 
     * @param context ReAct 执行上下文
     * @return 改进建议列表
     */
    public List<String> getImprovementSuggestions(ReActContext context) {
        ReflectionResult result = reflect(context);
        return result.getImprovements();
    }

    /**
     * 解析 LLM 返回的反思结果
     */
    private ReflectionResult parseReflectionResult(String response) {
        try {
            // 提取 JSON 部分
            String json = extractJson(response);
            
            Map<String, Object> map = objectMapper.readValue(json, 
                    new TypeReference<Map<String, Object>>() {});

            return ReflectionResult.builder()
                    .success(getBoolean(map, "success", true))
                    .confidence(getDouble(map, "confidence", 0.5))
                    .issues(getStringList(map, "issues"))
                    .improvements(getStringList(map, "improvements"))
                    .shouldRetry(getBoolean(map, "shouldRetry", false))
                    .alternativeApproach(getString(map, "alternativeApproach"))
                    .summary(getString(map, "summary"))
                    .build();

        } catch (Exception e) {
            log.warn("[ReflectionAgent] 解析反思结果失败，使用默认值: {}", e.getMessage());
            return ReflectionResult.builder()
                    .success(true)
                    .confidence(0.5)
                    .summary("解析失败，使用默认值")
                    .build();
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        // 移除 markdown 代码块标记
        String json = response;
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7);
            json = json.substring(0, json.indexOf("```"));
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3);
            json = json.substring(0, json.indexOf("```"));
        }
        return json.trim();
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }
}
