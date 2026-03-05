package com.eazyai.ai.nexus.api.tool.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 流程步骤
 * 
 * <p>描述流程中的一个执行单元，引用一个工具并定义其参数映射。</p>
 *
 * <h3>参数映射：</h3>
 * <pre>
 * inputMappings: {
 *   "userId": "${input.userId}",        // 从流程输入获取
 *   "userName": "${step1.output.name}", // 从上一步输出获取
 *   "score": "#{variables.score}"       // 从变量获取（SpEL）
 * }
 * </pre>
 *
 * @see FlowDefinition 流程定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowStep implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 步骤ID（流程内唯一）
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
     * 静态参数配置
     */
    private Map<String, Object> params;
    
    /**
     * 动态参数映射
     * <p>
     * key: 工具参数名<br>
     * value: 变量表达式（如 ${flow.input.userId}、${step1.output.id}）
     * </p>
     */
    private Map<String, String> inputMappings;
    
    /**
     * 条件表达式（仅 CONDITIONAL 类型时有效）
     * <p>支持 SpEL、JavaScript、简单比较表达式</p>
     */
    private String condition;
    
    /**
     * 条件分支（仅 CONDITIONAL 类型时有效）
     */
    private List<ConditionBranch> branches;
    
    /**
     * 循环配置（仅 LOOP 类型时有效）
     */
    private LoopConfig loopConfig;
    
    /**
     * 是否为关键步骤（失败则整个流程失败）
     */
    @Builder.Default
    private Boolean isCritical = true;
    
    /**
     * 步骤超时时间（毫秒），null 则使用流程默认超时
     */
    private Long timeout;
    
    /**
     * 重试次数
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 重试间隔（毫秒）
     */
    @Builder.Default
    private Long retryInterval = 1000L;
    
    /**
     * 输出变量名
     * <p>步骤执行结果将存储到此变量中，供后续步骤使用</p>
     */
    private String outputVariable;
    
    /**
     * 输出路径提取
     * <p>使用 JSONPath 从工具返回结果中提取特定字段</p>
     */
    private String outputJsonPath;
    
    /**
     * 条件分支
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionBranch implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /** 分支名称 */
        private String name;
        
        /** 分支条件表达式 */
        private String condition;
        
        /** 条件满足时执行的子步骤 */
        private List<FlowStep> subSteps;
        
        /** 分支优先级（数字越大优先级越高） */
        @Builder.Default
        private Integer priority = 0;
    }
    
    /**
     * 循环配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoopConfig implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /** 循环类型 */
        private LoopType type;
        
        /** 迭代变量名（遍历集合时） */
        private String itemVariable;
        
        /** 索引变量名 */
        private String indexVariable;
        
        /** 迭代集合表达式 */
        private String collectionExpression;
        
        /** 最大迭代次数（防止死循环） */
        @Builder.Default
        private Integer maxIterations = 100;
        
        /** 循环体（要重复执行的步骤） */
        private List<FlowStep> loopBody;
        
        /** 循环类型 */
        public enum LoopType {
            /** 遍历集合 */
            FOR_EACH,
            /** 条件循环 */
            WHILE,
            /** 计数循环 */
            FOR_COUNT
        }
    }
}
