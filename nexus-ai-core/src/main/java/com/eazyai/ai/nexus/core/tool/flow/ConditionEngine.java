package com.eazyai.ai.nexus.core.tool.flow;

import com.eazyai.ai.nexus.api.tool.flow.FlowExecutionContext;
import com.eazyai.ai.nexus.api.tool.flow.StepExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件表达式引擎
 * 
 * <p>支持多种表达式语法：</p>
 * <ul>
 *   <li>SpEL: #{expression}</li>
 *   <li>JavaScript: ${expression}（兼容模式）</li>
 *   <li>简单比较: variable op value</li>
 *   <li>JSONPath: $.path.to.value</li>
 * </ul>
 *
 * <h3>示例：</h3>
 * <pre>
 * #{variables.score > 0.8}           // SpEL
 * ${score > 0.8 && intent == 'qa'}   // JavaScript风格
 * score > 0.8                         // 简单比较
 * $.step1.output.data.id             // JSONPath
 * </pre>
 */
@Slf4j
@Component
public class ConditionEngine {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    
    // SpEL 表达式缓存
    private final Map<String, Expression> spelExpressionCache = new ConcurrentHashMap<>();
    
    // 简单比较表达式正则
    private static final Pattern SIMPLE_COMPARISON_PATTERN = Pattern.compile(
            "^\\s*(\\w+(?:\\.\\w+)*)\\s*(==|!=|>=|<=|>|<|contains|startsWith|endsWith|matches|in|not\\s+in)\\s*(.+)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    
    // JSONPath 前缀
    private static final String JSONPATH_PREFIX = "$.";
    
    /**
     * 评估条件表达式
     *
     * @param expression 条件表达式
     * @param context    流程执行上下文
     * @return 条件结果
     */
    public ConditionResult evaluate(String expression, FlowExecutionContext context) {
        if (expression == null || expression.isBlank()) {
            return ConditionResult.success(true, "空表达式默认返回 true");
        }
        
        expression = expression.trim();
        log.debug("[ConditionEngine] 评估表达式: {}", expression);
        
        try {
            // 1. 检测并执行 SpEL 表达式
            if (expression.startsWith("#{") && expression.endsWith("}")) {
                return evaluateSpEL(expression.substring(2, expression.length() - 1), context);
            }
            
            // 2. 检测并执行变量引用表达式 ${...}
            if (expression.startsWith("${") && expression.endsWith("}")) {
                String inner = expression.substring(2, expression.length() - 1).trim();
                // 如果内部是变量路径，解析值后转布尔
                if (!containsOperators(inner)) {
                    Object value = resolveVariable(inner, context);
                    return ConditionResult.success(convertToBoolean(value), "变量: " + inner);
                }
                // 否则作为表达式评估
                return evaluateAsExpression(inner, context);
            }
            
            // 3. 检测并执行 JSONPath 表达式
            if (expression.startsWith(JSONPATH_PREFIX)) {
                return evaluateJsonPath(expression, context);
            }
            
            // 4. 检测并执行简单比较表达式
            Matcher matcher = SIMPLE_COMPARISON_PATTERN.matcher(expression);
            if (matcher.matches()) {
                return evaluateSimpleComparison(
                        matcher.group(1),  // 变量名
                        matcher.group(2),  // 操作符
                        matcher.group(3),  // 比较值
                        context
                );
            }
            
            // 5. 尝试作为变量引用或表达式评估
            return evaluateAsExpression(expression, context);
            
        } catch (Exception e) {
            log.error("[ConditionEngine] 条件表达式评估失败: {}", expression, e);
            return ConditionResult.failure("表达式评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行 SpEL 表达式
     */
    private ConditionResult evaluateSpEL(String expression, FlowExecutionContext context) {
        try {
            EvaluationContext evalContext = buildSpelEvaluationContext(context);
            
            Expression exp = spelExpressionCache.computeIfAbsent(
                    expression, 
                    SPEL_PARSER::parseExpression
            );
            
            Object result = exp.getValue(evalContext);
            boolean booleanResult = convertToBoolean(result);
            
            log.debug("[ConditionEngine] SpEL 结果: {} → {}", expression, booleanResult);
            return ConditionResult.success(booleanResult, "SpEL: " + expression);
            
        } catch (Exception e) {
            log.error("[ConditionEngine] SpEL 表达式评估失败: {}", expression, e);
            return ConditionResult.failure("SpEL 评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 作为表达式评估（支持简单逻辑运算）
     */
    private ConditionResult evaluateAsExpression(String expression, FlowExecutionContext context) {
        try {
            // 处理逻辑运算符
            if (expression.contains("&&") || expression.contains("||")) {
                return evaluateLogicalExpression(expression, context);
            }
            
            // 尝试解析为变量
            Object value = resolveVariable(expression, context);
            if (value != null) {
                return ConditionResult.success(convertToBoolean(value), "变量: " + expression);
            }
            
            // 尝试解析为布尔字面值
            if ("true".equalsIgnoreCase(expression)) {
                return ConditionResult.success(true, "字面值: true");
            }
            if ("false".equalsIgnoreCase(expression)) {
                return ConditionResult.success(false, "字面值: false");
            }
            
            // 默认返回 false
            return ConditionResult.success(false, "无法解析: " + expression);
            
        } catch (Exception e) {
            log.warn("[ConditionEngine] 表达式评估失败: {}", expression, e);
            return ConditionResult.failure("表达式评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 评估逻辑表达式
     */
    private ConditionResult evaluateLogicalExpression(String expression, FlowExecutionContext context) {
        // 处理 && 优先级低于 ||
        if (expression.contains("||")) {
            String[] orParts = splitByOperator(expression, "||");
            for (String part : orParts) {
                ConditionResult result = evaluate(part.trim(), context);
                if (result.isSuccess() && result.getValue()) {
                    return ConditionResult.success(true, "OR 分支为 true");
                }
            }
            return ConditionResult.success(false, "所有 OR 分支为 false");
        }
        
        if (expression.contains("&&")) {
            String[] andParts = splitByOperator(expression, "&&");
            for (String part : andParts) {
                ConditionResult result = evaluate(part.trim(), context);
                if (!result.isSuccess() || !result.getValue()) {
                    return ConditionResult.success(false, "AND 分支为 false");
                }
            }
            return ConditionResult.success(true, "所有 AND 分支为 true");
        }
        
        return evaluate(expression, context);
    }
    
    /**
     * 按操作符分割表达式
     */
    private String[] splitByOperator(String expression, String operator) {
        // 简单分割，不考虑括号嵌套
        return expression.split("\\s*" + Pattern.quote(operator) + "\\s*");
    }
    
    /**
     * 执行 JSONPath 表达式
     */
    private ConditionResult evaluateJsonPath(String jsonPath, FlowExecutionContext context) {
        try {
            Object value = resolveJsonPath(jsonPath, context);
            boolean booleanResult = convertToBoolean(value);
            
            log.debug("[ConditionEngine] JSONPath 结果: {} → {}", jsonPath, booleanResult);
            return ConditionResult.success(booleanResult, "JSONPath: " + jsonPath);
            
        } catch (Exception e) {
            log.error("[ConditionEngine] JSONPath 表达式评估失败: {}", jsonPath, e);
            return ConditionResult.failure("JSONPath 评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行简单比较表达式
     */
    private ConditionResult evaluateSimpleComparison(
            String variableName, 
            String operator, 
            String compareValue,
            FlowExecutionContext context) {
        
        try {
            Object actualValue = resolveVariable(variableName, context);
            Object expectedValue = parseCompareValue(compareValue.trim());
            boolean result = compare(actualValue, operator.trim().toLowerCase(), expectedValue);
            
            log.debug("[ConditionEngine] 简单比较: {} {} {} → {}", 
                    actualValue, operator, expectedValue, result);
            return ConditionResult.success(result, 
                    String.format("%s %s %s", actualValue, operator, expectedValue));
            
        } catch (Exception e) {
            log.error("[ConditionEngine] 简单比较评估失败: {} {} {}", variableName, operator, compareValue, e);
            return ConditionResult.failure("比较评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析变量
     */
    public Object resolveVariable(String variableName, FlowExecutionContext context) {
        if (variableName == null || variableName.isBlank()) {
            return null;
        }
        
        variableName = variableName.trim();
        
        // 支持点号访问：step1.output.id
        if (variableName.contains(".")) {
            return resolveNestedVariable(variableName, context);
        }
        
        // 先查变量，再查输入参数
        Object value = context.getVariable(variableName);
        if (value == null && context.getInput() != null) {
            value = context.getInput().get(variableName);
        }
        
        return value;
    }
    
    /**
     * 解析嵌套变量（如 step1.output.data.id）
     */
    private Object resolveNestedVariable(String path, FlowExecutionContext context) {
        String[] parts = path.split("\\.");
        Object current = null;
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (i == 0) {
                // 第一部分：从上下文获取
                current = context.getVariable(part);
                if (current == null) {
                    // 尝试从步骤结果获取
                    StepExecutionResult stepResult = context.getStepResult(part);
                    if (stepResult != null) {
                        current = stepResult.getOutput();
                    }
                }
                if (current == null && context.getInput() != null) {
                    current = context.getInput().get(part);
                }
            } else {
                current = getProperty(current, part);
            }
            
            if (current == null) {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * 解析 JSONPath
     */
    private Object resolveJsonPath(String jsonPath, FlowExecutionContext context) {
        // 简化实现：将 $.step1.output.data 转换为 step1.output.data
        if (jsonPath.startsWith("$.")) {
            jsonPath = jsonPath.substring(2);
        }
        return resolveNestedVariable(jsonPath, context);
    }
    
    /**
     * 获取对象属性
     */
    @SuppressWarnings("unchecked")
    private Object getProperty(Object obj, String propertyName) {
        if (obj == null) {
            return null;
        }
        
        // Map 类型
        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).get(propertyName);
        }
        
        // JsonNode 类型
        if (obj instanceof JsonNode) {
            JsonNode node = (JsonNode) obj;
            if (node.has(propertyName)) {
                JsonNode child = node.get(propertyName);
                return jsonNodeToObject(child);
            }
            return null;
        }
        
        // 处理数组索引 [0]
        if (propertyName.startsWith("[") && propertyName.endsWith("]")) {
            try {
                int index = Integer.parseInt(propertyName.substring(1, propertyName.length() - 1));
                if (obj instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) obj;
                    return index < list.size() ? list.get(index) : null;
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
            return null;
        }
        
        // POJO 类型 - 使用反射
        try {
            // 尝试 getter 方法
            String getterName = "get" + propertyName.substring(0, 1).toUpperCase() 
                    + propertyName.substring(1);
            Method method = obj.getClass().getMethod(getterName);
            return method.invoke(obj);
        } catch (Exception ex1) {
            try {
                // 尝试字段访问
                Field field = obj.getClass().getDeclaredField(propertyName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception ex2) {
                log.trace("[ConditionEngine] 无法获取属性: {} from {}", propertyName, obj.getClass().getName());
                return null;
            }
        }
    }
    
    /**
     * JsonNode 转对象
     */
    private Object jsonNodeToObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isInt()) {
            return node.asInt();
        }
        if (node.isLong()) {
            return node.asLong();
        }
        if (node.isDouble()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node;
    }
    
    /**
     * 解析比较值
     */
    private Object parseCompareValue(String value) {
        // 去除引号
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        
        // 数字
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        }
        
        // 布尔
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        
        // null
        if ("null".equalsIgnoreCase(value)) {
            return null;
        }
        
        // 数组（逗号分隔）
        if (value.startsWith("[") && value.endsWith("]")) {
            String[] items = value.substring(1, value.length() - 1).split(",");
            return Arrays.stream(items)
                    .map(String::trim)
                    .toArray(String[]::new);
        }
        
        return value;
    }
    
    /**
     * 执行比较
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean compare(Object actualValue, String operator, Object expectedValue) {
        // 处理 null
        if (actualValue == null) {
            return switch (operator) {
                case "==", "equals" -> expectedValue == null;
                case "!=", "notequals" -> expectedValue != null;
                default -> false;
            };
        }
        
        return switch (operator) {
            case "==", "equals" -> equals(actualValue, expectedValue);
            case "!=", "notequals" -> !equals(actualValue, expectedValue);
            case ">" -> compareNumbers(actualValue, expectedValue) > 0;
            case ">=" -> compareNumbers(actualValue, expectedValue) >= 0;
            case "<" -> compareNumbers(actualValue, expectedValue) < 0;
            case "<=" -> compareNumbers(actualValue, expectedValue) <= 0;
            case "contains" -> contains(actualValue, expectedValue);
            case "startswith" -> actualValue.toString().startsWith(expectedValue.toString());
            case "endswith" -> actualValue.toString().endsWith(expectedValue.toString());
            case "matches" -> actualValue.toString().matches(expectedValue.toString());
            case "in" -> {
                if (expectedValue instanceof Object[] arr) {
                    yield Arrays.asList(arr).contains(actualValue);
                }
                if (expectedValue instanceof Iterable iterable) {
                    for (Object item : iterable) {
                        if (equals(actualValue, item)) {
                            yield true;
                        }
                    }
                    yield false;
                }
                yield false;
            }
            case "not in" -> {
                if (expectedValue instanceof Object[] arr) {
                    yield !Arrays.asList(arr).contains(actualValue);
                }
                if (expectedValue instanceof Iterable iterable) {
                    for (Object item : iterable) {
                        if (equals(actualValue, item)) {
                            yield false;
                        }
                    }
                    yield true;
                }
                yield true;
            }
            default -> throw new IllegalArgumentException("不支持的操作符: " + operator);
        };
    }
    
    /**
     * 相等比较
     */
    private boolean equals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        
        // 类型转换后比较
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        
        return a.equals(b) || a.toString().equals(b.toString());
    }
    
    /**
     * 数字比较
     */
    private int compareNumbers(Object a, Object b) {
        double aVal = a instanceof Number ? ((Number) a).doubleValue() : Double.parseDouble(a.toString());
        double bVal;
        if (b instanceof Number) {
            bVal = ((Number) b).doubleValue();
        } else if (b != null) {
            bVal = Double.parseDouble(b.toString());
        } else {
            return 1; // null 视为最小
        }
        return Double.compare(aVal, bVal);
    }
    
    /**
     * 包含检查
     */
    private boolean contains(Object container, Object item) {
        String containerStr = container.toString();
        String itemStr = item.toString();
        
        // 集合包含
        if (container instanceof Iterable) {
            for (Object obj : (Iterable<?>) container) {
                if (equals(obj, item)) {
                    return true;
                }
            }
            return false;
        }
        
        // 字符串包含
        return containerStr.contains(itemStr);
    }
    
    /**
     * 转换为布尔值
     */
    private boolean convertToBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        if (value instanceof String) {
            String str = ((String) value).trim().toLowerCase();
            return !str.isEmpty() && !"false".equals(str) && !"0".equals(str);
        }
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (node.isBoolean()) {
                return node.asBoolean();
            }
            if (node.isNumber()) {
                return node.asDouble() != 0;
            }
            if (node.isTextual()) {
                return convertToBoolean(node.asText());
            }
        }
        return true;
    }
    
    /**
     * 检查是否包含运算符
     */
    private boolean containsOperators(String expression) {
        return expression.contains("==") || expression.contains("!=") 
                || expression.contains(">") || expression.contains("<")
                || expression.contains("&&") || expression.contains("||")
                || expression.toLowerCase().contains("contains")
                || expression.toLowerCase().contains("in");
    }
    
    /**
     * 构建 SpEL 评估上下文
     */
    private EvaluationContext buildSpelEvaluationContext(FlowExecutionContext context) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        
        // 设置变量
        evalContext.setVariable("input", context.getInput());
        evalContext.setVariable("variables", context.getVariables());
        evalContext.setVariable("steps", context.getStepResults());
        evalContext.setVariable("flow", Map.of(
                "input", context.getInput() != null ? context.getInput() : Map.of(),
                "variables", context.getVariables() != null ? context.getVariables() : Map.of(),
                "steps", context.getStepResults() != null ? context.getStepResults() : Map.of()
        ));
        
        return evalContext;
    }
    
    /**
     * 条件评估结果
     */
    public static class ConditionResult {
        private final boolean success;
        private final boolean value;
        private final String message;
        
        private ConditionResult(boolean success, boolean value, String message) {
            this.success = success;
            this.value = value;
            this.message = message;
        }
        
        public static ConditionResult success(boolean value, String message) {
            return new ConditionResult(true, value, message);
        }
        
        public static ConditionResult failure(String message) {
            return new ConditionResult(false, false, message);
        }
        
        public boolean isSuccess() { return success; }
        public boolean getValue() { return value; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("ConditionResult{success=%s, value=%s, message='%s'}", 
                    success, value, message);
        }
    }
}
