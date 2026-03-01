package com.eazyai.ai.nexus.tools;

import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.regex.Pattern;

/**
 * 计算器工具类
 * 提供数学计算能力
 */
@Slf4j
@Component
public class CalculatorTools {

    private static final Pattern SAFE_EXPRESSION = Pattern.compile("[\\d+\\-*/().\\s]+");
    private final ScriptEngine scriptEngine;

    public CalculatorTools() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("JavaScript");
    }

    /**
     * 执行数学计算
     * 
     * @param expression 数学表达式，如 "2 + 2 * 3"
     * @return 计算结果
     */
    @Tool(name = "calculate", value = "执行数学计算。支持 +, -, *, /, (, ) 等运算符。例如: '2 + 2 * 3'")
    public String calculate(String expression) {
        log.info("执行计算: {}", expression);
        long startTime = System.currentTimeMillis();
        
        try {
            // 安全检查
            if (!isSafeExpression(expression)) {
                String error = "错误: 表达式包含不安全字符";
                ToolExecutionContext.current().recordToolCallFailure(
                    "calculate", "数学计算", expression, error, 
                    System.currentTimeMillis() - startTime);
                return error;
            }

            // 替换中文符号
            String sanitized = sanitizeExpression(expression);
            
            // 执行计算
            Object result = scriptEngine.eval(sanitized);
            
            String resultStr;
            if (result instanceof Number) {
                BigDecimal bd = new BigDecimal(result.toString());
                // 保留6位小数，去除末尾的0
                bd = bd.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
                resultStr = bd.toPlainString();
            } else {
                resultStr = result.toString();
            }
            
            // 记录工具调用
            ToolExecutionContext.current().recordToolCall(
                "calculate", "数学计算", expression, resultStr, 
                System.currentTimeMillis() - startTime);
            
            return resultStr;
            
        } catch (ScriptException e) {
            log.error("计算错误", e);
            String error = "计算错误: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "calculate", "数学计算", expression, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * 计算百分比
     * 
     * @param params 格式: "数值|百分比" 如 "100|20"
     * @return 计算结果
     */
    @Tool(name = "calculate_percentage", value = "计算百分比。参数格式: '数值|百分比'，如 '100|20' 表示计算100的20%")
    public String calculatePercentage(String params) {
        long startTime = System.currentTimeMillis();
        
        try {
            String[] parts = params.split("\\|");
            if (parts.length != 2) {
                String error = "参数格式错误，请使用: '数值|百分比'";
                ToolExecutionContext.current().recordToolCallFailure(
                    "calculate_percentage", "百分比计算", params, error, 
                    System.currentTimeMillis() - startTime);
                return error;
            }

            BigDecimal value = new BigDecimal(parts[0].trim());
            BigDecimal percentage = new BigDecimal(parts[1].trim());
            
            BigDecimal result = value.multiply(percentage)
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                    .stripTrailingZeros();
            
            String resultStr = result.toPlainString();
            ToolExecutionContext.current().recordToolCall(
                "calculate_percentage", "百分比计算", params, resultStr, 
                System.currentTimeMillis() - startTime);
            
            return resultStr;
            
        } catch (NumberFormatException e) {
            String error = "数值格式错误: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "calculate_percentage", "百分比计算", params, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * 货币转换
     * 
     * @param params 格式: "金额|汇率" 如 "100|7.2"
     * @return 转换后金额
     */
    @Tool(name = "currency_convert", value = "货币汇率转换。参数格式: '金额|汇率'，如 '100|7.2'")
    public String currencyConvert(String params) {
        long startTime = System.currentTimeMillis();
        
        try {
            String[] parts = params.split("\\|");
            if (parts.length != 2) {
                String error = "参数格式错误，请使用: '金额|汇率'";
                ToolExecutionContext.current().recordToolCallFailure(
                    "currency_convert", "货币转换", params, error, 
                    System.currentTimeMillis() - startTime);
                return error;
            }

            BigDecimal amount = new BigDecimal(parts[0].trim());
            BigDecimal rate = new BigDecimal(parts[1].trim());
            
            BigDecimal result = amount.multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP);
            
            String resultStr = result.toPlainString();
            ToolExecutionContext.current().recordToolCall(
                "currency_convert", "货币转换", params, resultStr, 
                System.currentTimeMillis() - startTime);
            
            return resultStr;
            
        } catch (NumberFormatException e) {
            String error = "数值格式错误: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "currency_convert", "货币转换", params, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * 格式化数字
     * 
     * @param number 数字字符串
     * @return 格式化后的数字
     */
    @Tool(name = "format_number", value = "格式化数字，添加千分位分隔符")
    public String formatNumber(String number) {
        long startTime = System.currentTimeMillis();
        
        try {
            BigDecimal value = new BigDecimal(number.trim());
            NumberFormat formatter = NumberFormat.getInstance();
            formatter.setMaximumFractionDigits(6);
            String result = formatter.format(value);
            
            ToolExecutionContext.current().recordToolCall(
                "format_number", "数字格式化", number, result, 
                System.currentTimeMillis() - startTime);
            
            return result;
        } catch (NumberFormatException e) {
            String error = "数值格式错误: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "format_number", "数字格式化", number, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * 四舍五入
     * 
     * @param params 格式: "数值|小数位" 如 "3.14159|2"
     * @return 四舍五入后的结果
     */
    @Tool(name = "round", value = "四舍五入。参数格式: '数值|小数位'，如 '3.14159|2'")
    public String round(String params) {
        long startTime = System.currentTimeMillis();
        
        try {
            String[] parts = params.split("\\|");
            if (parts.length != 2) {
                String error = "参数格式错误，请使用: '数值|小数位'";
                ToolExecutionContext.current().recordToolCallFailure(
                    "round", "四舍五入", params, error, 
                    System.currentTimeMillis() - startTime);
                return error;
            }

            BigDecimal value = new BigDecimal(parts[0].trim());
            int scale = Integer.parseInt(parts[1].trim());
            
            String result = value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
            ToolExecutionContext.current().recordToolCall(
                "round", "四舍五入", params, result, 
                System.currentTimeMillis() - startTime);
            
            return result;
            
        } catch (Exception e) {
            String error = "计算错误: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "round", "四舍五入", params, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * 检查表达式是否安全
     */
    protected boolean isSafeExpression(String expression) {
        // 只允许数字、运算符和括号
        return SAFE_EXPRESSION.matcher(expression).matches();
    }

    /**
     * 清理表达式
     */
    protected String sanitizeExpression(String expression) {
        return expression
                .replace("，", ",")
                .replace("（", "(")
                .replace("）", ")")
                .replace("x", "*")
                .replace("X", "*")
                .replace("÷", "/");
    }
}
