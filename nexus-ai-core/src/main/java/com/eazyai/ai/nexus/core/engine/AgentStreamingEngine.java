package com.eazyai.ai.nexus.core.engine;

import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.StreamEvent;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import com.eazyai.ai.nexus.core.assistant.StreamingAgentAssistant;
import com.eazyai.ai.nexus.core.assistant.AssistantFactory;
import com.eazyai.ai.nexus.core.assistant.McpDynamicToolAdapter;
import com.eazyai.ai.nexus.core.event.StreamingEventBus;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 流式智能体引擎
 * 支持 SSE 流式输出，实时返回 AI 思考执行流程
 * 
 * <p>事件流程：</p>
 * <ol>
 *   <li>REQUEST_START - 请求开始</li>
 *   <li>TOOL_CALL_START - 工具调用开始（如果有工具调用）</li>
 *   <li>TOOL_CALL_END - 工具调用结束（如果有工具调用）</li>
 *   <li>TOKEN - 文本生成 Token</li>
 *   <li>REQUEST_END - 请求结束</li>
 * </ol>
 */
@Slf4j
@Component
public class AgentStreamingEngine {

    @Autowired
    private AssistantFactory assistantFactory;

    @Autowired
    private StreamingEventBus eventBus;

    /**
     * 流式执行请求（阻塞式，等待流完成）
     * 
     * @param request 执行请求
     * @param eventConsumer 事件消费者（用于 SSE 推送）
     */
    public void executeStreaming(AgentRequest request, Consumer<StreamEvent> eventConsumer) {
        long startTime = System.currentTimeMillis();
        String requestId = java.util.UUID.randomUUID().toString();

        log.info("[AgentStreamingEngine] 收到流式执行请求: requestId={}, appId={}, query={}", 
                requestId, request.getAppId(), request.getQuery());

        // 初始化工具执行上下文
        ToolExecutionContext.init();
        
        // 设置当前请求ID（用于工具事件发布）
        McpDynamicToolAdapter.setCurrentRequestId(requestId);
        
        // 订阅事件总线
        eventBus.subscribe(requestId, eventConsumer);

        StringBuilder fullResponse = new StringBuilder();
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        try {
            // 发送请求开始事件
            eventConsumer.accept(StreamEvent.requestStart(requestId, request.getQuery()));

            // 获取流式 Assistant
            StreamingAgentAssistant assistant = getStreamingAssistant(request);
            
            // 执行流式请求
            TokenStream tokenStream;
            if (request.getSessionId() != null) {
                tokenStream = assistant.chatStreamWithMemory(request.getQuery(), request.getSessionId());
            } else {
                tokenStream = assistant.chatStream(request.getQuery());
            }

            // 处理流式响应
            tokenStream
                .onNext(token -> {
                    // 发送 Token 事件
                    eventConsumer.accept(StreamEvent.token(token));
                    fullResponse.append(token);
                })
                .onComplete(response -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    
                    // 发送请求结束事件
                    eventConsumer.accept(StreamEvent.builder()
                            .type(StreamEvent.EventType.REQUEST_END)
                            .data(new StreamEvent.RequestEndData(requestId, executionTime))
                            .timestamp(System.currentTimeMillis())
                            .build());
                    
                    log.info("[AgentStreamingEngine] 流式执行完成: requestId={}, executionTime={}ms, responseLength={}", 
                            requestId, executionTime, fullResponse.length());
                    
                    // 清理
                    ToolExecutionContext.clear();
                    McpDynamicToolAdapter.clearCurrentRequestId();
                    eventBus.unsubscribe(requestId);
                    completionLatch.countDown();
                })
                .onError(error -> {
                    log.error("[AgentStreamingEngine] 流式执行失败: requestId={}", requestId, error);
                    eventConsumer.accept(StreamEvent.error(error.getMessage()));
                    
                    // 清理
                    ToolExecutionContext.clear();
                    McpDynamicToolAdapter.clearCurrentRequestId();
                    eventBus.unsubscribe(requestId);
                    errorRef.set(error);
                    completionLatch.countDown();
                })
                .start();

            // 等待流完成（最长等待请求超时时间）
            long timeout = request.getTimeout() > 0 ? request.getTimeout() : 60000;
            boolean completed = completionLatch.await(timeout, TimeUnit.MILLISECONDS);
            
            if (!completed) {
                log.warn("[AgentStreamingEngine] 流式执行超时: requestId={}, timeout={}ms", requestId, timeout);
                eventConsumer.accept(StreamEvent.error("执行超时"));
                McpDynamicToolAdapter.clearCurrentRequestId();
                eventBus.unsubscribe(requestId);
            }
            
            // 检查是否有错误
            if (errorRef.get() != null) {
                throw new RuntimeException(errorRef.get());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[AgentStreamingEngine] 流式执行被中断: requestId={}", requestId, e);
            eventConsumer.accept(StreamEvent.error("执行被中断"));
            ToolExecutionContext.clear();
            McpDynamicToolAdapter.clearCurrentRequestId();
            eventBus.unsubscribe(requestId);
        } catch (Exception e) {
            log.error("[AgentStreamingEngine] 流式执行异常: requestId={}", requestId, e);
            eventConsumer.accept(StreamEvent.error(e.getMessage()));
            ToolExecutionContext.clear();
            McpDynamicToolAdapter.clearCurrentRequestId();
            eventBus.unsubscribe(requestId);
        }
    }

    /**
     * 获取流式 Assistant
     */
    private StreamingAgentAssistant getStreamingAssistant(AgentRequest request) {
        if (request.getAppId() != null) {
            log.info("[AgentStreamingEngine] 使用应用专属流式模式, appId={}", request.getAppId());
            return assistantFactory.getStreamingAssistantByAppId(request.getAppId(), request.getSessionId());
        } else if (request.getSessionId() != null) {
            log.info("[AgentStreamingEngine] 使用带记忆流式模式, sessionId={}", request.getSessionId());
            return assistantFactory.getStreamingAssistantWithMemory(request.getSessionId());
        } else {
            log.info("[AgentStreamingEngine] 使用默认流式模式");
            return assistantFactory.getStreamingAssistant();
        }
    }
}
