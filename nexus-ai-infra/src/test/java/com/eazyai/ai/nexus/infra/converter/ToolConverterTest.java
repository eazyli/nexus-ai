package com.eazyai.ai.nexus.infra.converter;

import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolVisibility;
import com.eazyai.ai.nexus.api.tool.flow.FlowDefinition;
import com.eazyai.ai.nexus.api.tool.flow.FlowStep;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolConverter 单元测试 - 流程工具支持
 */
class ToolConverterTest {

    private ToolConverter toolConverter;

    @BeforeEach
    void setUp() {
        toolConverter = new ToolConverter();
    }

    @Nested
    @DisplayName("原子工具转换测试")
    class AtomicToolConversionTests {

        @Test
        @DisplayName("实体转描述符 - HTTP工具")
        void testEntityToDescriptorHttpTool() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolId("tool-http-001");
            entity.setToolName("HTTP请求工具");
            entity.setToolType("HTTP");
            entity.setDescription("发送HTTP请求");
            entity.setAppId("app001");
            entity.setVisibility("PRIVATE");
            entity.setStatus(1);
            entity.setTimeout(30000);
            entity.setRetryTimes(3);
            
            Map<String, Object> config = new HashMap<>();
            config.put("url", "https://api.example.com");
            config.put("method", "POST");
            entity.setConfig(config);

            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);

            assertNotNull(descriptor);
            assertEquals("tool-http-001", descriptor.getToolId());
            assertEquals("HTTP请求工具", descriptor.getName());
            assertEquals("HTTP", descriptor.getExecutorType().toUpperCase());
            assertEquals(ToolDescriptor.ToolType.ATOMIC, descriptor.getToolType());
            assertTrue(descriptor.isEnabled());
            assertNull(descriptor.getFlowDefinition());
        }

        @Test
        @DisplayName("描述符转实体 - 原子工具")
        void testDescriptorToEntityAtomicTool() {
            ToolDescriptor descriptor = ToolDescriptor.builder()
                .toolId("tool-db-001")
                .name("数据库查询工具")
                .executorType("DB")
                .toolType(ToolDescriptor.ToolType.ATOMIC)
                .description("执行SQL查询")
                .visibility(ToolVisibility.PUBLIC)
                .enabled(true)
                .timeout(60000L)
                .build();

            AiMcpTool entity = toolConverter.toEntity(descriptor);

            assertNotNull(entity);
            assertEquals("tool-db-001", entity.getToolId());
            assertEquals("数据库查询工具", entity.getToolName());
            assertEquals("DB", entity.getToolType());
            assertEquals(1, entity.getStatus());
            assertNull(entity.getConfig().get("flowDefinition"));
        }
    }

    @Nested
    @DisplayName("流程工具转换测试")
    class FlowToolConversionTests {

        @Test
        @DisplayName("实体转描述符 - 流程工具")
        void testEntityToDescriptorFlowTool() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolId("flow-001");
            entity.setToolName("用户订单查询流程");
            entity.setToolType("FLOW");
            entity.setDescription("查询用户信息并获取订单列表");
            entity.setVisibility("PUBLIC");
            entity.setStatus(1);

            // 构建流程定义
            Map<String, Object> flowDefMap = new HashMap<>();
            flowDefMap.put("type", "SEQUENTIAL");
            
            List<Map<String, Object>> steps = new ArrayList<>();
            
            Map<String, Object> step1 = new HashMap<>();
            step1.put("stepId", "step1");
            step1.put("name", "获取用户信息");
            step1.put("toolId", "tool-get-user");
            step1.put("inputMappings", Map.of("userId", "${input.userId}"));
            step1.put("outputVariable", "userInfo");
            steps.add(step1);
            
            Map<String, Object> step2 = new HashMap<>();
            step2.put("stepId", "step2");
            step2.put("name", "查询订单");
            step2.put("toolId", "tool-get-orders");
            step2.put("inputMappings", Map.of("userId", "${userInfo.id}"));
            step2.put("outputVariable", "orders");
            steps.add(step2);
            
            flowDefMap.put("steps", steps);
            
            Map<String, Object> config = new HashMap<>();
            config.put("flowDefinition", flowDefMap);
            entity.setConfig(config);

            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);

            assertNotNull(descriptor);
            assertEquals("flow-001", descriptor.getToolId());
            assertEquals(ToolDescriptor.ToolType.FLOW, descriptor.getToolType());
            assertEquals("flow", descriptor.getExecutorType());
            assertNotNull(descriptor.getFlowDefinition());
            assertEquals(FlowDefinition.FlowType.SEQUENTIAL, descriptor.getFlowDefinition().getType());
            assertEquals(2, descriptor.getFlowDefinition().getSteps().size());
        }

        @Test
        @DisplayName("描述符转实体 - 流程工具")
        void testDescriptorToEntityFlowTool() {
            // 构建流程定义
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.SEQUENTIAL)
                    .steps(List.of(
                            FlowStep.builder()
                                    .stepId("extract")
                                    .name("数据提取")
                                    .toolId("tool-db-query")
                                    .build(),
                            FlowStep.builder()
                                    .stepId("transform")
                                    .name("数据转换")
                                    .toolId("tool-transform")
                                    .build(),
                            FlowStep.builder()
                                    .stepId("load")
                                    .name("数据加载")
                                    .toolId("tool-db-insert")
                                    .build()
                    ))
                    .build();

            ToolDescriptor descriptor = ToolDescriptor.builder()
                .toolId("flow-002")
                .name("数据同步流程")
                .toolType(ToolDescriptor.ToolType.FLOW)
                .description("ETL数据同步流程")
                .visibility(ToolVisibility.PRIVATE)
                .flowDefinition(flowDef)
                .enabled(true)
                .build();

            AiMcpTool entity = toolConverter.toEntity(descriptor);

            assertNotNull(entity);
            assertEquals("flow-002", entity.getToolId());
            assertEquals("数据同步流程", entity.getToolName());
            assertEquals("FLOW", entity.getToolType());
            assertEquals(1, entity.getStatus());
            assertNotNull(entity.getConfig());
            assertNotNull(entity.getConfig().get("flowDefinition"));
        }

        @Test
        @DisplayName("复杂流程定义转换")
        void testComplexFlowConversion() {
            // 构建包含条件分支和循环的复杂流程
            FlowDefinition flowDef = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.HYBRID)
                    .steps(List.of(
                            // 步骤1: 串行
                            FlowStep.builder()
                                    .stepId("init")
                                    .toolId("tool-init")
                                    .outputVariable("initData")
                                    .build(),
                            // 步骤2: 条件分支
                            FlowStep.builder()
                                    .stepId("condition")
                                    .branches(List.of(
                                            FlowStep.ConditionBranch.builder()
                                                    .name("processA")
                                                    .condition("${initData.type == 'A'}")
                                                    .subSteps(List.of(
                                                            FlowStep.builder()
                                                                    .stepId("processA")
                                                                    .toolId("tool-process-a")
                                                                    .build()
                                                    ))
                                                    .build(),
                                            FlowStep.ConditionBranch.builder()
                                                    .name("processB")
                                                    .condition("${initData.type == 'B'}")
                                                    .subSteps(List.of(
                                                            FlowStep.builder()
                                                                    .stepId("processB")
                                                                    .toolId("tool-process-b")
                                                                    .build()
                                                    ))
                                                    .build()
                                    ))
                                    .build(),
                            // 步骤3: 循环
                            FlowStep.builder()
                                    .stepId("batchProcess")
                                    .loopConfig(FlowStep.LoopConfig.builder()
                                            .type(FlowStep.LoopConfig.LoopType.FOR_EACH)
                                            .collectionExpression("${initData.items}")
                                            .itemVariable("item")
                                            .loopBody(List.of(
                                                    FlowStep.builder()
                                                            .stepId("process")
                                                            .toolId("tool-process-item")
                                                            .build()
                                            ))
                                            .build())
                                    .build()
                    ))
                    .build();

            ToolDescriptor descriptor = ToolDescriptor.builder()
                .toolId("flow-complex")
                .name("复杂流程")
                .toolType(ToolDescriptor.ToolType.FLOW)
                .flowDefinition(flowDef)
                .build();

            // 转换为实体
            AiMcpTool entity = toolConverter.toEntity(descriptor);
            
            // 再转换回描述符
            ToolDescriptor backDescriptor = toolConverter.toDescriptor(entity);

            assertNotNull(backDescriptor.getFlowDefinition());
            assertEquals(FlowDefinition.FlowType.HYBRID, backDescriptor.getFlowDefinition().getType());
            assertEquals(3, backDescriptor.getFlowDefinition().getSteps().size());
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空实体转换")
        void testNullEntity() {
            ToolDescriptor descriptor = toolConverter.toDescriptor(null);
            assertNull(descriptor);
        }

        @Test
        @DisplayName("空描述符转换")
        void testNullDescriptor() {
            AiMcpTool entity = toolConverter.toEntity(null);
            assertNull(entity);
        }

        @Test
        @DisplayName("无配置的流程工具")
        void testFlowToolWithoutConfig() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolId("flow-no-config");
            entity.setToolType("FLOW");
            entity.setConfig(null);

            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);

            assertEquals(ToolDescriptor.ToolType.FLOW, descriptor.getToolType());
            assertNull(descriptor.getFlowDefinition());
        }

        @Test
        @DisplayName("流程定义为空的流程工具")
        void testFlowToolWithoutFlowDefinition() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolId("flow-empty");
            entity.setToolType("FLOW");
            entity.setConfig(new HashMap<>());

            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);

            assertEquals(ToolDescriptor.ToolType.FLOW, descriptor.getToolType());
            assertNull(descriptor.getFlowDefinition());
        }

        @Test
        @DisplayName("无效的流程定义JSON")
        void testInvalidFlowDefinitionJson() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolId("flow-invalid");
            entity.setToolType("FLOW");
            
            Map<String, Object> config = new HashMap<>();
            config.put("flowDefinition", "invalid json string");
            entity.setConfig(config);

            // 不应该抛出异常，flowDefinition 应该为 null
            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
            
            assertNotNull(descriptor);
            assertEquals(ToolDescriptor.ToolType.FLOW, descriptor.getToolType());
        }
    }

    @Nested
    @DisplayName("工具类型解析测试")
    class ToolTypeParsingTests {

        @Test
        @DisplayName("解析HTTP类型")
        void testParseHttpToolType() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolType("HTTP");
            
            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
            
            assertEquals(ToolDescriptor.ToolType.ATOMIC, descriptor.getToolType());
        }

        @Test
        @DisplayName("解析FLOW类型（大写）")
        void testParseFlowToolTypeUppercase() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolType("FLOW");
            
            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
            
            assertEquals(ToolDescriptor.ToolType.FLOW, descriptor.getToolType());
        }

        @Test
        @DisplayName("解析flow类型（小写）")
        void testParseFlowToolTypeLowercase() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolType("flow");
            
            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
            
            assertEquals(ToolDescriptor.ToolType.FLOW, descriptor.getToolType());
        }

        @Test
        @DisplayName("解析空类型 - 默认为ATOMIC")
        void testParseEmptyToolType() {
            AiMcpTool entity = new AiMcpTool();
            entity.setToolType(null);
            
            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
            
            assertEquals(ToolDescriptor.ToolType.ATOMIC, descriptor.getToolType());
        }
    }

    @Nested
    @DisplayName("可见性和权限转换测试")
    class VisibilityAndPermissionTests {

        @Test
        @DisplayName("PUBLIC可见性转换")
        void testPublicVisibility() {
            AiMcpTool entity = new AiMcpTool();
            entity.setVisibility("PUBLIC");
            
            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
            
            assertEquals(ToolVisibility.PUBLIC, descriptor.getVisibility());
        }

        @Test
        @DisplayName("SHARED可见性转换")
        void testSharedVisibility() {
            AiMcpTool entity = new AiMcpTool();
            entity.setVisibility("SHARED");
            entity.setPermissionApps("app1,app2,app3");
            
            ToolDescriptor descriptor = toolConverter.toDescriptor(entity);
            
            assertEquals(ToolVisibility.SHARED, descriptor.getVisibility());
            assertEquals(3, descriptor.getAuthorizedApps().size());
            assertTrue(descriptor.getAuthorizedApps().contains("app1"));
        }

        @Test
        @DisplayName("流程工具可见性设置")
        void testFlowToolVisibility() {
            ToolDescriptor descriptor = ToolDescriptor.builder()
                .toolType(ToolDescriptor.ToolType.FLOW)
                .visibility(ToolVisibility.SHARED)
                .authorizedApps(List.of("app1", "app2"))
                .build();

            AiMcpTool entity = toolConverter.toEntity(descriptor);

            assertEquals("SHARED", entity.getVisibility());
            assertEquals("app1,app2", entity.getPermissionApps());
        }
    }
}
