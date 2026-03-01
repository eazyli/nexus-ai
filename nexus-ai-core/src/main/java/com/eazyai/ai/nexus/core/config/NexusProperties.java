package com.eazyai.ai.nexus.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * Nexus AI 平台配置属性
 */
@Data
@ConfigurationProperties(prefix = "nexus.ai")
public class NexusProperties {

    /**
     * HTTP工具执行器配置
     */
    private HttpProperties http = new HttpProperties();

    /**
     * 会话管理配置
     */
    private SessionProperties session = new SessionProperties();

    /**
     * 流式执行配置
     */
    private StreamingProperties streaming = new StreamingProperties();

    /**
     * 工具执行配置
     */
    private ToolProperties tool = new ToolProperties();

    /**
     * HTTP工具执行器配置
     */
    @Data
    public static class HttpProperties {
        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 5000;

        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeout = 30000;

        /**
         * 写入超时时间（毫秒）
         */
        private int writeTimeout = 30000;

        /**
         * 最大连接数
         */
        private int maxConnections = 100;

        /**
         * 每个路由的最大连接数
         */
        private int maxConnectionsPerRoute = 20;

        /**
         * 连接保持时间（毫秒）
         */
        private long keepAliveDuration = 60000;

        /**
         * 重试次数
         */
        private int retryCount = 3;

        /**
         * 重试间隔（毫秒）
         */
        private long retryInterval = 1000;
    }

    /**
     * 会话管理配置
     */
    @Data
    public static class SessionProperties {
        /**
         * 是否启用会话过期清理
         */
        private boolean expirationEnabled = true;

        /**
         * 会话过期时间（分钟）
         */
        private long expirationMinutes = 30;

        /**
         * 清理任务执行间隔（分钟）
         */
        private long cleanupIntervalMinutes = 5;

        /**
         * 最大会话数（0表示无限制）
         */
        private int maxSessions = 10000;

        /**
         * 默认消息窗口大小
         */
        private int defaultMemoryWindow = 20;
    }

    /**
     * 流式执行配置
     */
    @Data
    public static class StreamingProperties {
        /**
         * SSE连接超时时间（毫秒）
         */
        private long sseTimeout = 300000;

        /**
         * 核心线程数
         */
        private int corePoolSize = 4;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 20;

        /**
         * 线程空闲时间（秒）
         */
        private int keepAliveSeconds = 60;

        /**
         * 队列容量
         */
        private int queueCapacity = 100;

        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "nexus-streaming-";

        /**
         * 是否允许核心线程超时
         */
        private boolean allowCoreThreadTimeOut = true;
    }

    /**
     * 工具执行配置
     */
    @Data
    public static class ToolProperties {
        /**
         * 工具执行超时时间（毫秒）
         */
        private long executionTimeout = 60000;

        /**
         * 最大迭代次数
         */
        private int maxIterations = 10;

        /**
         * 是否并行执行工具
         */
        private boolean parallelExecution = false;

        /**
         * 并行执行的最大线程数
         */
        private int parallelMaxThreads = 5;
    }
}
