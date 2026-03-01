package com.eazyai.ai.nexus.examples;

import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct智能体示例
 * 展示如何使用ReAct架构实现任务处理
 */
@Slf4j
public class ReActExample {

    /**
     * 示例运行
     */
    public static void main(String[] args) {
        log.info("=== ReAct智能体示例 ===");
        
        // 注意：实际运行需要Spring环境和LLM配置
        // 这里展示的是代码结构
        
        // 示例1：数学问题
        AgentRequest mathRequest = AgentRequest.builder()
                .query("计算 (123 + 456) * 2 等于多少？")
                .taskType("math")
                .maxIterations(5)
                .build();
        
        log.info("数学问题: {}", mathRequest.getQuery());
        
        // 示例2：信息检索
        AgentRequest researchRequest = AgentRequest.builder()
                .query("查询人工智能的最新发展动态")
                .taskType("search")
                .maxIterations(10)
                .build();
        
        log.info("检索问题: {}", researchRequest.getQuery());
    }

    /**
     * 展示响应结果
     */
    private static void printResponse(AgentResponse response) {
        log.info("执行结果:");
        log.info("- 成功: {}", response.isSuccess());
        log.info("- 输出: {}", response.getOutput());
        log.info("- 耗时: {} ms", response.getExecutionTime());
        
        if (!response.getSteps().isEmpty()) {
            log.info("- 执行步骤:");
            response.getSteps().forEach(step -> {
                log.info("  Step {} [{}]: {}", 
                        step.getStepNumber(), 
                        step.getStage(),
                        step.getDescription());
            });
        }
    }
}
