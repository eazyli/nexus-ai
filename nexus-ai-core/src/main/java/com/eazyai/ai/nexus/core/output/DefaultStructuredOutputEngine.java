package com.eazyai.ai.nexus.core.output;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认结构化输出引擎实现
 */
@Slf4j
@Component
public class DefaultStructuredOutputEngine implements StructuredOutputEngine {

    private final Map<String, OutputSchema> schemaRegistry = new ConcurrentHashMap<>();
    
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final Pattern ARRAY_PATTERN = Pattern.compile("\\[[\\s\\S]*\\]");

    @Override
    public void registerSchema(String schemaId, OutputSchema schema) {
        schemaRegistry.put(schemaId, schema);
        log.info("注册输出Schema: {} - {}", schemaId, schema.name());
    }

    @Override
    public Optional<OutputSchema> getSchema(String schemaId) {
        return Optional.ofNullable(schemaRegistry.get(schemaId));
    }

    @Override
    public StructuredOutput format(String content, String schemaId) {
        OutputSchema schema = schemaRegistry.get(schemaId);
        if (schema == null) {
            return new StructuredOutput(false, null, null, content, "Schema不存在: " + schemaId);
        }
        return format(content, schema);
    }

    @Override
    public StructuredOutput format(String content, OutputSchema schema) {
        if (content == null || content.isEmpty()) {
            return new StructuredOutput(false, schema.type(), null, content, "内容为空");
        }

        try {
            return switch (schema.type().toLowerCase()) {
                case "json" -> formatAsJson(content, schema);
                case "xml" -> formatAsXml(content, schema);
                case "csv" -> formatAsCsv(content, schema);
                case "markdown" -> formatAsMarkdown(content, schema);
                default -> new StructuredOutput(true, "text", content, content, null);
            };
        } catch (Exception e) {
            log.error("格式化输出失败: {}", schema.schemaId(), e);
            return new StructuredOutput(false, schema.type(), null, content, e.getMessage());
        }
    }

    @Override
    public ValidationResult validate(Object output, String schemaId) {
        OutputSchema schema = schemaRegistry.get(schemaId);
        if (schema == null) {
            return new ValidationResult(false, List.of("Schema不存在"), List.of());
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Map<String, Object> schemaDef = schema.schema();
        if (schemaDef == null || schemaDef.isEmpty()) {
            return new ValidationResult(true, errors, warnings);
        }

        // 验证必填字段
        Object requiredObj = schemaDef.get("required");
        if (requiredObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) requiredObj;
            
            for (String field : required) {
                if (output instanceof Map) {
                    if (!((Map<?, ?>) output).containsKey(field)) {
                        errors.add("缺少必填字段: " + field);
                    }
                } else if (output instanceof JSONObject) {
                    if (!((JSONObject) output).containsKey(field)) {
                        errors.add("缺少必填字段: " + field);
                    }
                }
            }
        }

        // 验证字段类型
        Object propertiesObj = schemaDef.get("properties");
        if (propertiesObj instanceof Map && output instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;
            Map<?, ?> outputMap = (Map<?, ?>) output;

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldDef = entry.getValue();
                
                if (outputMap.containsKey(fieldName)) {
                    Object value = outputMap.get(fieldName);
                    if (fieldDef instanceof Map) {
                        String expectedType = (String) ((Map<?, ?>) fieldDef).get("type");
                        if (expectedType != null && !validateType(value, expectedType)) {
                            warnings.add("字段 " + fieldName + " 类型不匹配，期望: " + expectedType);
                        }
                    }
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * 格式化为JSON
     */
    private StructuredOutput formatAsJson(String content, OutputSchema schema) {
        // 尝试提取JSON
        String jsonStr = extractJson(content);
        
        if (jsonStr == null) {
            return new StructuredOutput(false, "json", null, content, "无法提取有效的JSON");
        }

        try {
            Object data = JSON.parse(jsonStr);
            
            // 验证
            ValidationResult validation = validate(data, schema.schemaId());
            if (!validation.valid()) {
                log.warn("JSON验证失败: {}", validation.errors());
            }

            return new StructuredOutput(true, "json", data, content, null);
        } catch (Exception e) {
            return new StructuredOutput(false, "json", null, content, "JSON解析失败: " + e.getMessage());
        }
    }

    /**
     * 格式化为XML
     */
    private StructuredOutput formatAsXml(String content, OutputSchema schema) {
        // TODO: 实现XML格式化
        return new StructuredOutput(true, "xml", content, content, null);
    }

    /**
     * 格式化为CSV
     */
    private StructuredOutput formatAsCsv(String content, OutputSchema schema) {
        // TODO: 实现CSV格式化
        return new StructuredOutput(true, "csv", content, content, null);
    }

    /**
     * 格式化为Markdown
     */
    private StructuredOutput formatAsMarkdown(String content, OutputSchema schema) {
        return new StructuredOutput(true, "markdown", content, content, null);
    }

    /**
     * 提取JSON内容
     */
    private String extractJson(String content) {
        // 尝试匹配JSON对象
        Matcher objectMatcher = JSON_PATTERN.matcher(content);
        if (objectMatcher.find()) {
            return objectMatcher.group();
        }
        
        // 尝试匹配JSON数组
        Matcher arrayMatcher = ARRAY_PATTERN.matcher(content);
        if (arrayMatcher.find()) {
            return arrayMatcher.group();
        }
        
        return null;
    }

    /**
     * 验证类型
     */
    private boolean validateType(Object value, String expectedType) {
        return switch (expectedType.toLowerCase()) {
            case "string" -> value instanceof String;
            case "number", "integer" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List;
            case "object" -> value instanceof Map;
            default -> true;
        };
    }
}
