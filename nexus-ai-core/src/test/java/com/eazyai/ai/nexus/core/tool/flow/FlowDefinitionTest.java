package com.eazyai.ai.nexus.core.tool.flow;

import com.eazyai.ai.nexus.api.tool.flow.FlowDefinition;
import com.eazyai.ai.nexus.api.tool.flow.FlowStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowDefinition 模型测试
 */
class FlowDefinitionTest {

    @Nested
    @DisplayName("流程定义构建测试")
    class FlowDefinitionBuilderTests {

        @Test
        @DisplayName("构建串行流程")
        void testBuildSequentialFlow() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setType(FlowDefinition.FlowType.SEQUENTIAL);
            
            List<FlowStep> steps = new ArrayList<>();
            
            FlowStep step1 = FlowStep.builder()
                .stepId("getUser")
                .name("获取用户信息")
                .toolId("tool-get-user")
                .inputMappings(Map.of("userId", "${input.userId}"))
                .outputVariable("user")
                .build();
            steps.add(step1);
            
            FlowStep step2 = FlowStep.builder()
                .stepId("getOrders")
                .name("查询订单")
                .toolId("tool-get-orders")
                .inputMappings(Map.of("userId", "${user.id}"))
                .outputVariable("orders")
                .build();
            steps.add(step2);
            
            flowDef.setSteps(steps);
            flowDef.setVariableMappings(Map.of(
                "userInfo", "${user}",
                "orderList", "${orders}"
            ));
            
            // 验证
            assertEquals(FlowDefinition.FlowType.SEQUENTIAL, flowDef.getType());
            assertEquals(2, flowDef.getSteps().size());
            assertNotNull(flowDef.getVariableMappings());
        }

        @Test
        @DisplayName("构建并行流程")
        void testBuildParallelFlow() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setType(FlowDefinition.FlowType.PARALLEL);
            flowDef.setMaxConcurrency(5);
            
            List<FlowStep> steps = Arrays.asList(
                createStep("weather", "tool-weather"),
                createStep("stock", "tool-stock"),
                createStep("news", "tool-news")
            );
            flowDef.setSteps(steps);
            
            // 验证
            assertEquals(FlowDefinition.FlowType.PARALLEL, flowDef.getType());
            assertEquals(3, flowDef.getSteps().size());
            assertEquals(5, flowDef.getMaxConcurrency());
        }

        @Test
        @DisplayName("构建条件分支流程")
        void testBuildConditionalFlow() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setType(FlowDefinition.FlowType.CONDITIONAL);
            
            // 条件判断步骤
            FlowStep conditionStep = FlowStep.builder()
                .stepId("condition")
                .name("评分判断")
                .branches(Arrays.asList(
                    FlowStep.ConditionBranch.builder()
                        .name("优秀")
                        .condition("${score >= 90}")
                        .subSteps(List.of(createStep("excellent", "tool-reward-excellent")))
                        .build(),
                    FlowStep.ConditionBranch.builder()
                        .name("良好")
                        .condition("${score >= 80}")
                        .subSteps(List.of(createStep("good", "tool-reward-good")))
                        .build(),
                    FlowStep.ConditionBranch.builder()
                        .name("及格")
                        .condition("${score >= 60}")
                        .subSteps(List.of(createStep("pass", "tool-notify")))
                        .build()
                ))
                .build();
            
            flowDef.setSteps(List.of(conditionStep));
            
            // 验证
            assertEquals(FlowDefinition.FlowType.CONDITIONAL, flowDef.getType());
            assertEquals(1, flowDef.getSteps().size());
            assertNotNull(conditionStep.getBranches());
            assertEquals(3, conditionStep.getBranches().size());
        }

        @Test
        @DisplayName("构建循环流程")
        void testBuildLoopFlow() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setType(FlowDefinition.FlowType.LOOP);
            
            FlowStep loopStep = FlowStep.builder()
                .stepId("batchProcess")
                .name("批量处理")
                .loopConfig(FlowStep.LoopConfig.builder()
                    .type(FlowStep.LoopConfig.LoopType.FOR_EACH)
                    .collectionExpression("${input.items}")
                    .itemVariable("item")
                    .maxIterations(100)
                    .loopBody(List.of(
                        FlowStep.builder()
                            .stepId("processItem")
                            .toolId("tool-process")
                            .inputMappings(Map.of("data", "${item}"))
                            .build()
                    ))
                    .build())
                .build();
            
            flowDef.setSteps(List.of(loopStep));
            
            // 验证
            assertEquals(FlowDefinition.FlowType.LOOP, flowDef.getType());
            assertEquals(FlowStep.LoopConfig.LoopType.FOR_EACH, 
                loopStep.getLoopConfig().getType());
            assertEquals("item", loopStep.getLoopConfig().getItemVariable());
            assertEquals(1, loopStep.getLoopConfig().getLoopBody().size());
        }

        @Test
        @DisplayName("构建混合流程")
        void testBuildHybridFlow() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setType(FlowDefinition.FlowType.HYBRID);
            
            // 步骤1: 串行
            FlowStep step1 = createStep("init", "tool-init");
            step1.setOutputVariable("initData");
            
            // 步骤2: 条件分支
            FlowStep conditionStep = FlowStep.builder()
                .stepId("condition")
                .branches(Arrays.asList(
                    FlowStep.ConditionBranch.builder()
                        .name("类型A")
                        .condition("${initData.type == 'A'}")
                        .subSteps(List.of(createStep("processA", "tool-process-a")))
                        .build(),
                    FlowStep.ConditionBranch.builder()
                        .name("类型B")
                        .condition("${initData.type == 'B'}")
                        .subSteps(List.of(createStep("processB", "tool-process-b")))
                        .build()
                ))
                .build();
            
            // 步骤3: 循环
            FlowStep loopStep = FlowStep.builder()
                .stepId("batchProcess")
                .loopConfig(FlowStep.LoopConfig.builder()
                    .type(FlowStep.LoopConfig.LoopType.FOR_EACH)
                    .collectionExpression("${initData.items}")
                    .itemVariable("item")
                    .loopBody(List.of(createStep("processItem", "tool-process")))
                    .build())
                .build();
            
            flowDef.setSteps(Arrays.asList(step1, conditionStep, loopStep));
            
            // 验证
            assertEquals(FlowDefinition.FlowType.HYBRID, flowDef.getType());
            assertEquals(3, flowDef.getSteps().size());
        }
    }

    @Nested
    @DisplayName("流程验证测试")
    class FlowValidationTests {

        @Test
        @DisplayName("验证有效流程")
        void testValidateValidFlow() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setType(FlowDefinition.FlowType.SEQUENTIAL);
            flowDef.setSteps(List.of(createStep("step1", "tool1")));
            
            // 基本验证
            assertNotNull(flowDef.getType());
            assertFalse(flowDef.getSteps().isEmpty());
        }

        @Test
        @DisplayName("验证必需字段")
        void testRequiredFields() {
            FlowStep step = FlowStep.builder()
                .stepId("step1")
                .toolId("tool1")
                .build();
            
            // 验证必需字段
            assertNotNull(step.getStepId());
            assertNotNull(step.getToolId());
        }
    }

    @Nested
    @DisplayName("失败策略测试")
    class FailureStrategyTests {

        @Test
        @DisplayName("默认失败策略为 STOP")
        void testDefaultFailureStrategy() {
            FlowDefinition flowDef = new FlowDefinition();
            
            assertEquals(FlowDefinition.FailureStrategy.STOP, flowDef.getFailureStrategy());
        }

        @Test
        @DisplayName("设置 CONTINUE 策略")
        void testContinueStrategy() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setFailureStrategy(FlowDefinition.FailureStrategy.CONTINUE);
            
            assertEquals(FlowDefinition.FailureStrategy.CONTINUE, flowDef.getFailureStrategy());
        }

        @Test
        @DisplayName("设置 ROLLBACK 策略")
        void testRollbackStrategy() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setFailureStrategy(FlowDefinition.FailureStrategy.ROLLBACK);
            
            assertEquals(FlowDefinition.FailureStrategy.ROLLBACK, flowDef.getFailureStrategy());
        }
    }

    @Nested
    @DisplayName("超时配置测试")
    class TimeoutConfigTests {

        @Test
        @DisplayName("默认超时为 120 秒")
        void testDefaultTimeout() {
            FlowDefinition flowDef = new FlowDefinition();
            
            assertEquals(120000L, flowDef.getTimeout());
        }

        @Test
        @DisplayName("自定义流程超时")
        void testCustomFlowTimeout() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setTimeout(300000L); // 5分钟
            
            assertEquals(300000L, flowDef.getTimeout());
        }
    }

    @Nested
    @DisplayName("并发配置测试")
    class ConcurrencyConfigTests {

        @Test
        @DisplayName("默认最大并发数为 10")
        void testDefaultMaxConcurrency() {
            FlowDefinition flowDef = new FlowDefinition();
            
            assertEquals(10, flowDef.getMaxConcurrency());
        }

        @Test
        @DisplayName("自定义最大并发数")
        void testCustomMaxConcurrency() {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setMaxConcurrency(20);
            
            assertEquals(20, flowDef.getMaxConcurrency());
        }
    }

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderPatternTests {

        @Test
        @DisplayName("使用 Builder 构建流程定义")
        void testBuilderPattern() {
            FlowDefinition flowDef = FlowDefinition.builder()
                .type(FlowDefinition.FlowType.SEQUENTIAL)
                .timeout(60000L)
                .maxConcurrency(5)
                .failureStrategy(FlowDefinition.FailureStrategy.CONTINUE)
                .steps(List.of(
                    FlowStep.builder()
                        .stepId("step1")
                        .toolId("tool1")
                        .build()
                ))
                .variableMappings(Map.of("result", "${step1.output}"))
                .build();
            
            assertEquals(FlowDefinition.FlowType.SEQUENTIAL, flowDef.getType());
            assertEquals(60000L, flowDef.getTimeout());
            assertEquals(5, flowDef.getMaxConcurrency());
            assertEquals(FlowDefinition.FailureStrategy.CONTINUE, flowDef.getFailureStrategy());
            assertEquals(1, flowDef.getSteps().size());
            assertEquals(1, flowDef.getVariableMappings().size());
        }
    }

    // ==================== 辅助方法 ====================
    
    private FlowStep createStep(String stepId, String toolId) {
        return FlowStep.builder()
            .stepId(stepId)
            .toolId(toolId)
            .build();
    }
}
