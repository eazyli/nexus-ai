package com.eazyai.ai.nexus.core.tool.executor;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolExecutor;
import com.eazyai.ai.nexus.api.tool.ToolOrchestrator;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.eazyai.ai.nexus.api.tool.flow.*;
import com.eazyai.ai.nexus.core.tool.flow.ConditionEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * 流程执行器
 * 
 * <p>实现 ToolExecutor 接口，将流程作为一种特殊的工具类型执行。</p>
 * 
 * <h3>架构位置：</h3>
 * <pre>
 * ToolBus.invoke(flowToolId, params, context)
 *        ↓
 * FlowExecutor.execute(descriptor, params, context)
 *        ↓
 * 根据流程类型分发执行：
 *   - SEQUENTIAL: executeSequential()
 *   - PARALLEL: executeParallel()
 *   - CONDITIONAL: executeConditional()
 *   - LOOP: executeLoop()
 *   - HYBRID: executeHybrid()
 *        ↓
 * executeStep() → ToolBus.invoke(subToolId, stepParams, context)
 * </pre>
 *
 * @see FlowDefinition 流程定义
 * @see ConditionEngine 条件引擎
 */
@Slf4j
@Component
public class FlowExecutor implements ToolExecutor {
    
    private static final String EXECUTOR_TYPE = "flow";
    
    /**
     * 工具编排器 - 用于执行流程中的子工具
     * 通过编排器间接调用ToolBus，打破循环依赖
     */
    private final ToolOrchestrator toolOrchestrator;
    
    @Autowired
    private ConditionEngine conditionEngine;
    
    public FlowExecutor(ToolOrchestrator toolOrchestrator) {
        this.toolOrchestrator = toolOrchestrator;
        log.info("[FlowExecutor] 初始化完成，使用ToolOrchestrator进行子工具编排");
    }
    
    private final ExecutorService parallelExecutor = Executors.newFixedThreadPool(10);
    
    @Override
    public String getExecutorType() {
        return EXECUTOR_TYPE;
    }
    
    @Override
    public ToolResult execute(ToolDescriptor descriptor, Map<String, Object> params, AgentContext agentContext) {
        long startTime = System.currentTimeMillis();
        String flowId = descriptor.getToolId();
        
        log.info("[FlowExecutor] 开始执行流程: {} ({})", descriptor.getName(), flowId);
        
        // 1. 验证流程定义
        FlowDefinition flowDefinition = descriptor.getFlowDefinition();
        if (flowDefinition == null) {
            return ToolResult.error(flowId, "FLOW_DEFINITION_MISSING", "流程工具缺少流程定义");
        }
        
        // 2. 创建执行上下文
        FlowExecutionContext context = FlowExecutionContext.builder()
                .executionId(UUID.randomUUID().toString())
                .flowDefinition(flowDefinition)
                .flowDescriptor(descriptor)
                .agentContext(agentContext)
                .input(params != null ? new HashMap<>(params) : new HashMap<>())
                .variables(new HashMap<>(params != null ? params : Collections.emptyMap()))
                .status(FlowExecutionContext.FlowStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .build();
        
        try {
            // 3. 根据流程类型执行
            Object result = executeByType(flowDefinition, context);
            
            // 4. 标记完成
            context.setStatus(FlowExecutionContext.FlowStatus.COMPLETED);
            context.setEndTime(LocalDateTime.now());
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[FlowExecutor] 流程执行完成: {}, 耗时: {}ms, 步骤数: {}", 
                    flowId, executionTime, context.getStepResults().size());
            
            return ToolResult.builder()
                    .toolId(flowId)
                    .success(true)
                    .data(result)
                    .executionTime(executionTime)
                    .metadata(Map.of(
                            "executionId", context.getExecutionId(),
                            "stepCount", context.getStepResults().size(),
                            "status", "COMPLETED"
                    ))
                    .build();
            
        } catch (FlowExecutionException e) {
            log.error("[FlowExecutor] 流程执行失败: {}, 步骤: {}", flowId, e.getStepId(), e);
            
            context.setStatus(FlowExecutionContext.FlowStatus.FAILED);
            context.setEndTime(LocalDateTime.now());
            context.setErrorMessage(e.getMessage());
            context.setErrorStepId(e.getStepId());
            
            return ToolResult.builder()
                    .toolId(flowId)
                    .success(false)
                    .errorCode("FLOW_EXECUTION_ERROR")
                    .errorMessage(e.getMessage())
                    .exception(e)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .metadata(Map.of(
                            "executionId", context.getExecutionId(),
                            "errorStepId", e.getStepId(),
                            "status", "FAILED"
                    ))
                    .build();
            
        } catch (Exception e) {
            log.error("[FlowExecutor] 流程执行异常: {}", flowId, e);
            
            context.setStatus(FlowExecutionContext.FlowStatus.FAILED);
            context.setEndTime(LocalDateTime.now());
            context.setErrorMessage(e.getMessage());
            
            return ToolResult.builder()
                    .toolId(flowId)
                    .success(false)
                    .errorCode("FLOW_EXECUTION_ERROR")
                    .errorMessage(e.getMessage())
                    .exception(e)
                    .executionTime(System.currentTimeMillis() - startTime)
                    .metadata(Map.of(
                            "executionId", context.getExecutionId(),
                            "status", "FAILED"
                    ))
                    .build();
        }
    }
    
    /**
     * 根据流程类型执行
     */
    private Object executeByType(FlowDefinition flowDefinition, FlowExecutionContext context) {
        if (flowDefinition.getType() == null) {
            flowDefinition.setType(FlowDefinition.FlowType.SEQUENTIAL);
        }
        
        return switch (flowDefinition.getType()) {
            case SEQUENTIAL -> executeSequential(flowDefinition, context);
            case PARALLEL -> executeParallel(flowDefinition, context);
            case CONDITIONAL -> executeConditional(flowDefinition, context);
            case LOOP -> executeLoop(flowDefinition, context);
            case HYBRID -> executeHybrid(flowDefinition, context);
        };
    }
    
    /**
     * 串行执行
     */
    private Object executeSequential(FlowDefinition flowDefinition, FlowExecutionContext context) {
        log.debug("[FlowExecutor] 串行执行流程，步骤数: {}", 
                flowDefinition.getSteps() != null ? flowDefinition.getSteps().size() : 0);
        
        if (flowDefinition.getSteps() == null || flowDefinition.getSteps().isEmpty()) {
            return buildFlowOutput(flowDefinition, context);
        }
        
        for (FlowStep step : flowDefinition.getSteps()) {
            StepExecutionResult stepResult = executeStep(step, context);
            context.recordStepResult(step.getStepId(), stepResult);
            
            // 检查执行结果
            if (!stepResult.isSuccess() && Boolean.TRUE.equals(step.getIsCritical())) {
                context.setErrorStepId(step.getStepId());
                throw new FlowExecutionException(
                        "关键步骤执行失败: " + step.getName(), 
                        step.getStepId(),
                        stepResult.getToolResult()
                );
            }
        }
        
        return buildFlowOutput(flowDefinition, context);
    }
    
    /**
     * 并行执行
     */
    private Object executeParallel(FlowDefinition flowDefinition, FlowExecutionContext context) {
        log.debug("[FlowExecutor] 并行执行流程，步骤数: {}", 
                flowDefinition.getSteps() != null ? flowDefinition.getSteps().size() : 0);
        
        if (flowDefinition.getSteps() == null || flowDefinition.getSteps().isEmpty()) {
            return buildFlowOutput(flowDefinition, context);
        }
        
        List<FlowStep> steps = flowDefinition.getSteps();
        int maxConcurrency = flowDefinition.getMaxConcurrency() != null 
                ? flowDefinition.getMaxConcurrency() : 10;
        
        Semaphore semaphore = new Semaphore(maxConcurrency);
        List<CompletableFuture<StepExecutionResult>> futures = new ArrayList<>();
        
        for (FlowStep step : steps) {
            CompletableFuture<StepExecutionResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    return executeStep(step, context);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return StepExecutionResult.builder()
                            .stepId(step.getStepId())
                            .name(step.getName())
                            .status(StepExecutionResult.StepStatus.FAILED)
                            .errorMessage("线程被中断")
                            .build();
                } finally {
                    semaphore.release();
                }
            }, parallelExecutor);
            
            futures.add(future);
        }
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 收集结果并检查失败
        for (int i = 0; i < futures.size(); i++) {
            StepExecutionResult result = futures.get(i).join();
            FlowStep step = steps.get(i);
            context.recordStepResult(step.getStepId(), result);
            
            if (!result.isSuccess() && Boolean.TRUE.equals(step.getIsCritical())) {
                context.setErrorStepId(step.getStepId());
                throw new FlowExecutionException(
                        "关键步骤执行失败: " + step.getName(),
                        step.getStepId(),
                        result.getToolResult()
                );
            }
        }
        
        return buildFlowOutput(flowDefinition, context);
    }
    
    /**
     * 条件分支执行
     */
    private Object executeConditional(FlowDefinition flowDefinition, FlowExecutionContext context) {
        log.debug("[FlowExecutor] 条件分支执行流程");
        
        if (flowDefinition.getSteps() == null) {
            return buildFlowOutput(flowDefinition, context);
        }
        
        for (FlowStep step : flowDefinition.getSteps()) {
            if (step.getBranches() == null || step.getBranches().isEmpty()) {
                // 没有分支配置，检查步骤条件
                if (step.getCondition() != null) {
                    ConditionEngine.ConditionResult conditionResult = 
                            conditionEngine.evaluate(step.getCondition(), context);
                    if (!conditionResult.isSuccess() || !conditionResult.getValue()) {
                        log.debug("[FlowExecutor] 步骤条件不满足，跳过: {}", step.getStepId());
                        continue;
                    }
                }
                
                StepExecutionResult result = executeStep(step, context);
                context.recordStepResult(step.getStepId(), result);
                
                if (!result.isSuccess() && Boolean.TRUE.equals(step.getIsCritical())) {
                    context.setErrorStepId(step.getStepId());
                    throw new FlowExecutionException(
                            "步骤执行失败: " + step.getName(),
                            step.getStepId(),
                            result.getToolResult()
                    );
                }
                continue;
            }
            
            // 按优先级排序分支
            List<FlowStep.ConditionBranch> sortedBranches = step.getBranches().stream()
                    .sorted(Comparator.comparing(FlowStep.ConditionBranch::getPriority, 
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            
            // 评估条件，选择分支
            FlowStep.ConditionBranch selectedBranch = null;
            for (FlowStep.ConditionBranch branch : sortedBranches) {
                ConditionEngine.ConditionResult conditionResult = 
                        conditionEngine.evaluate(branch.getCondition(), context);
                
                if (conditionResult.isSuccess() && conditionResult.getValue()) {
                    selectedBranch = branch;
                    log.debug("[FlowExecutor] 选择分支: {}", branch.getName());
                    break;
                }
            }
            
            // 执行选中分支的子步骤
            if (selectedBranch != null && selectedBranch.getSubSteps() != null) {
                for (FlowStep subStep : selectedBranch.getSubSteps()) {
                    StepExecutionResult result = executeStep(subStep, context);
                    context.recordStepResult(subStep.getStepId(), result);
                    
                    if (!result.isSuccess() && Boolean.TRUE.equals(subStep.getIsCritical())) {
                        context.setErrorStepId(subStep.getStepId());
                        throw new FlowExecutionException(
                                "分支步骤执行失败: " + subStep.getName(),
                                subStep.getStepId(),
                                result.getToolResult()
                        );
                    }
                }
            }
        }
        
        return buildFlowOutput(flowDefinition, context);
    }
    
    /**
     * 循环执行
     */
    private Object executeLoop(FlowDefinition flowDefinition, FlowExecutionContext context) {
        log.debug("[FlowExecutor] 循环执行流程");
        
        if (flowDefinition.getSteps() == null) {
            return buildFlowOutput(flowDefinition, context);
        }
        
        for (FlowStep step : flowDefinition.getSteps()) {
            FlowStep.LoopConfig loopConfig = step.getLoopConfig();
            if (loopConfig == null) {
                // 没有循环配置，直接执行
                StepExecutionResult result = executeStep(step, context);
                context.recordStepResult(step.getStepId(), result);
                continue;
            }
            
            List<Object> loopResults = new ArrayList<>();
            int iteration = 0;
            int maxIterations = loopConfig.getMaxIterations() != null 
                    ? loopConfig.getMaxIterations() : 100;
            
            if (loopConfig.getType() == null) {
                loopConfig.setType(FlowStep.LoopConfig.LoopType.FOR_EACH);
            }
            
            switch (loopConfig.getType()) {
                case FOR_EACH -> {
                    Object collection = conditionEngine.resolveVariable(
                            loopConfig.getCollectionExpression(), context);
                    
                    if (collection instanceof Iterable<?> iterable) {
                        for (Object item : iterable) {
                            if (iteration >= maxIterations) {
                                log.warn("[FlowExecutor] 循环达到最大迭代次数: {}", maxIterations);
                                break;
                            }
                            
                            // 设置迭代变量
                            if (loopConfig.getItemVariable() != null) {
                                context.setVariable(loopConfig.getItemVariable(), item);
                            }
                            if (loopConfig.getIndexVariable() != null) {
                                context.setVariable(loopConfig.getIndexVariable(), iteration);
                            }
                            
                            // 执行循环体
                            if (loopConfig.getLoopBody() != null) {
                                for (FlowStep bodyStep : loopConfig.getLoopBody()) {
                                    StepExecutionResult result = executeStep(bodyStep, context);
                                    if (result.isSuccess()) {
                                        loopResults.add(result.getOutput());
                                    }
                                }
                            }
                            
                            iteration++;
                        }
                    }
                }
                
                case WHILE -> {
                    while (iteration < maxIterations) {
                        ConditionEngine.ConditionResult conditionResult = 
                                conditionEngine.evaluate(loopConfig.getCollectionExpression(), context);
                        
                        if (!conditionResult.isSuccess() || !conditionResult.getValue()) {
                            break;
                        }
                        
                        if (loopConfig.getLoopBody() != null) {
                            for (FlowStep bodyStep : loopConfig.getLoopBody()) {
                                StepExecutionResult result = executeStep(bodyStep, context);
                                if (result.isSuccess()) {
                                    loopResults.add(result.getOutput());
                                }
                            }
                        }
                        
                        iteration++;
                    }
                }
                
                case FOR_COUNT -> {
                    int count;
                    try {
                        count = Integer.parseInt(loopConfig.getCollectionExpression());
                    } catch (NumberFormatException e) {
                        count = 0;
                    }
                    count = Math.min(count, maxIterations);
                    
                    for (int i = 0; i < count; i++) {
                        if (loopConfig.getIndexVariable() != null) {
                            context.setVariable(loopConfig.getIndexVariable(), i);
                        }
                        
                        if (loopConfig.getLoopBody() != null) {
                            for (FlowStep bodyStep : loopConfig.getLoopBody()) {
                                StepExecutionResult result = executeStep(bodyStep, context);
                                if (result.isSuccess()) {
                                    loopResults.add(result.getOutput());
                                }
                            }
                        }
                    }
                }
            }
            
            // 记录循环结果
            StepExecutionResult loopResult = StepExecutionResult.builder()
                    .stepId(step.getStepId())
                    .name(step.getName())
                    .status(StepExecutionResult.StepStatus.COMPLETED)
                    .output(loopResults)
                    .build();
            context.recordStepResult(step.getStepId(), loopResult);
        }
        
        return buildFlowOutput(flowDefinition, context);
    }
    
    /**
     * 混合模式执行
     */
    private Object executeHybrid(FlowDefinition flowDefinition, FlowExecutionContext context) {
        log.debug("[FlowExecutor] 混合模式执行流程");
        
        if (flowDefinition.getSteps() == null) {
            return buildFlowOutput(flowDefinition, context);
        }
        
        for (FlowStep step : flowDefinition.getSteps()) {
            // 从步骤配置中获取执行类型
            String stepType = "sequential";
            if (step.getParams() != null && step.getParams().containsKey("_stepType")) {
                stepType = step.getParams().get("_stepType").toString();
            }
            
            FlowDefinition singleStepFlow = FlowDefinition.builder()
                    .type(FlowDefinition.FlowType.valueOf(stepType.toUpperCase()))
                    .steps(List.of(step))
                    .build();
            
            executeByType(singleStepFlow, context);
        }
        
        return buildFlowOutput(flowDefinition, context);
    }
    
    /**
     * 执行单个步骤
     */
    private StepExecutionResult executeStep(FlowStep step, FlowExecutionContext context) {
        String stepId = step.getStepId();
        log.debug("[FlowExecutor] 执行步骤: {} ({})", step.getName(), stepId);
        
        LocalDateTime stepStartTime = LocalDateTime.now();
        context.setCurrentStepId(stepId);
        
        try {
            // 1. 检查条件（如果有）
            if (step.getCondition() != null) {
                ConditionEngine.ConditionResult conditionResult = 
                        conditionEngine.evaluate(step.getCondition(), context);
                
                if (!conditionResult.isSuccess() || !conditionResult.getValue()) {
                    log.debug("[FlowExecutor] 步骤条件不满足，跳过: {}", stepId);
                    return StepExecutionResult.builder()
                            .stepId(stepId)
                            .name(step.getName())
                            .toolId(step.getToolId())
                            .status(StepExecutionResult.StepStatus.SKIPPED)
                            .startTime(stepStartTime)
                            .endTime(LocalDateTime.now())
                            .build();
                }
            }
            
            // 2. 构建参数
            Map<String, Object> stepParams = buildStepParams(step, context);
            
            // 3. 调用子工具（通过编排器）
            ToolResult toolResult = toolOrchestrator.invokeTool(step.getToolId(), stepParams, context.getAgentContext());
            
            // 4. 提取输出
            Object output = toolResult.getData();
            if (step.getOutputJsonPath() != null && output != null) {
                output = extractByJsonPath(output, step.getOutputJsonPath());
            }
            
            // 5. 设置输出变量
            if (step.getOutputVariable() != null && output != null) {
                context.setVariable(step.getOutputVariable(), output);
            }
            
            LocalDateTime stepEndTime = LocalDateTime.now();
            
            return StepExecutionResult.builder()
                    .stepId(stepId)
                    .name(step.getName())
                    .toolId(step.getToolId())
                    .status(toolResult.isSuccess() 
                            ? StepExecutionResult.StepStatus.COMPLETED 
                            : StepExecutionResult.StepStatus.FAILED)
                    .toolResult(toolResult)
                    .output(output)
                    .inputSnapshot(stepParams)
                    .startTime(stepStartTime)
                    .endTime(stepEndTime)
                    .executionTime(java.time.Duration.between(stepStartTime, stepEndTime).toMillis())
                    .errorMessage(toolResult.isSuccess() ? null : toolResult.getErrorMessage())
                    .build();
            
        } catch (Exception e) {
            log.error("[FlowExecutor] 步骤执行异常: {} ({})", step.getName(), stepId, e);
            
            return StepExecutionResult.builder()
                    .stepId(stepId)
                    .name(step.getName())
                    .toolId(step.getToolId())
                    .status(StepExecutionResult.StepStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .startTime(stepStartTime)
                    .endTime(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * 构建步骤参数
     */
    private Map<String, Object> buildStepParams(FlowStep step, FlowExecutionContext context) {
        Map<String, Object> params = new HashMap<>();
        
        // 1. 静态参数
        if (step.getParams() != null) {
            params.putAll(step.getParams());
        }
        
        // 2. 动态参数映射
        if (step.getInputMappings() != null) {
            for (Map.Entry<String, String> entry : step.getInputMappings().entrySet()) {
                String paramName = entry.getKey();
                String expression = entry.getValue();
                
                Object value = resolveVariable(expression, context);
                params.put(paramName, value);
            }
        }
        
        return params;
    }
    
    /**
     * 解析变量表达式
     */
    private Object resolveVariable(String expression, FlowExecutionContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        
        expression = expression.trim();
        
        // ${variable} 格式
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String varPath = expression.substring(2, expression.length() - 1).trim();
            return conditionEngine.resolveVariable(varPath, context);
        }
        
        // #{expression} 格式（SpEL）
        if (expression.startsWith("#{") && expression.endsWith("}")) {
            ConditionEngine.ConditionResult result = 
                    conditionEngine.evaluate(expression, context);
            return result.getValue();
        }
        
        // 直接变量路径
        if (expression.contains(".") || expression.startsWith("input.") 
                || expression.startsWith("variables.") || expression.startsWith("step")) {
            return conditionEngine.resolveVariable(expression, context);
        }
        
        // 字面值
        return parseLiteral(expression);
    }
    
    /**
     * 解析字面值
     */
    private Object parseLiteral(String value) {
        if (value == null) return null;
        
        // 字符串（带引号）
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        
        // 数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // 不是数字
        }
        
        // 布尔
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        if ("null".equalsIgnoreCase(value)) return null;
        
        return value;
    }
    
    /**
     * JSONPath 提取
     */
    private Object extractByJsonPath(Object data, String jsonPath) {
        try {
            if (jsonPath.startsWith("$.")) {
                jsonPath = jsonPath.substring(2);
            }
            
            String[] parts = jsonPath.split("\\.");
            Object current = data;
            
            for (String part : parts) {
                current = getNestedProperty(current, part);
            }
            
            return current;
        } catch (Exception e) {
            log.warn("[FlowExecutor] JSONPath 提取失败: {}", jsonPath, e);
            return data;
        }
    }
    
    /**
     * 获取嵌套属性
     */
    @SuppressWarnings("unchecked")
    private Object getNestedProperty(Object obj, String property) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).get(property);
        }
        
        // 处理数组索引 [0]
        if (property.startsWith("[") && property.endsWith("]")) {
            try {
                int index = Integer.parseInt(property.substring(1, property.length() - 1));
                if (obj instanceof List) {
                    List<?> list = (List<?>) obj;
                    return index < list.size() ? list.get(index) : null;
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
            return null;
        }
        
        // 反射获取属性
        try {
            Field field = obj.getClass().getDeclaredField(property);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 构建流程输出
     */
    private Object buildFlowOutput(FlowDefinition flowDefinition, FlowExecutionContext context) {
        // 应用全局变量映射
        if (flowDefinition.getVariableMappings() != null) {
            for (Map.Entry<String, String> entry : flowDefinition.getVariableMappings().entrySet()) {
                String targetVar = entry.getKey();
                String sourceExpr = entry.getValue();
                Object value = resolveVariable(sourceExpr, context);
                context.setVariable(targetVar, value);
            }
        }
        
        // 构建输出
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("variables", context.getVariables());
        
        // 添加步骤结果摘要
        Map<String, Object> stepSummary = new LinkedHashMap<>();
        if (context.getStepResults() != null) {
            for (Map.Entry<String, StepExecutionResult> entry : context.getStepResults().entrySet()) {
                StepExecutionResult result = entry.getValue();
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("status", result.getStatus().name());
                if (result.getOutput() != null) {
                    summary.put("output", result.getOutput());
                }
                stepSummary.put(entry.getKey(), summary);
            }
        }
        output.put("steps", stepSummary);
        
        return output;
    }
    
    /**
     * 流程执行异常
     */
    public static class FlowExecutionException extends RuntimeException {
        private final String stepId;
        private final ToolResult failedToolResult;
        
        public FlowExecutionException(String message, String stepId, ToolResult failedToolResult) {
            super(message);
            this.stepId = stepId;
            this.failedToolResult = failedToolResult;
        }
        
        public String getStepId() { return stepId; }
        public ToolResult getFailedToolResult() { return failedToolResult; }
    }
}
