package com.eazyai.ai.nexus.api.tool.flow;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程执行上下文
 * 
 * <p>流程执行过程中的运行时状态和数据存储。</p>
 *
 * @see FlowDefinition 流程定义
 * @see StepExecutionResult 步骤执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowExecutionContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 执行ID
     */
    private String executionId;
    
    /**
     * 流程定义
     */
    private FlowDefinition flowDefinition;
    
    /**
     * 流程工具描述符
     */
    private ToolDescriptor flowDescriptor;
    
    /**
     * 父级 Agent 上下文
     */
    private AgentContext agentContext;
    
    /**
     * 流程输入参数
     */
    private Map<String, Object> input;
    
    /**
     * 步骤执行结果
     * <p>
     * key: stepId<br>
     * value: StepExecutionResult
     * </p>
     */
    @Builder.Default
    private Map<String, StepExecutionResult> stepResults = new LinkedHashMap<>();
    
    /**
     * 流程变量
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
    
    /**
     * 当前执行状态
     */
    @Builder.Default
    private FlowStatus status = FlowStatus.PENDING;
    
    /**
     * 当前执行的步骤ID
     */
    private String currentStepId;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 出错的步骤ID
     */
    private String errorStepId;
    
    /**
     * 流程状态
     */
    public enum FlowStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
    
    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put(key, value);
    }
    
    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables != null ? variables.get(key) : null;
    }
    
    /**
     * 获取变量（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, T defaultValue) {
        Object value = variables != null ? variables.get(key) : null;
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }
    
    /**
     * 记录步骤执行结果
     */
    public void recordStepResult(String stepId, StepExecutionResult result) {
        if (stepResults == null) {
            stepResults = new LinkedHashMap<>();
        }
        stepResults.put(stepId, result);
    }
    
    /**
     * 获取步骤执行结果
     */
    public StepExecutionResult getStepResult(String stepId) {
        return stepResults != null ? stepResults.get(stepId) : null;
    }
    
    /**
     * 获取上一步的执行结果
     */
    public StepExecutionResult getPreviousStepResult(String currentStepId) {
        if (stepResults == null || stepResults.isEmpty()) {
            return null;
        }
        
        String prevStepId = null;
        for (String stepId : stepResults.keySet()) {
            if (stepId.equals(currentStepId)) {
                break;
            }
            prevStepId = stepId;
        }
        return prevStepId != null ? stepResults.get(prevStepId) : null;
    }
    
    /**
     * 计算执行进度
     */
    public int calculateProgress() {
        if (flowDefinition == null || flowDefinition.getSteps() == null 
                || flowDefinition.getSteps().isEmpty()) {
            return 0;
        }
        int total = flowDefinition.getSteps().size();
        int completed = stepResults != null ? stepResults.size() : 0;
        return (completed * 100) / total;
    }
}
