package com.eazyai.ai.nexus.core.engine;

import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.StreamEvent;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import com.eazyai.ai.nexus.core.assistant.StreamingAgentAssistant;
import com.eazyai.ai.nexus.core.assistant.AssistantFactory;
import com.eazyai.ai.nexus.core.assistant.DynamicToolAdapter;
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

        // 转义用户输入中的模板语法，防止 {{xxx}} 被当作模板变量解析
        String query = escapeTemplateVariables(request.getQuery());

        // 判断是否使用会话记忆：需要同时满足 sessionId 存在且会话记忆存在
        String sessionId = request.getSessionId();
        boolean useMemory = false;
        
        if (sessionId != null) {
            useMemory = assistantFactory.sessionExists(sessionId);
            if (useMemory) {
                log.info("[AgentStreamingEngine] 找到已有会话记忆, sessionId={}", sessionId);
            } else {
                log.info("[AgentStreamingEngine] 会话记忆不存在，将创建新会话, sessionId={}", sessionId);
            }
        } else {
            sessionId = java.util.UUID.randomUUID().toString();
            log.info("[AgentStreamingEngine] 生成新会话ID, sessionId={}", sessionId);
        }

        log.info("[AgentStreamingEngine] 收到流式执行请求: requestId={}, appId={}, useMemory={}, query={}", 
                requestId, request.getAppId(), useMemory, query);

        // 初始化工具执行上下文
        ToolExecutionContext.init();
        
        // 设置当前请求ID（用于工具事件发布）
        DynamicToolAdapter.setCurrentRequestId(requestId);
        
        // 订阅事件总线
        eventBus.subscribe(requestId, eventConsumer);

        StringBuilder fullResponse = new StringBuilder();
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        try {
            // 发送请求开始事件
            eventConsumer.accept(StreamEvent.requestStart(requestId, query));

            // 获取流式 Assistant
            StreamingAgentAssistant assistant = getStreamingAssistant(request, useMemory);
            
            // 执行流式请求
            TokenStream tokenStream;
            if (useMemory) {
                // 带记忆模式
                tokenStream = assistant.chatStreamWithMemory(query);
            } else {
                // 无记忆模式
                tokenStream = assistant.chatStream(query);
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
                    DynamicToolAdapter.clearCurrentRequestId();
                    eventBus.unsubscribe(requestId);
                    completionLatch.countDown();
                })
                .onError(error -> {
                    log.error("[AgentStreamingEngine] 流式执行失败: requestId={}", requestId, error);
                    eventConsumer.accept(StreamEvent.error(error.getMessage()));
                    
                    // 清理
                    ToolExecutionContext.clear();
                    DynamicToolAdapter.clearCurrentRequestId();
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
                DynamicToolAdapter.clearCurrentRequestId();
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
            DynamicToolAdapter.clearCurrentRequestId();
            eventBus.unsubscribe(requestId);
        } catch (Exception e) {
            log.error("[AgentStreamingEngine] 流式执行异常: requestId={}", requestId, e);
            eventConsumer.accept(StreamEvent.error(e.getMessage()));
            ToolExecutionContext.clear();
            DynamicToolAdapter.clearCurrentRequestId();
            eventBus.unsubscribe(requestId);
        }
    }

    /**
     * 获取流式 Assistant
     * @param request 请求
     * @param useMemory 是否使用会话记忆
     */
    private StreamingAgentAssistant getStreamingAssistant(AgentRequest request, boolean useMemory) {
        if (request.getAppId() != null) {
            if (useMemory) {
                log.info("[AgentStreamingEngine] 使用应用专属记忆流式模式, appId={}, sessionId={}", 
                        request.getAppId(), request.getSessionId());
                return assistantFactory.getStreamingAssistantByAppId(request.getAppId(), request.getSessionId());
            } else {
                log.info("[AgentStreamingEngine] 使用应用专属无记忆流式模式, appId={}", request.getAppId());
                return assistantFactory.getStreamingAssistantByAppId(request.getAppId());
            }
        } else {
            if (useMemory) {
                log.info("[AgentStreamingEngine] 使用通用记忆流式模式, sessionId={}", request.getSessionId());
                return assistantFactory.getStreamingAssistantWithMemory(request.getSessionId());
            } else {
                log.info("[AgentStreamingEngine] 使用通用无记忆流式模式");
                return assistantFactory.getStreamingAssistant();
            }
        }
    }

    /**
     * 转义模板变量语法
     * LangChain4j 会把 {{xxx}} 当作模板变量解析，需要转义防止误解析
     */
    private String escapeTemplateVariables(String input) {
        if (input == null) {
            return null;
        }
        // 将 {{ 替换为 { { ，避免被解析为模板变量
        return input.replace("{{", "{ {").replace("}}", "} }");
    }
}
