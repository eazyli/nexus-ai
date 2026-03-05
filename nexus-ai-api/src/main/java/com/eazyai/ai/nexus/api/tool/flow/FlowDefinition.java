package com.eazyai.ai.nexus.api.tool.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 流程定义
 * 
 * <p>描述一个由多个工具组合而成的复合工具（流程工具）。</p>
 * <p>流程工具与原子工具使用相同的 ToolDescriptor，通过 toolType 字段区分。</p>
 *
 * <h3>流程类型：</h3>
 * <ul>
 *   <li>SEQUENTIAL - 串行执行：步骤按顺序依次执行</li>
 *   <li>PARALLEL - 并行执行：所有步骤同时执行，等待全部完成</li>
 *   <li>CONDITIONAL - 条件分支：根据条件选择分支执行</li>
 *   <li>LOOP - 循环执行：重复执行步骤直到条件满足</li>
 *   <li>HYBRID - 混合模式：步骤内可嵌套不同类型</li>
 * </ul>
 *
 * @see FlowStep 流程步骤
 * @see com.eazyai.ai.nexus.api.tool.ToolDescriptor 工具描述符
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowDefinition implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 流程类型
     */
    private FlowType type;
    
    /**
     * 流程步骤列表
     */
    private List<FlowStep> steps;
    
    /**
     * 全局变量映射
     * <p>
     * key: 目标变量名<br>
     * value: 源表达式（如 ${step1.output.data}）
     * </p>
     */
    private Map<String, String> variableMappings;
    
    /**
     * 失败处理策略
     */
    @Builder.Default
    private FailureStrategy failureStrategy = FailureStrategy.STOP;
    
    /**
     * 流程超时时间（毫秒）
     */
    @Builder.Default
    private Long timeout = 120000L;
    
    /**
     * 并行执行的最大并发数
     */
    @Builder.Default
    private Integer maxConcurrency = 10;
    
    /**
     * 流程类型枚举
     */
    public enum FlowType {
        /** 串行执行 - 步骤按顺序依次执行 */
        SEQUENTIAL,
        
        /** 并行执行 - 所有步骤同时执行，等待全部完成 */
        PARALLEL,
        
        /** 条件分支 - 根据条件选择分支执行 */
        CONDITIONAL,
        
        /** 循环执行 - 重复执行步骤直到条件满足 */
        LOOP,
        
        /** 混合模式 - 步骤内可嵌套不同类型 */
        HYBRID
    }
    
    /**
     * 失败处理策略
     */
    public enum FailureStrategy {
        /** 立即停止整个流程 */
        STOP,
        
        /** 跳过失败步骤继续执行 */
        CONTINUE,
        
        /** 回滚已执行的步骤 */
        ROLLBACK
    }
}
