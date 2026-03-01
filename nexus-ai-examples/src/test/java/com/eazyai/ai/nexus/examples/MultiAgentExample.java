package com.eazyai.ai.nexus.examples;

import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.core.Agent;
import lombok.extern.slf4j.Slf4j;

/**
 * 多智能体协作示例
 * 展示主管-执行者模式、链式协作、辩论式架构
 */
@Slf4j
public class MultiAgentExample {

    /**
     * 代码审查智能体
     */
    public static class CodeReviewAgent implements Agent {
        
        @Override
        public String getName() {
            return "CodeReviewer";
        }

        @Override
        public String getDescription() {
            return "代码审查智能体 - 审查代码质量和潜在问题";
        }

        @Override
        public AgentResponse execute(AgentRequest request) {
            log.info("审查代码: {}", request.getQuery());
            return AgentResponse.builder()
                    .output("代码审查报告: 代码质量良好，建议优化异常处理")
                    .success(true)
                    .build();
        }

        @Override
        public boolean supports(String taskType) {
            return taskType.contains("code") || taskType.contains("代码");
        }
    }

    /**
     * 文档生成智能体
     */
    public static class DocGenAgent implements Agent {
        
        @Override
        public String getName() {
            return "DocGenerator";
        }

        @Override
        public String getDescription() {
            return "文档生成智能体 - 根据代码生成技术文档";
        }

        @Override
        public AgentResponse execute(AgentRequest request) {
            log.info("生成文档: {}", request.getQuery());
            
            // 模拟文档生成
            String doc = "## 技术文档\n\n根据代码分析...";
            
            return AgentResponse.builder()
                    .output(doc)
                    .success(true)
                    .build();
        }

        @Override
        public boolean supports(String taskType) {
            return taskType.contains("doc") || taskType.contains("文档");
        }
    }

    /**
     * 代码优化智能体
     */
    public static class CodeOptimizerAgent implements Agent {
        
        @Override
        public String getName() {
            return "CodeOptimizer";
        }

        @Override
        public String getDescription() {
            return "代码优化智能体 - 优化代码性能和可读性";
        }

        @Override
        public AgentResponse execute(AgentRequest request) {
            return AgentResponse.builder()
                    .output("优化建议: 建议使用缓存减少数据库查询")
                    .success(true)
                    .build();
        }

        @Override
        public boolean supports(String taskType) {
            return taskType.contains("optimize") || taskType.contains("优化");
        }
    }

    /**
     * 示例：链式协作模式
     * 代码审查 -> 文档生成
     */
    public static void chainExample() {
        log.info("=== 链式协作模式示例 ===");
        
        // 第一步：代码审查
        Agent codeReviewer = new CodeReviewAgent();
        AgentRequest request = AgentRequest.builder()
                .query("审查以下Java代码: public void process() { ... }")
                .taskType("code")
                .build();
        
        AgentResponse reviewResult = codeReviewer.execute(request);
        log.info("审查结果: {}", reviewResult.getOutput());
        
        // 第二步：基于审查结果生成文档
        Agent docGenerator = new DocGenAgent();
        AgentRequest docRequest = AgentRequest.builder()
                .query("根据以下审查报告生成文档: " + reviewResult.getOutput())
                .taskType("doc")
                .build();
        
        AgentResponse docResult = docGenerator.execute(docRequest);
        log.info("文档结果: {}", docResult.getOutput());
    }

    public static void main(String[] args) {
        log.info("多智能体协作示例");
        log.info("注意：实际运行需要Spring环境和LLM配置");
        
        chainExample();
    }
}
