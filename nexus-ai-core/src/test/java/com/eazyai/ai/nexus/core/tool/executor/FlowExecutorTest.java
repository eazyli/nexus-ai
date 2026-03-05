package com.eazyai.ai.nexus.core.tool.executor;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.eazyai.ai.nexus.api.tool.flow.FlowDefinition;
import com.eazyai.ai.nexus.api.tool.flow.FlowExecutionContext;
import com.eazyai.ai.nexus.api.tool.flow.FlowStep;
import com.eazyai.ai.nexus.core.tool.DefaultToolBus;
import com.eazyai.ai.nexus.core.tool.flow.ConditionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FlowExecutor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class FlowExecutorTest {

    @Mock
    private DefaultToolBus toolBus;

    @Mock
    private ConditionEngine conditionEngine;

    @InjectMocks
    private FlowExecutor flowExecutor;

    private AgentContext agentContext;

    @BeforeEach
    void setUp() {
        agentContext = AgentContext.builder()
                .appId("app-test")
                .sessionId("session-123")
                .userId("user-456")
                .build();
    }

    @Nested
    @DisplayName("串行执行测试")
    class SequentialExecutionTests {

        @Test
        @DisplayName("基本串行执行")
        void testBasicSequentialExecution() {
            // 准备流程定义
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.SEQUENTIAL)
                    .steps(Arrays.asList(
                            FlowStep.builder()
                                    .stepId("step1")
                                    .name("获取用户")
                                    .toolId("tool-get-user")
                                    .inputMappings(Map.of("userId", "${input.userId}"))
                                    .outputVariable("userInfo")
                                    .build(),
                            FlowStep.builder()
                                    .stepId("step2")
                                    .name("查询订单")
                                    .toolId("tool-get-orders")
                                    .inputMappings(Map.of("userId", "${userInfo.id}"))
                                    .outputVariable("orders")
                                    .build()
                    ))
                    .build();

            // 准备工具描述符
            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-test")
                    .name("测试流程")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            // Mock 工具执行
            ToolResult userResult = ToolResult.builder()
                    .toolId("tool-get-user")
                    .success(true)
                    .data(Map.of("id", "user123", "name", "张三"))
                    .build();
            
            ToolResult orderResult = ToolResult.builder()
                    .toolId("tool-get-orders")
                    .success(true)
                    .data(Map.of("count", 5, "total", 1000.0))
                    .build();

            when(toolBus.invoke(eq("tool-get-user"), anyMap(), any()))
                    .thenReturn(userResult);
            when(toolBus.invoke(eq("tool-get-orders"), anyMap(), any()))
                    .thenReturn(orderResult);
            
            // Mock 条件引擎
            when(conditionEngine.resolveVariable(anyString(), any(FlowExecutionContext.class)))
                    .thenAnswer(inv -> {
                        String expr = inv.getArgument(0);
                        if (expr.contains("userId")) return "user123";
                        if (expr.contains("userInfo.id")) return "user123";
                        return null;
                    });

            // 执行
            Map<String, Object> input = Map.of("userId", "user123");
            ToolResult result = flowExecutor.execute(flowTool, input, agentContext);
            
            // 验证
            assertTrue(result.isSuccess());
            verify(toolBus, times(1)).invoke(eq("tool-get-user"), anyMap(), any());
            verify(toolBus, times(1)).invoke(eq("tool-get-orders"), anyMap(), any());
        }

        @Test
        @DisplayName("串行执行关键步骤失败后停止")
        void testSequentialExecutionStopsOnCriticalFailure() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.SEQUENTIAL)
                    .steps(Arrays.asList(
                            FlowStep.builder()
                                    .stepId("step1")
                                    .toolId("tool-fail")
                                    .isCritical(true)
                                    .build(),
                            FlowStep.builder()
                                    .stepId("step2")
                                    .toolId("tool-success")
                                    .build()
                    ))
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-fail-test")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            // Mock 第一个工具执行失败
            ToolResult failedResult = ToolResult.builder()
                    .toolId("tool-fail")
                    .success(false)
                    .errorCode("EXECUTION_ERROR")
                    .errorMessage("执行失败")
                    .build();
            
            when(toolBus.invoke(eq("tool-fail"), anyMap(), any()))
                    .thenReturn(failedResult);

            // 执行
            ToolResult result = flowExecutor.execute(flowTool, Map.of(), agentContext);
            
            // 验证
            assertFalse(result.isSuccess());
            assertEquals("FLOW_EXECUTION_ERROR", result.getErrorCode());
            
            // 验证第二个工具没有被调用
            verify(toolBus, never()).invoke(eq("tool-success"), anyMap(), any());
        }

        @Test
        @DisplayName("非关键步骤失败继续执行")
        void testNonCriticalStepFailureContinues() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.SEQUENTIAL)
                    .steps(Arrays.asList(
                            FlowStep.builder()
                                    .stepId("step1")
                                    .toolId("tool-fail")
                                    .isCritical(false) // 非关键步骤
                                    .build(),
                            FlowStep.builder()
                                    .stepId("step2")
                                    .toolId("tool-success")
                                    .build()
                    ))
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-non-critical")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            ToolResult failedResult = ToolResult.builder()
                    .toolId("tool-fail")
                    .success(false)
                    .build();
            
            ToolResult successResult = ToolResult.builder()
                    .toolId("tool-success")
                    .success(true)
                    .build();

            when(toolBus.invoke(eq("tool-fail"), anyMap(), any()))
                    .thenReturn(failedResult);
            when(toolBus.invoke(eq("tool-success"), anyMap(), any()))
                    .thenReturn(successResult);

            // 执行
            ToolResult result = flowExecutor.execute(flowTool, Map.of(), agentContext);
            
            // 验证：非关键步骤失败不影响整体流程
            assertTrue(result.isSuccess());
            verify(toolBus, times(1)).invoke(eq("tool-success"), anyMap(), any());
        }
    }

    @Nested
    @DisplayName("并行执行测试")
    class ParallelExecutionTests {

        @Test
        @DisplayName("基本并行执行")
        void testBasicParallelExecution() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.PARALLEL)
                    .maxConcurrency(5)
                    .steps(Arrays.asList(
                            FlowStep.builder().stepId("step1").toolId("tool-weather").build(),
                            FlowStep.builder().stepId("step2").toolId("tool-stock").build(),
                            FlowStep.builder().stepId("step3").toolId("tool-news").build()
                    ))
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-parallel")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            // Mock 并行工具执行
            when(toolBus.invoke(eq("tool-weather"), anyMap(), any()))
                    .thenReturn(ToolResult.builder().toolId("tool-weather").success(true).data(Map.of("temp", 25)).build());
            when(toolBus.invoke(eq("tool-stock"), anyMap(), any()))
                    .thenReturn(ToolResult.builder().toolId("tool-stock").success(true).data(Map.of("price", 100.0)).build());
            when(toolBus.invoke(eq("tool-news"), anyMap(), any()))
                    .thenReturn(ToolResult.builder().toolId("tool-news").success(true).data(Map.of("headlines", List.of("news1"))).build());

            // 执行
            ToolResult result = flowExecutor.execute(flowTool, Map.of(), agentContext);
            
            // 验证
            assertTrue(result.isSuccess());
            verify(toolBus, times(1)).invoke(eq("tool-weather"), anyMap(), any());
            verify(toolBus, times(1)).invoke(eq("tool-stock"), anyMap(), any());
            verify(toolBus, times(1)).invoke(eq("tool-news"), anyMap(), any());
        }
    }

    @Nested
    @DisplayName("条件分支执行测试")
    class ConditionalExecutionTests {

        @Test
        @DisplayName("条件分支选择")
        void testConditionalBranchSelection() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.CONDITIONAL)
                    .steps(List.of(
                            FlowStep.builder()
                                    .stepId("condition")
                                    .branches(Arrays.asList(
                                            FlowStep.ConditionBranch.builder()
                                                    .name("优秀")
                                                    .condition("${score >= 90}")
                                                    .subSteps(List.of(
                                                            FlowStep.builder().stepId("reward").toolId("tool-reward").build()
                                                    ))
                                                    .priority(3)
                                                    .build(),
                                            FlowStep.ConditionBranch.builder()
                                                    .name("及格")
                                                    .condition("${score >= 60}")
                                                    .subSteps(List.of(
                                                            FlowStep.builder().stepId("notify").toolId("tool-notify").build()
                                                    ))
                                                    .priority(1)
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-conditional")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            // Mock 条件评估 - 选择"优秀"分支
            when(conditionEngine.evaluate(anyString(), any(FlowExecutionContext.class)))
                    .thenAnswer(inv -> {
                        ConditionEngine.ConditionResult result = mock(ConditionEngine.ConditionResult.class);
                        when(result.isSuccess()).thenReturn(true);
                        when(result.getValue()).thenReturn(true); // 第一个条件满足
                        return result;
                    });

            when(toolBus.invoke(eq("tool-reward"), anyMap(), any()))
                    .thenReturn(ToolResult.builder().toolId("tool-reward").success(true).build());

            // 执行
            ToolResult result = flowExecutor.execute(flowTool, Map.of("score", 95), agentContext);
            
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("循环执行测试")
    class LoopExecutionTests {

        @Test
        @DisplayName("FOR_EACH 循环")
        void testForEachLoop() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.LOOP)
                    .steps(List.of(
                            FlowStep.builder()
                                    .stepId("batchProcess")
                                    .loopConfig(FlowStep.LoopConfig.builder()
                                            .type(FlowStep.LoopConfig.LoopType.FOR_EACH)
                                            .collectionExpression("input.items")
                                            .itemVariable("item")
                                            .loopBody(List.of(
                                                    FlowStep.builder()
                                                            .stepId("process")
                                                            .toolId("tool-process")
                                                            .build()
                                            ))
                                            .maxIterations(10)
                                            .build())
                                    .build()
                    ))
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-loop")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            // 使用真实的 ConditionEngine 或者更简单的 mock
            when(conditionEngine.resolveVariable(anyString(), any(FlowExecutionContext.class)))
                    .thenAnswer(inv -> {
                        String expr = inv.getArgument(0);
                        FlowExecutionContext ctx = inv.getArgument(1);
                        // 从上下文获取 items
                        if (expr.equals("input.items") || expr.equals("${input.items}")) {
                            return ctx.getInput().get("items");
                        }
                        return null;
                    });

            when(toolBus.invoke(eq("tool-process"), anyMap(), any()))
                    .thenReturn(ToolResult.builder().toolId("tool-process").success(true).build());

            // 执行
            ToolResult result = flowExecutor.execute(flowTool, 
                    Map.of("items", List.of("a", "b", "c")), agentContext);
            
            assertTrue(result.isSuccess());
            // 验证循环执行了3次
            verify(toolBus, times(3)).invoke(eq("tool-process"), anyMap(), any());
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("缺少流程定义")
        void testMissingFlowDefinition() {
            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-no-def")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    // 没有 flowDefinition
                    .build();

            ToolResult result = flowExecutor.execute(flowTool, Map.of(), agentContext);
            
            assertFalse(result.isSuccess());
            assertEquals("FLOW_DEFINITION_MISSING", result.getErrorCode());
        }

        @Test
        @DisplayName("空步骤列表")
        void testEmptySteps() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.SEQUENTIAL)
                    .steps(Collections.emptyList())
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-empty")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            ToolResult result = flowExecutor.execute(flowTool, Map.of(), agentContext);
            
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("工具不存在")
        void testToolNotFound() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.SEQUENTIAL)
                    .steps(List.of(
                            FlowStep.builder().stepId("step1").toolId("non-existent-tool").build()
                    ))
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-test")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            when(toolBus.invoke(eq("non-existent-tool"), anyMap(), any()))
                    .thenReturn(ToolResult.error("non-existent-tool", "TOOL_NOT_FOUND", "工具不存在"));

            ToolResult result = flowExecutor.execute(flowTool, Map.of(), agentContext);
            
            // 关键步骤失败应该返回错误
            assertFalse(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("参数映射测试")
    class ParameterMappingTests {

        @Test
        @DisplayName("动态参数映射")
        void testDynamicParameterMapping() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.SEQUENTIAL)
                    .steps(List.of(
                            FlowStep.builder()
                                    .stepId("step1")
                                    .toolId("tool-api")
                                    .inputMappings(Map.of(
                                            "userId", "${input.userId}",
                                            "action", "query"
                                    ))
                                    .build()
                    ))
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-mapping")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            when(conditionEngine.resolveVariable(eq("input.userId"), any(FlowExecutionContext.class)))
                    .thenReturn("user123");
            
            when(toolBus.invoke(eq("tool-api"), anyMap(), any()))
                    .thenReturn(ToolResult.builder().toolId("tool-api").success(true).build());

            Map<String, Object> input = Map.of("userId", "user123");
            flowExecutor.execute(flowTool, input, agentContext);

            // 验证参数映射
            verify(toolBus).invoke(eq("tool-api"), argThat(params -> 
                    params.containsKey("userId") && 
                    params.containsKey("action") &&
                    "query".equals(params.get("action"))
            ), any());
        }

        @Test
        @DisplayName("输出变量映射")
        void testOutputVariableMapping() {
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.SEQUENTIAL)
                    .steps(List.of(
                            FlowStep.builder()
                                    .stepId("step1")
                                    .toolId("tool-user")
                                    .outputVariable("userInfo")
                                    .build(),
                            FlowStep.builder()
                                    .stepId("step2")
                                    .toolId("tool-orders")
                                    .inputMappings(Map.of("userId", "${userInfo.id}"))
                                    .build()
                    ))
                    .variableMappings(Map.of("userName", "${userInfo.name}"))
                    .build();

            ToolDescriptor flowTool = ToolDescriptor.builder()
                    .toolId("flow-output-mapping")
                    .toolType(ToolDescriptor.ToolType.FLOW)
                    .flowDefinition(flowDef)
                    .build();

            Map<String, Object> userData = Map.of("id", "123", "name", "张三");
            
            when(toolBus.invoke(eq("tool-user"), anyMap(), any()))
                    .thenReturn(ToolResult.builder().toolId("tool-user").success(true).data(userData).build());
            when(toolBus.invoke(eq("tool-orders"), anyMap(), any()))
                    .thenReturn(ToolResult.builder().toolId("tool-orders").success(true).build());
            
            when(conditionEngine.resolveVariable(eq("userInfo.id"), any(FlowExecutionContext.class)))
                    .thenReturn("123");
            when(conditionEngine.resolveVariable(eq("userInfo.name"), any(FlowExecutionContext.class)))
                    .thenReturn("张三");

            ToolResult result = flowExecutor.execute(flowTool, Map.of("userId", "123"), agentContext);

            assertTrue(result.isSuccess());
        }
    }
}
