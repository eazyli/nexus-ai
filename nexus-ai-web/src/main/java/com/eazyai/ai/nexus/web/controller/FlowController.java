package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.eazyai.ai.nexus.api.tool.ToolVisibility;
import com.eazyai.ai.nexus.api.tool.flow.FlowDefinition;
import com.eazyai.ai.nexus.infra.converter.ToolConverter;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import com.eazyai.ai.nexus.infra.dal.repository.AiMcpToolRepository;
import com.eazyai.ai.nexus.web.dto.FlowToolRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 流程工具管理控制器
 * 
 * <p>提供流程工具的创建、查询、执行等 REST 接口。</p>
 * <p>流程工具是由多个原子工具组合而成的复合工具，支持串行、并行、条件分支、循环等控制结构。</p>
 *
 * @see FlowDefinition 流程定义
 * @see ToolDescriptor 工具描述符
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tools/flows")
@RequiredArgsConstructor
@Tag(name = "流程工具管理接口", description = "流程工具的创建、查询、执行等操作")
public class FlowController {

    private final ToolBus toolBus;
    private final AiMcpToolRepository aiMcpToolRepository;
    private final ToolConverter toolConverter;

    /**
     * 创建流程工具
     */
    @PostMapping
    @Operation(summary = "创建流程工具", description = "创建一个由多个工具组合而成的流程工具")
    public ResponseEntity<ToolDescriptor> createFlowTool(@Valid @RequestBody FlowToolRegisterRequest request) {
        log.info("创建流程工具: {} (步骤数: {})", 
                request.getName(), 
                request.getFlowDefinition() != null && request.getFlowDefinition().getSteps() != null 
                        ? request.getFlowDefinition().getSteps().size() : 0);
        
        String toolId = request.getToolId() != null ? request.getToolId() : "flow-" + UUID.randomUUID().toString();
        
        // 构建工具描述符
        ToolDescriptor descriptor = ToolDescriptor.builder()
                .toolId(toolId)
                .appId(request.getAppId())
                .name(request.getName())
                .description(request.getDescription())
                .executorType("flow")
                .protocol("internal")
                .toolType(ToolDescriptor.ToolType.FLOW)
                .flowDefinition(request.getFlowDefinition())
                .visibility(parseVisibility(request.getVisibility()))
                .authorizedApps(request.getAuthorizedApps())
                .capabilities(request.getCapabilities())
                .parameters(request.getParameters())
                .retryTimes(request.getRetryTimes())
                .timeout(request.getTimeout() != null ? request.getTimeout() : 
                        (request.getFlowDefinition() != null ? request.getFlowDefinition().getTimeout() : 120000L))
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        
        // 保存到数据库
        AiMcpTool entity = toolConverter.toEntity(descriptor);
        entity.setToolType("FLOW");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        aiMcpToolRepository.insert(entity);
        
        // 注册到内存
        toolBus.registerTool(descriptor);
        
        log.info("流程工具创建成功: {} ({})", descriptor.getName(), toolId);
        return ResponseEntity.ok(descriptor);
    }

    /**
     * 获取流程工具详情
     */
    @GetMapping("/{flowId}")
    @Operation(summary = "获取流程工具详情", description = "根据流程ID获取流程工具详细信息")
    public ResponseEntity<ToolDescriptor> getFlowTool(@PathVariable("flowId") String flowId) {
        return aiMcpToolRepository.findById(flowId)
                .filter(t -> "FLOW".equals(t.getToolType()))
                .map(toolConverter::toDescriptor)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有流程工具
     */
    @GetMapping
    @Operation(summary = "获取流程工具列表", description = "获取所有已注册的流程工具列表")
    public ResponseEntity<List<ToolDescriptor>> listFlowTools() {
        List<ToolDescriptor> flows = aiMcpToolRepository.findByToolType("FLOW").stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                .map(toolConverter::toDescriptor)
                .toList();
        return ResponseEntity.ok(flows);
    }

    /**
     * 根据应用ID获取流程工具
     */
    @GetMapping("/app/{appId}")
    @Operation(summary = "根据应用ID获取流程工具", description = "获取指定应用下的所有流程工具")
    public ResponseEntity<List<ToolDescriptor>> listFlowToolsByAppId(@PathVariable("appId") String appId) {
        List<ToolDescriptor> flows = aiMcpToolRepository.findByAppId(appId).stream()
                .filter(t -> "FLOW".equals(t.getToolType()))
                .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                .map(toolConverter::toDescriptor)
                .toList();
        return ResponseEntity.ok(flows);
    }

    /**
     * 更新流程工具
     */
    @PutMapping("/{flowId}")
    @Operation(summary = "更新流程工具", description = "更新指定流程工具的定义")
    public ResponseEntity<ToolDescriptor> updateFlowTool(
            @PathVariable("flowId") String flowId,
            @Valid @RequestBody FlowToolRegisterRequest request) {
        log.info("更新流程工具: {}", flowId);
        
        AiMcpTool existing = aiMcpToolRepository.findById(flowId)
                .orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 更新字段
        if (request.getName() != null) {
            existing.setToolName(request.getName());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getFlowDefinition() != null) {
            Map<String, Object> config = new HashMap<>();
            config.put("flowDefinition", request.getFlowDefinition());
            existing.setConfig(config);
        }
        existing.setUpdateTime(LocalDateTime.now());
        
        aiMcpToolRepository.updateById(existing);
        
        // 更新内存中的描述符
        ToolDescriptor descriptor = toolConverter.toDescriptor(existing);
        toolBus.registerTool(descriptor);
        
        return ResponseEntity.ok(descriptor);
    }

    /**
     * 删除流程工具
     */
    @DeleteMapping("/{flowId}")
    @Operation(summary = "删除流程工具", description = "删除指定的流程工具")
    public ResponseEntity<Void> deleteFlowTool(@PathVariable("flowId") String flowId) {
        log.info("删除流程工具: {}", flowId);
        
        // 从数据库删除
        aiMcpToolRepository.deleteById(flowId);
        // 从内存注销
        toolBus.unregisterTool(flowId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 执行流程工具
     */
    @PostMapping("/{flowId}/invoke")
    @Operation(summary = "执行流程工具", description = "执行指定的流程工具并返回结果")
    public ResponseEntity<ToolResult> invokeFlowTool(
            @PathVariable("flowId") String flowId,
            @RequestBody Map<String, Object> params,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId) {
        
        log.info("执行流程工具: {} - params: {}", flowId, params);
        
        // 构建上下文
        AgentContext context = AgentContext.builder()
                .appId(appId)
                .sessionId(sessionId)
                .userId(userId)
                .requestId(UUID.randomUUID().toString())
                .build();
        
        ToolResult result = toolBus.invoke(flowId, params, context);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取可用工具列表（用于编排）
     */
    @GetMapping("/available-tools")
    @Operation(summary = "获取可用工具列表", description = "获取可用于编排的工具列表（原子工具）")
    public ResponseEntity<List<ToolDescriptor>> listAvailableTools(
            @RequestParam(required = false) String appId) {
        
        List<ToolDescriptor> tools;
        if (appId != null) {
            tools = aiMcpToolRepository.findByAppId(appId).stream()
                    .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                    .filter(t -> !"FLOW".equals(t.getToolType()))
                    .map(toolConverter::toDescriptor)
                    .toList();
        } else {
            tools = aiMcpToolRepository.findAllEnabled().stream()
                    .filter(t -> !"FLOW".equals(t.getToolType()))
                    .map(toolConverter::toDescriptor)
                    .toList();
        }
        
        return ResponseEntity.ok(tools);
    }

    /**
     * 验证流程定义
     */
    @PostMapping("/validate")
    @Operation(summary = "验证流程定义", description = "验证流程定义是否有效")
    public ResponseEntity<Map<String, Object>> validateFlowDefinition(
            @RequestBody FlowDefinition flowDefinition) {
        
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 验证步骤
        if (flowDefinition.getSteps() == null || flowDefinition.getSteps().isEmpty()) {
            errors.add("流程必须包含至少一个步骤");
        } else {
            Set<String> stepIds = new HashSet<>();
            for (int i = 0; i < flowDefinition.getSteps().size(); i++) {
                FlowStepWrapper step = wrapStep(flowDefinition.getSteps().get(i));
                
                if (step.stepId == null || step.stepId.isBlank()) {
                    errors.add("步骤 " + (i + 1) + " 缺少 stepId");
                } else if (stepIds.contains(step.stepId)) {
                    errors.add("步骤ID重复: " + step.stepId);
                } else {
                    stepIds.add(step.stepId);
                }
                
                if (step.toolId == null || step.toolId.isBlank()) {
                    errors.add("步骤 [" + step.stepId + "] 缺少 toolId");
                }
                
                if (step.outputVariable == null) {
                    warnings.add("步骤 [" + step.stepId + "] 没有配置输出变量");
                }
            }
        }
        
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 解析可见性
     */
    private ToolVisibility parseVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return ToolVisibility.PRIVATE;
        }
        try {
            return ToolVisibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ToolVisibility.PRIVATE;
        }
    }
    
    /**
     * 步骤包装类
     */
    private record FlowStepWrapper(String stepId, String toolId, String outputVariable) {}
    
    private FlowStepWrapper wrapStep(com.eazyai.ai.nexus.api.tool.flow.FlowStep step) {
        return new FlowStepWrapper(step.getStepId(), step.getToolId(), step.getOutputVariable());
    }
}
