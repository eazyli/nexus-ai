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
                .description("数学计算插件，支持基本算术运算")
                .author("AI Agent Team")
                .capabilities(List.of("math", "calculation", "arithmetic"))
                .parameters(List.of(
                        PluginDescriptor.ParameterDef.builder()
                                .name("expression")
                                .type("string")
                                .description("数学表达式")
                                .required(true)
                                .build()
                ))
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
