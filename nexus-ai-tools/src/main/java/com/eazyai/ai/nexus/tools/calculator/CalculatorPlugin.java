package com.eazyai.ai.nexus.tools.calculator;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.executor.ExecutionResult;
import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 计算器插件
 * 提供数学计算能力
 */
@Slf4j
@Component
public class CalculatorPlugin implements Plugin {

    private final ScriptEngine scriptEngine;
    private final PluginDescriptor descriptor;

    public CalculatorPlugin() {
        this.scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
        this.descriptor = PluginDescriptor.builder()
                .id("calculator")
                .name("Calculator Plugin")
                .version("1.0.0")
                .type("calculator")
                .description("数学计算插件，支持基本算术运算和数学表达式求值")
                .author("AI Agent Team")
                .capabilities(List.of("math", "calculation", "arithmetic"))
                .parameters(List.of(
                        PluginDescriptor.ParameterDef.builder()
                                .name("expression")
                                .type("string")
                                .description("数学表达式，支持加减乘除、括号、数学函数等")
                                .required(true)
                                .example("(3 + 5) * 2 - 4 / 2")
                                .validation(Map.of("maxLength", 1000))
                                .build()
                ))
                // 触发条件
                .triggerConditions("""
                        当用户问题满足以下条件之一时，应调用此工具：
                        1. 包含明确的数学计算需求（如"计算"、"等于多少"）
                        2. 问题中包含数字和运算符组合的表达式
                        3. 需要进行单位换算、百分比计算等数值运算
                        4. 用户明确要求进行数学运算
                        """)
                // 使用指导
                .guidance("""
                        最佳实践：
                        1. 表达式应使用标准数学语法，支持 + - * / ( ) 等运算符
                        2. 支持JavaScript数学函数，如 Math.sqrt(), Math.pow() 等
                        3. 表达式长度限制为1000字符以内
                        4. 复杂计算建议分解为多个步骤，逐步计算
                        5. 对于包含变量的表达式，应先确认变量值再调用
                        """)
                // 使用示例
                .examples(List.of(
                        PluginDescriptor.UsageExample.builder()
                                .scenario("基础算术运算")
                                .userInput("计算 123 + 456 等于多少")
                                .toolArguments(Map.of("expression", "123 + 456"))
                                .expectedOutput("579")
                                .notes("直接提取用户表达式进行计算")
                                .build(),
                        PluginDescriptor.UsageExample.builder()
                                .scenario("复杂表达式计算")
                                .userInput("帮我算一下 (15 + 25) * 3 / 4")
                                .toolArguments(Map.of("expression", "(15 + 25) * 3 / 4"))
                                .expectedOutput("30")
                                .notes("支持括号和运算符优先级")
                                .build(),
                        PluginDescriptor.UsageExample.builder()
                                .scenario("数学函数计算")
                                .userInput("求100的平方根")
                                .toolArguments(Map.of("expression", "Math.sqrt(100)"))
                                .expectedOutput("10")
                                .notes("支持JavaScript Math对象的方法")
                                .build()
                ))
                // 错误处理
                .errorHandling("""
                        常见错误及处理：
                        1. 表达式语法错误：检查运算符和括号是否匹配
                        2. 表达式过长：建议拆分为多个计算步骤
                        3. 除零错误：检查分母是否为0
                        4. 不支持的运算：告知用户支持的运算类型
                        """)
                // 执行特性
                .idempotent(true)
                .priority(20)
                .estimatedDuration(10L)
                .config(Map.of())
                .enabled(true)
                .build();
    }

    @Override
    public ExecutionResult execute(Map<String, Object> params, AgentContext context) {
        String expression = (String) params.get("expression");
        if (expression == null || expression.trim().isEmpty()) {
            return ExecutionResult.error("Expression is required", null);
        }

        try {
            // 安全：限制表达式长度
            if (expression.length() > 1000) {
                return ExecutionResult.error("Expression too long (max 1000 chars)", null);
            }

            // 执行计算
            Object result = scriptEngine.eval(expression);

            return ExecutionResult.success(Map.of(
                    "expression", expression,
                    "result", result,
                    "resultType", result.getClass().getSimpleName()
            ));

        } catch (ScriptException e) {
            log.error("Calculation failed for expression: {}", expression, e);
            return ExecutionResult.error("Calculation error: " + e.getMessage(), e);
        }
    }

    @Override
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean supports(Map<String, Object> params) {
        return params.containsKey("expression");
    }
}
