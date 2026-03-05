package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolVisibility;
import com.eazyai.ai.nexus.api.tool.flow.FlowDefinition;
import com.eazyai.ai.nexus.api.tool.flow.FlowStep;
import com.eazyai.ai.nexus.web.dto.FlowToolRegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FlowController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdFlowId;

    @Test
    @Order(1)
    @DisplayName("创建流程工具")
    void testCreateFlowTool() throws Exception {
        FlowToolRegisterRequest request = new FlowToolRegisterRequest();
        request.setName("测试查询流程");
        request.setDescription("用户订单查询测试流程");
        request.setVisibility("PUBLIC");
        
        FlowDefinition flowDef = new FlowDefinition();
        flowDef.setFlowType(FlowDefinition.FlowType.SEQUENTIAL);
        
        List<FlowStep> steps = new ArrayList<>();
        
        FlowStep step1 = new FlowStep();
        step1.setStepId("step1");
        step1.setStepName("获取用户");
        step1.setToolId("tool-get-user");
        step1.setInputMappings(Map.of("userId", "${input.userId}"));
        step1.setOutputVariable("user");
        steps.add(step1);
        
        FlowStep step2 = new FlowStep();
        step2.setStepId("step2");
        step2.setStepName("查询订单");
        step2.setToolId("tool-get-orders");
        step2.setInputMappings(Map.of("userId", "${user.id}"));
        step2.setOutputVariable("orders");
        steps.add(step2);
        
        flowDef.setSteps(steps);
        flowDef.setOutputMappings(Map.of("userInfo", "${user}", "orderList", "${orders}"));
        
        request.setFlowDefinition(flowDef);
        
        List<Map<String, Object>> parameters = new ArrayList<>();
        parameters.add(Map.of(
            "name", "userId",
            "type", "string",
            "description", "用户ID",
            "required", true
        ));
        request.setParameters(parameters);

        MvcResult result = mockMvc.perform(post("/api/v1/tools/flows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.toolId").exists())
            .andExpect(jsonPath("$.data.name").value("测试查询流程"))
            .andExpect(jsonPath("$.data.toolType").value("FLOW"))
            .andReturn();

        // 保存创建的流程ID
        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        createdFlowId = (String) data.get("toolId");
    }

    @Test
    @Order(2)
    @DisplayName("获取流程工具列表")
    void testGetFlowToolList() throws Exception {
        mockMvc.perform(get("/api/v1/tools/flows")
                .param("page", "0")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("获取流程工具详情")
    void testGetFlowToolDetail() throws Exception {
        mockMvc.perform(get("/api/v1/tools/flows/{flowId}", createdFlowId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.toolId").value(createdFlowId))
            .andExpect(jsonPath("$.data.flowDefinition").exists());
    }

    @Test
    @Order(4)
    @DisplayName("验证流程定义 - 有效")
    void testValidateFlowDefinitionValid() throws Exception {
        FlowDefinition flowDef = new FlowDefinition();
        flowDef.setFlowType(FlowDefinition.FlowType.SEQUENTIAL);
        
        FlowStep step = new FlowStep();
        step.setStepId("step1");
        step.setToolId("tool-test");
        flowDef.setSteps(List.of(step));

        mockMvc.perform(post("/api/v1/tools/flows/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flowDef)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(5)
    @DisplayName("获取可用工具列表")
    void testGetAvailableTools() throws Exception {
        mockMvc.perform(get("/api/v1/tools/flows/available-tools"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("更新流程工具")
    void testUpdateFlowTool() throws Exception {
        FlowToolRegisterRequest request = new FlowToolRegisterRequest();
        request.setName("更新后的查询流程");
        request.setDescription("更新后的描述");
        
        FlowDefinition flowDef = new FlowDefinition();
        flowDef.setFlowType(FlowDefinition.FlowType.SEQUENTIAL);
        
        FlowStep step = new FlowStep();
        step.setStepId("step1");
        step.setToolId("tool-get-user");
        flowDef.setSteps(List.of(step));
        
        request.setFlowDefinition(flowDef);

        mockMvc.perform(put("/api/v1/tools/flows/{flowId}", createdFlowId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("更新后的查询流程"));
    }

    @Test
    @Order(7)
    @DisplayName("获取不存在的流程")
    void testGetNonExistentFlow() throws Exception {
        mockMvc.perform(get("/api/v1/tools/flows/{flowId}", "non-existent-flow-id"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(100)
    @DisplayName("删除流程工具")
    void testDeleteFlowTool() throws Exception {
        mockMvc.perform(delete("/api/v1/tools/flows/{flowId}", createdFlowId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // 验证已删除
        mockMvc.perform(get("/api/v1/tools/flows/{flowId}", createdFlowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Nested
    @DisplayName("流程定义验证测试")
    class FlowValidationTests {

        @Test
        @DisplayName("空步骤列表验证")
        void testValidateEmptySteps() throws Exception {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setFlowType(FlowDefinition.FlowType.SEQUENTIAL);
            flowDef.setSteps(Collections.emptyList());

            mockMvc.perform(post("/api/v1/tools/flows/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(flowDef)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("步骤缺少工具ID验证")
        void testValidateStepWithoutToolId() throws Exception {
            FlowDefinition flowDef = new FlowDefinition();
            flowDef.setFlowType(FlowDefinition.FlowType.SEQUENTIAL);
            
            FlowStep step = new FlowStep();
            step.setStepId("step1");
            // 不设置 toolId
            flowDef.setSteps(List.of(step));

            mockMvc.perform(post("/api/v1/tools/flows/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(flowDef)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("流程执行测试")
    class FlowExecutionTests {

        @Test
        @DisplayName("执行流程 - 缺少必需参数")
        void testExecuteFlowMissingParams() throws Exception {
            Map<String, Object> input = new HashMap<>();
            // 不提供 userId

            mockMvc.perform(post("/api/v1/tools/flows/{flowId}/invoke", "some-flow-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
        }
    }
}
