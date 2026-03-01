package com.eazyai.ai.nexus.core.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认规则引擎实现
 * 支持条件表达式和JavaScript脚本
 */
@Slf4j
@Component
public class DefaultRuleEngine implements RuleEngine {

    private final Map<String, Rule> ruleRegistry = new ConcurrentHashMap<>();
    private final ScriptEngine scriptEngine;

    public DefaultRuleEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("JavaScript");
    }

    @Override
    public RuleResult execute(String ruleId, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        
        Rule rule = ruleRegistry.get(ruleId);
        if (rule == null) {
            return new RuleResult(ruleId, false, false, null, "规则不存在: " + ruleId, 0);
        }

        if (!rule.enabled()) {
            return new RuleResult(ruleId, false, true, null, null, 0);
        }

        try {
            // 评估条件
            boolean matched = evaluateCondition(rule.condition(), context);
            
            if (!matched) {
                return new RuleResult(ruleId, false, true, null, null, 
                        System.currentTimeMillis() - startTime);
            }

            // 执行动作
            Object result = executeActions(rule.actions(), context);
            
            return new RuleResult(ruleId, true, true, result, null, 
                    System.currentTimeMillis() - startTime);
                    
        } catch (Exception e) {
            log.error("规则执行失败: {}", ruleId, e);
            return new RuleResult(ruleId, false, false, null, e.getMessage(), 
                    System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public List<RuleResult> executeBatch(List<String> ruleIds, Map<String, Object> context) {
        // 按优先级排序
        List<Rule> rules = ruleIds.stream()
                .map(ruleRegistry::get)
                .filter(Objects::nonNull)
                .filter(Rule::enabled)
                .sorted(Comparator.comparingInt(Rule::priority).reversed())
                .toList();

        List<RuleResult> results = new ArrayList<>();
        for (Rule rule : rules) {
            RuleResult result = execute(rule.ruleId(), context);
            results.add(result);
            
            // 如果规则匹配且返回了结果，可以选择停止继续执行
            if (result.matched() && result.result() != null) {
                break;
            }
        }
        
        return results;
    }

    @Override
    public void registerRule(Rule rule) {
        ruleRegistry.put(rule.ruleId(), rule);
        log.info("注册规则: {} - {} (优先级: {})", rule.ruleId(), rule.name(), rule.priority());
    }

    @Override
    public void unregisterRule(String ruleId) {
        Rule removed = ruleRegistry.remove(ruleId);
        if (removed != null) {
            log.info("注销规则: {}", ruleId);
        }
    }

    @Override
    public Rule getRule(String ruleId) {
        return ruleRegistry.get(ruleId);
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        try {
            // 简单条件解析
            if (condition.startsWith("${") && condition.endsWith("}")) {
                // SpEL风格表达式
                String expression = condition.substring(2, condition.length() - 1);
                return evaluateExpression(expression, context);
            }
            
            if (condition.contains("==") || condition.contains("!=") || 
                condition.contains(">") || condition.contains("<") ||
                condition.contains("&&") || condition.contains("||")) {
                // JavaScript表达式
                return evaluateScript(condition, context);
            }
            
            // 默认当作真值处理
            return Boolean.parseBoolean(condition);
            
        } catch (Exception e) {
            log.warn("条件评估失败: {}", condition, e);
            return false;
        }
    }

    /**
     * 评估简单表达式
     */
    private boolean evaluateExpression(String expression, Map<String, Object> context) {
        // 解析形如 "input == 'hello'" 或 "count > 5" 的表达式
        expression = expression.trim();
        
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            expression = expression.replace(entry.getKey(), 
                    "\"" + String.valueOf(entry.getValue()) + "\"");
        }
        
        return evaluateScript(expression, context);
    }

    /**
     * 使用脚本引擎评估
     */
    private boolean evaluateScript(String script, Map<String, Object> context) {
        try {
            // 绑定上下文变量
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                scriptEngine.put(entry.getKey(), entry.getValue());
            }
            
            Object result = scriptEngine.eval(script);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("脚本评估失败: {}", script, e);
            return false;
        }
    }

    /**
     * 执行动作列表
     */
    private Object executeActions(List<RuleAction> actions, Map<String, Object> context) {
        Object result = null;
        
        for (RuleAction action : actions) {
            result = executeAction(action, context);
        }
        
        return result;
    }

    /**
     * 执行单个动作
     */
    private Object executeAction(RuleAction action, Map<String, Object> context) {
        return switch (action.type()) {
            case "set_value" -> {
                context.put(action.target(), action.value());
                yield action.value();
            }
            case "return_result" -> action.value();
            default -> null;
        };
    }
}
