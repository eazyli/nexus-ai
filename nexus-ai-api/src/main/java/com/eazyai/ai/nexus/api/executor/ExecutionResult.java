package com.eazyai.ai.nexus.api.executor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 执行结果数据
     */
    private Object data;

    /**
     * 结果类型
     */
    private String resultType;

    /**
     * 执行耗时（毫秒）
     */
    private long executionTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 异常对象
     */
    private Throwable exception;

    /**
     * 资源使用情况
     */
    @Builder.Default
    private ResourceUsage resourceUsage = new ResourceUsage();

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 资源使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUsage implements Serializable {
        private static final long serialVersionUID = 1L;

        private long memoryUsed;      // 内存使用（字节）
        private long cpuTime;         // CPU时间（毫秒）
        private int ioReadBytes;      // IO读取字节数
        private int ioWriteBytes;     // IO写入字节数
    }

    /**
     * 创建成功结果
     */
    public static ExecutionResult success(Object data) {
        return ExecutionResult.builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ExecutionResult error(String message, Throwable exception) {
        return ExecutionResult.builder()
                .success(false)
                .errorMessage(message)
                .exception(exception)
                .build();
    }
}
