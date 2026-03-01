package com.eazyai.ai.nexus.web.controller;

import com.alibaba.fastjson.JSON;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.StreamEvent;
import com.eazyai.ai.nexus.core.config.NexusProperties;
import com.eazyai.ai.nexus.core.engine.AgentStreamingEngine;
import com.eazyai.ai.nexus.web.dto.AgentExecuteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * 智能体流式API控制器
 * 提供 SSE 流式接口，实时返回 AI 思考执行流程
 * 
 * <p>特性：</p>
 * <ul>
 *   <li>可配置的线程池参数</li>
 *   <li>可配置的SSE超时时间</li>
 *   <li>有界队列防止OOM</li>
 *   <li>优雅关闭</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "智能体流式接口", description = "AI Agent 流式执行接口，支持实时返回执行过程")
public class AgentStreamingController {

    @Autowired(required = false)
    private AgentStreamingEngine streamingEngine;

    @Autowired
    private NexusProperties nexusProperties;

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        NexusProperties.StreamingProperties streamingProps = nexusProperties.getStreaming();

        // 使用有界线程池，防止OOM
        this.executor = new ThreadPoolExecutor(
            streamingProps.getCorePoolSize(),
            streamingProps.getMaxPoolSize(),
            streamingProps.getKeepAliveSeconds(),
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(streamingProps.getQueueCapacity()),
            new ThreadFactory() {
                private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, streamingProps.getThreadNamePrefix() + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：调用者执行
        );

        log.info("[AgentStreamingController] 线程池初始化完成: core={}, max={}, queue={}", 
            streamingProps.getCorePoolSize(), streamingProps.getMaxPoolSize(), streamingProps.getQueueCapacity());
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                log.info("[AgentStreamingController] 线程池已关闭");
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 流式执行智能体任务
     * 通过 SSE 实时返回 AI 的思考执行流程
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式执行智能体任务", 
               description = "通过 SSE 实时返回 AI 的思考执行流程，包括 Token 输出和工具调用过程")
    public SseEmitter executeStreaming(@Valid @RequestBody AgentExecuteRequest request) {
        log.info("[SSE] 收到流式执行请求: query={}, appId={}, sessionId={}", 
                request.getQuery(), request.getAppId(), request.getSessionId());

        // 使用配置的SSE超时时间
        long sseTimeout = nexusProperties.getStreaming().getSseTimeout();
        SseEmitter emitter = new SseEmitter(sseTimeout);

        // 检查流式引擎是否可用
        if (streamingEngine == null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ERROR")
                        .data("流式引擎未初始化"));
                emitter.complete();
            } catch (IOException e) {
                log.error("[SSE] 发送错误失败", e);
            }
            return emitter;
        }

        executor.execute(() -> {
            try {
                AgentRequest agentRequest = AgentRequest.builder()
                        .query(request.getQuery())
                        .appId(request.getAppId())
                        .taskType(request.getTaskType())
                        .sessionId(request.getSessionId())
                        .maxIterations(request.getMaxIterations() != null ? 
                            request.getMaxIterations() : nexusProperties.getTool().getMaxIterations())
                        .timeout(request.getTimeout() != null ? 
                            request.getTimeout() : nexusProperties.getTool().getExecutionTimeout())
                        .params(request.getParams())
                        .outputFormat(request.getOutputFormat())
                        .build();

                streamingEngine.executeStreaming(agentRequest, event -> {
                    try {
                        // 发送 SSE 事件
                        emitter.send(SseEmitter.event()
                                .name(event.getType().name())
                                .data(JSON.toJSONString(event)));
                    } catch (IOException e) {
                        log.error("[SSE] 发送事件失败: {}", e.getMessage());
                        throw new RuntimeException("SSE 发送失败", e);
                    }
                });

                // 完成流
                emitter.complete();
                log.info("[SSE] 流式执行完成");

            } catch (Exception e) {
                log.error("[SSE] 流式执行异常", e);
                try {
                    StreamEvent errorEvent = StreamEvent.error(e.getMessage());
                    emitter.send(SseEmitter.event()
                            .name("ERROR")
                            .data(JSON.toJSONString(errorEvent)));
                } catch (IOException ex) {
                    log.error("[SSE] 发送错误事件失败", ex);
                }
                emitter.completeWithError(e);
            }
        });

        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("[SSE] 连接超时: sseTimeout={}ms", sseTimeout);
            emitter.complete();
        });

        // 设置完成回调
        emitter.onCompletion(() -> {
            log.debug("[SSE] 连接关闭");
        });

        // 设置错误回调
        emitter.onError(e -> {
            log.error("[SSE] 连接错误: {}", e.getMessage());
        });

        return emitter;
    }
}
