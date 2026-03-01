package com.eazyai.ai.nexus.core.rule;

import java.util.List;
import java.util.Map;

/**
 * 规则引擎接口
 */
public interface RuleEngine {

    /**
     * 执行规则
     *
     * @param ruleId 规则ID
     * @param context 上下文
     * @return 执行结果
     */
    RuleResult execute(String ruleId, Map<String, Object> context);

    /**
     * 批量执行规则
     *
     * @param ruleIds 规则ID列表
     * @param context 上下文
     * @return 执行结果列表
     */
    List<RuleResult> executeBatch(List<String> ruleIds, Map<String, Object> context);

    /**
     * 注册规则
     *
     * @param rule 规则定义
     */
    void registerRule(Rule rule);

    /**
     * 注销规则
     *
     * @param ruleId 规则ID
     */
    void unregisterRule(String ruleId);

    /**
     * 获取规则
     *
     * @param ruleId 规则ID
     * @return 规则定义
     */
    Rule getRule(String ruleId);

    /**
     * 规则定义
     */
    record Rule(
        String ruleId,
        String name,
        String description,
        String type, // condition, decision_table, script
        String condition,
        List<RuleAction> actions,
        int priority,
        boolean enabled
    ) {}

    /**
     * 规则动作
     */
    record RuleAction(
        String type, // set_value, call_function, return_result
        String target,
        Object value,
        Map<String, Object> params
    ) {}

    /**
     * 规则执行结果
     */
    record RuleResult(
        String ruleId,
        boolean matched,
        boolean success,
        Object result,
        String errorMessage,
        long executionTime
    ) {}
}
