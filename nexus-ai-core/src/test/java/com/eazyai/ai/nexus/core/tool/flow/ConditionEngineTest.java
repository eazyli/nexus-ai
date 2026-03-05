package com.eazyai.ai.nexus.core.tool.flow;

import com.eazyai.ai.nexus.api.tool.flow.FlowExecutionContext;
import com.eazyai.ai.nexus.api.tool.flow.StepExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConditionEngine 单元测试
 */
class ConditionEngineTest {

    private ConditionEngine conditionEngine;
    private FlowExecutionContext context;

    @BeforeEach
    void setUp() {
        conditionEngine = new ConditionEngine();
        context = new FlowExecutionContext();
        
        // 准备测试数据
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "user123");
        input.put("score", 85);
        input.put("isActive", true);
        context.setInput(input);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("confidence", 0.92);
        variables.put("status", "pending");
        context.setVariables(variables);
    }

    @Nested
    @DisplayName("空表达式测试")
    class EmptyExpressionTests {

        @Test
        @DisplayName("空表达式返回 true")
        void testEmptyExpression() {
            ConditionEngine.ConditionResult result = conditionEngine.evaluate("", context);
            assertTrue(result.isSuccess());
            assertTrue(result.getValue());
        }

        @Test
        @DisplayName("null 表达式返回 true")
        void testNullExpression() {
            ConditionEngine.ConditionResult result = conditionEngine.evaluate(null, context);
            assertTrue(result.isSuccess());
            assertTrue(result.getValue());
        }

        @Test
        @DisplayName("空白表达式返回 true")
        void testBlankExpression() {
            ConditionEngine.ConditionResult result = conditionEngine.evaluate("   ", context);
            assertTrue(result.isSuccess());
            assertTrue(result.getValue());
        }
    }

    @Nested
    @DisplayName("条件结果测试")
    class ConditionResultTests {

        @Test
        @DisplayName("成功结果")
        void testSuccessResult() {
            ConditionEngine.ConditionResult result = ConditionEngine.ConditionResult.success(true, "测试消息");
            assertTrue(result.isSuccess());
            assertTrue(result.getValue());
            assertEquals("测试消息", result.getMessage());
        }

        @Test
        @DisplayName("失败结果")
        void testFailureResult() {
            ConditionEngine.ConditionResult result = ConditionEngine.ConditionResult.failure("错误消息");
            assertFalse(result.isSuccess());
            assertFalse(result.getValue());
            assertEquals("错误消息", result.getMessage());
        }
    }

    @Nested
    @DisplayName("上下文操作测试")
    class ContextOperationTests {

        @Test
        @DisplayName("获取变量")
        void testGetVariable() {
            Object value = context.getVariable("confidence");
            assertEquals(0.92, value);
        }

        @Test
        @DisplayName("获取不存在的变量返回 null")
        void testGetNonExistentVariable() {
            Object value = context.getVariable("nonExistent");
            assertNull(value);
        }

        @Test
        @DisplayName("获取变量带默认值")
        void testGetVariableWithDefault() {
            Object value = context.getVariable("nonExistent", "default");
            assertEquals("default", value);
        }

        @Test
        @DisplayName("设置变量")
        void testSetVariable() {
            context.setVariable("newVar", "test");
            assertEquals("test", context.getVariable("newVar"));
        }

        @Test
        @DisplayName("记录步骤结果")
        void testRecordStepResult() {
            StepExecutionResult result = StepExecutionResult.builder()
                    .stepId("step1")
                    .status(StepExecutionResult.StepStatus.COMPLETED)
                    .build();
            
            context.recordStepResult("step1", result);
            
            StepExecutionResult retrieved = context.getStepResult("step1");
            assertNotNull(retrieved);
            assertEquals("step1", retrieved.getStepId());
        }
    }

    @Nested
    @DisplayName("流程状态测试")
    class FlowStatusTests {

        @Test
        @DisplayName("初始状态为 PENDING")
        void testInitialStatus() {
            assertEquals(FlowExecutionContext.FlowStatus.PENDING, context.getStatus());
        }

        @Test
        @DisplayName("状态转换到 RUNNING")
        void testStatusTransitionToRunning() {
            context.setStatus(FlowExecutionContext.FlowStatus.RUNNING);
            assertEquals(FlowExecutionContext.FlowStatus.RUNNING, context.getStatus());
        }

        @Test
        @DisplayName("状态转换到 COMPLETED")
        void testStatusTransitionToCompleted() {
            context.setStatus(FlowExecutionContext.FlowStatus.COMPLETED);
            assertEquals(FlowExecutionContext.FlowStatus.COMPLETED, context.getStatus());
        }

        @Test
        @DisplayName("状态转换到 FAILED")
        void testStatusTransitionToFailed() {
            context.setStatus(FlowExecutionContext.FlowStatus.FAILED);
            assertEquals(FlowExecutionContext.FlowStatus.FAILED, context.getStatus());
        }
    }

    @Nested
    @DisplayName("步骤执行结果测试")
    class StepExecutionResultTests {

        @Test
        @DisplayName("COMPLETED 状态为成功")
        void testCompletedIsSuccess() {
            StepExecutionResult result = StepExecutionResult.builder()
                    .stepId("step1")
                    .status(StepExecutionResult.StepStatus.COMPLETED)
                    .build();
            
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("SKIPPED 状态为成功")
        void testSkippedIsSuccess() {
            StepExecutionResult result = StepExecutionResult.builder()
                    .stepId("step1")
                    .status(StepExecutionResult.StepStatus.SKIPPED)
                    .build();
            
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("FAILED 状态为失败")
        void testFailedIsNotSuccess() {
            StepExecutionResult result = StepExecutionResult.builder()
                    .stepId("step1")
                    .status(StepExecutionResult.StepStatus.FAILED)
                    .build();
            
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("PENDING 状态为未完成")
        void testPendingIsNotSuccess() {
            StepExecutionResult result = StepExecutionResult.builder()
                    .stepId("step1")
                    .status(StepExecutionResult.StepStatus.PENDING)
                    .build();
            
            assertFalse(result.isSuccess());
        }
    }
}
