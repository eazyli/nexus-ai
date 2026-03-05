package com.eazyai.ai.nexus.api.tool.flow;

import com.eazyai.ai.nexus.api.tool.ToolResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 步骤执行结果
 * 
 * <p>记录流程中单个步骤的执行状态和结果。</p>
 *
 * @see FlowExecutionContext 流程执行上下文
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 步骤ID
     */
    private String stepId;
    
    /**
     * 步骤名称
     */
    private String name;
    
    /**
     * 引用的工具ID
     */
    private String toolId;
    
    /**
     * 执行状态
     */
    private StepStatus status;
    
    /**
     * 工具执行结果
     */
    private ToolResult toolResult;
    
    /**
     * 提取后的输出数据
     */
    private Object output;
    
    /**
     * 输入参数快照
     */
    private Map<String, Object> inputSnapshot;
    
    /**
     * 实际重试次数
     */
    @Builder.Default
    private Integer actualRetryCount = 0;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status == StepStatus.COMPLETED || status == StepStatus.SKIPPED;
    }
    
    /**
     * 步骤状态
     */
    public enum StepStatus {
        PENDING("待执行", false),
        RUNNING("执行中", false),
        COMPLETED("已完成", true),
        FAILED("已失败", false),
        SKIPPED("已跳过", true),
        TIMEOUT("已超时", false);
        
        private final String label;
        private final boolean finished;
        
        StepStatus(String label, boolean finished) {
            this.label = label;
            this.finished = finished;
        }
        
        public String getLabel() { 
            return label; 
        }
        
        public boolean isSuccess() { 
            return finished; 
        }
    }
}
