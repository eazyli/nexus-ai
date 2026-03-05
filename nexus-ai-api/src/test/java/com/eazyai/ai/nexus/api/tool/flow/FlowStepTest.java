package com.eazyai.ai.nexus.api.tool.flow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowStep 模型测试
 */
class FlowStepTest {

    @Nested
    @DisplayName("步骤构建测试")
    class StepBuilderTests {

        @Test
        @DisplayName("构建基本步骤")
        void testBuildBasicStep() {
            FlowStep step = new FlowStep();
            step.setStepId("step1");
            step.setName("获取用户信息");
            step.setToolId("tool-get-user");
            
            assertEquals("step1", step.getStepId());
            assertEquals("获取用户信息", step.getName());
            assertEquals("tool-get-user", step.getToolId());
        }

        @Test
        @DisplayName("构建带输入映射的步骤")
        void testBuildStepWithInputMappings() {
            FlowStep step = new FlowStep();
            step.setStepId("step1");
            step.setToolId("tool-api");
            step.setInputMappings(Map.of(
                "userId", "${input.userId}",
                "userName", "${input.user.name}",
                "action", "query"
            ));
            
            assertNotNull(step.getInputMappings());
            assertEquals(3, step.getInputMappings().size());
            assertEquals("${input.userId}", step.getInputMappings().get("userId"));
        }

        @Test
        @DisplayName("构建带输出变量的步骤")
        void testBuildStepWithOutputVariable() {
            FlowStep step = new FlowStep();
            step.setStepId("step1");
            step.setOutputVariable("userInfo");
            
            assertEquals("userInfo", step.getOutputVariable());
        }
    }

    @Nested
    @DisplayName("重试配置测试")
    class RetryConfigTests {

        @Test
        @DisplayName("基本重试配置")
        void testBasicRetryConfig() {
            FlowStep step = new FlowStep();
            step.setRetryCount(3);
            step.setRetryInterval(1000L);
            
            assertEquals(3, step.getRetryCount());
            assertEquals(1000L, step.getRetryInterval());
        }

        @Test
        @DisplayName("默认重试配置")
        void testDefaultRetryConfig() {
            FlowStep step = new FlowStep();
            
            assertEquals(0, step.getRetryCount());
            assertEquals(1000L, step.getRetryInterval());
        }
    }

    @Nested
    @DisplayName("循环配置测试")
    class LoopConfigTests {

        @Test
        @DisplayName("FOR_EACH 循环配置")
        void testForEachLoopConfig() {
            FlowStep.LoopConfig loopConfig = new FlowStep.LoopConfig();
            loopConfig.setType(FlowStep.LoopConfig.LoopType.FOR_EACH);
            loopConfig.setCollectionExpression("${input.items}");
            loopConfig.setItemVariable("item");
            loopConfig.setIndexVariable("index");
            loopConfig.setMaxIterations(100);
            
            assertEquals(FlowStep.LoopConfig.LoopType.FOR_EACH, loopConfig.getType());
            assertEquals("${input.items}", loopConfig.getCollectionExpression());
            assertEquals("item", loopConfig.getItemVariable());
            assertEquals(100, loopConfig.getMaxIterations());
        }

        @Test
        @DisplayName("WHILE 循环配置")
        void testWhileLoopConfig() {
            FlowStep.LoopConfig loopConfig = new FlowStep.LoopConfig();
            loopConfig.setType(FlowStep.LoopConfig.LoopType.WHILE);
            loopConfig.setMaxIterations(50);
            
            assertEquals(FlowStep.LoopConfig.LoopType.WHILE, loopConfig.getType());
        }

        @Test
        @DisplayName("FOR_COUNT 循环配置")
        void testForCountLoopConfig() {
            FlowStep.LoopConfig loopConfig = new FlowStep.LoopConfig();
            loopConfig.setType(FlowStep.LoopConfig.LoopType.FOR_COUNT);
            loopConfig.setIndexVariable("i");
            
            assertEquals(FlowStep.LoopConfig.LoopType.FOR_COUNT, loopConfig.getType());
        }

        @Test
        @DisplayName("循环体步骤")
        void testLoopBodySteps() {
            FlowStep loopStep = new FlowStep();
            loopStep.setStepId("batchProcess");
            
            FlowStep.LoopConfig loopConfig = new FlowStep.LoopConfig();
            loopConfig.setType(FlowStep.LoopConfig.LoopType.FOR_EACH);
            loopConfig.setCollectionExpression("${input.items}");
            
            // 循环体步骤
            FlowStep bodyStep1 = new FlowStep();
            bodyStep1.setStepId("validate");
            bodyStep1.setToolId("tool-validate");
            
            FlowStep bodyStep2 = new FlowStep();
            bodyStep2.setStepId("save");
            bodyStep2.setToolId("tool-save");
            
            loopConfig.setLoopBody(List.of(bodyStep1, bodyStep2));
            loopStep.setLoopConfig(loopConfig);
            
            assertEquals(2, loopStep.getLoopConfig().getLoopBody().size());
        }
    }

    @Nested
    @DisplayName("条件分支配置测试")
    class ConditionBranchTests {

        @Test
        @DisplayName("条件分支配置")
        void testConditionBranches() {
            FlowStep step = new FlowStep();
            step.setStepId("scoreCheck");
            
            FlowStep.ConditionBranch branch1 = FlowStep.ConditionBranch.builder()
                .name("优秀")
                .condition("${score >= 90}")
                .priority(3)
                .build();
            
            FlowStep.ConditionBranch branch2 = FlowStep.ConditionBranch.builder()
                .name("良好")
                .condition("${score >= 80}")
                .priority(2)
                .build();
            
            FlowStep.ConditionBranch branch3 = FlowStep.ConditionBranch.builder()
                .name("及格")
                .condition("${score >= 60}")
                .priority(1)
                .build();
            
            step.setBranches(List.of(branch1, branch2, branch3));
            
            assertEquals(3, step.getBranches().size());
            assertEquals("优秀", step.getBranches().get(0).getName());
        }

        @Test
        @DisplayName("分支子步骤")
        void testBranchSubSteps() {
            FlowStep.ConditionBranch branch = FlowStep.ConditionBranch.builder()
                .name("高分处理")
                .condition("${score >= 90}")
                .subSteps(List.of(
                    FlowStep.builder().stepId("reward").toolId("tool-reward").build(),
                    FlowStep.builder().stepId("notify").toolId("tool-notify").build()
                ))
                .build();
            
            assertEquals(2, branch.getSubSteps().size());
            assertEquals("reward", branch.getSubSteps().get(0).getStepId());
        }
    }

    @Nested
    @DisplayName("超时配置测试")
    class TimeoutTests {

        @Test
        @DisplayName("步骤超时设置")
        void testStepTimeout() {
            FlowStep step = new FlowStep();
            step.setTimeout(30000L); // 30秒
            
            assertEquals(30000L, step.getTimeout());
        }

        @Test
        @DisplayName("超时单位验证")
        void testTimeoutUnit() {
            FlowStep step = new FlowStep();
            step.setTimeout(60000L); // 60秒 = 1分钟
            
            // 超时应该以毫秒为单位
            assertTrue(step.getTimeout() >= 1000);
        }
    }

    @Nested
    @DisplayName("关键步骤测试")
    class CriticalStepTests {

        @Test
        @DisplayName("默认为关键步骤")
        void testDefaultCritical() {
            FlowStep step = new FlowStep();
            
            assertTrue(step.getIsCritical());
        }

        @Test
        @DisplayName("设置为非关键步骤")
        void testNonCriticalStep() {
            FlowStep step = new FlowStep();
            step.setIsCritical(false);
            
            assertFalse(step.getIsCritical());
        }
    }

    @Nested
    @DisplayName("输出配置测试")
    class OutputConfigTests {

        @Test
        @DisplayName("输出变量配置")
        void testOutputVariable() {
            FlowStep step = new FlowStep();
            step.setOutputVariable("result");
            
            assertEquals("result", step.getOutputVariable());
        }

        @Test
        @DisplayName("JSONPath 输出提取")
        void testOutputJsonPath() {
            FlowStep step = new FlowStep();
            step.setOutputJsonPath("$.data.items");
            
            assertEquals("$.data.items", step.getOutputJsonPath());
        }
    }

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderPatternTests {

        @Test
        @DisplayName("使用 Builder 构建步骤")
        void testBuilderPattern() {
            FlowStep step = FlowStep.builder()
                .stepId("step1")
                .name("测试步骤")
                .toolId("tool-test")
                .inputMappings(Map.of("param", "${input.value}"))
                .outputVariable("output")
                .timeout(5000L)
                .retryCount(2)
                .retryInterval(500L)
                .isCritical(true)
                .build();
            
            assertEquals("step1", step.getStepId());
            assertEquals("测试步骤", step.getName());
            assertEquals("tool-test", step.getToolId());
            assertEquals(1, step.getInputMappings().size());
            assertEquals("output", step.getOutputVariable());
            assertEquals(5000L, step.getTimeout());
            assertEquals(2, step.getRetryCount());
            assertEquals(500L, step.getRetryInterval());
            assertTrue(step.getIsCritical());
        }
    }
}
