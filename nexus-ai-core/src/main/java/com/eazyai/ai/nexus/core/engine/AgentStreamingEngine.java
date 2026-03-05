package com.eazyai.ai.nexus.core.engine;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.StreamEvent;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import com.eazyai.ai.nexus.api.intent.IntentAnalyzer;
import com.eazyai.ai.nexus.api.intent.IntentResult;
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
 * 流式智能体引擎 - 重构版
 * 
 * <p>核心改进：</p>
 * <ul>
 *   <li>使用 LangChain4j @MemoryId 自动关联会话记忆</li>
 *   <li>意图上下文通过 systemMessageProvider 自动注入</li>
 *   <li>简化 Assistant 获取逻辑</li>
 * </ul>
 */
@Slf4j
@Component
public class AgentStreamingEngine {

    @Autowired
    private AssistantFactory assistantFactory;

    @Autowired
    private StreamingEventBus eventBus;

    @Autowired(required = false)
    private IntentAnalyzer intentAnalyzer;

    /**
     * 流式执行请求
     */
    public void executeStreaming(AgentRequest request, Consumer<StreamEvent> eventConsumer) {
        long startTime = System.currentTimeMillis();
        String requestId = java.util.UUID.randomUUID().toString();
        String query = escapeTemplateVariables(request.getQuery());

        String sessionId = request.getSessionId();
        if (sessionId == null) {
            sessionId = java.util.UUID.randomUUID().toString();
        }

        log.info("[AgentStreamingEngine] 流式请求: requestId={}, appId={}, sessionId={}", 
                requestId, request.getAppId(), sessionId);

        // 初始化上下文
        ToolExecutionContext.init();
        DynamicToolAdapter.setCurrentRequestId(requestId);
        DynamicToolAdapter.setCurrentContext(AgentContext.builder()
                .requestId(requestId)
                .appId(request.getAppId())
                .sessionId(sessionId)
                .build());

        eventBus.subscribe(requestId, eventConsumer);

        StringBuilder fullResponse = new StringBuilder();
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        try {
            eventConsumer.accept(StreamEvent.requestStart(requestId, query));

            // 1. 意图分析（可选）
            IntentResult intentResult = analyzeIntent(request);

            // 2. 创建流式 Assistant（意图上下文自动注入到系统消息）
            StreamingAgentAssistant assistant = assistantFactory.createStreamingAssistant(
                    request.getAppId(), intentResult);

            // 3. 执行流式对话（@MemoryId 自动关联会话记忆，意图上下文已通过 systemMessageProvider 注入）
            TokenStream tokenStream = assistant.chatStreamWithMemory(sessionId, query);

            // 4. 处理流式响应
            tokenStream
                .onNext(token -> {
                    eventConsumer.accept(StreamEvent.token(token));
                    fullResponse.append(token);
                })
                .onComplete(response -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    eventConsumer.accept(StreamEvent.builder()
                            .type(StreamEvent.EventType.REQUEST_END)
                            .data(new StreamEvent.RequestEndData(requestId, executionTime))
                            .timestamp(System.currentTimeMillis())
                            .build());

                    log.info("[AgentStreamingEngine] 完成: requestId={}, time={}ms", requestId, executionTime);
                    cleanup(requestId);
                    completionLatch.countDown();
                })
                .onError(error -> {
                    log.error("[AgentStreamingEngine] 失败: requestId={}", requestId, error);
                    eventConsumer.accept(StreamEvent.error(error.getMessage()));
                    cleanup(requestId);
                    errorRef.set(error);
                    completionLatch.countDown();
                })
                .start();

            // 等待流完成
            long timeout = request.getTimeout() > 0 ? request.getTimeout() : 60000;
            boolean completed = completionLatch.await(timeout, TimeUnit.MILLISECONDS);

            if (!completed) {
                log.warn("[AgentStreamingEngine] 超时: requestId={}, timeout={}ms", requestId, timeout);
                eventConsumer.accept(StreamEvent.error("执行超时"));
                cleanup(requestId);
            }

            if (errorRef.get() != null) {
                throw new RuntimeException(errorRef.get());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[AgentStreamingEngine] 中断: requestId={}", requestId, e);
            eventConsumer.accept(StreamEvent.error("执行被中断"));
            cleanup(requestId);
        } catch (Exception e) {
            log.error("[AgentStreamingEngine] 异常: requestId={}", requestId, e);
            eventConsumer.accept(StreamEvent.error(e.getMessage()));
            cleanup(requestId);
        }
    }

    /**
     * 意图分析
     */
    private IntentResult analyzeIntent(AgentRequest request) {
        if (intentAnalyzer == null || request.getAppId() == null) {
            return null;
        }

        try {
            AgentContext context = AgentContext.builder()
                    .appId(request.getAppId())
                    .userId(request.getUserId())
                    .build();
            return intentAnalyzer.analyze(request, context);
        } catch (Exception e) {
            log.warn("[AgentStreamingEngine] 意图分析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 清理资源
     */
    private void cleanup(String requestId) {
        ToolExecutionContext.clear();
        DynamicToolAdapter.clearCurrentRequestId();
        DynamicToolAdapter.clearCurrentContext();
        eventBus.unsubscribe(requestId);
    }

    private String escapeTemplateVariables(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("{{", "{ {").replace("}}", "} }");
    }
}
