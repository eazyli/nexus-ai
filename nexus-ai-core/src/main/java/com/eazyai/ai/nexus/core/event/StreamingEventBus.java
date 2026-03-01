package com.eazyai.ai.nexus.core.event;

import com.eazyai.ai.nexus.api.dto.StreamEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 流式事件总线
 * 用于在工具执行过程中发布事件，供 SSE 推送使用
 * 
 * <p>使用方式：</p>
 * <pre>
 * // 请求开始时注册消费者
 * String subscriptionId = StreamingEventBus.subscribe(eventConsumer);
 * 
 * // 工具执行时发布事件
 * StreamingEventBus.publish(StreamEvent.toolCallStart(...));
 * 
 * // 请求结束时取消订阅
 * StreamingEventBus.unsubscribe(subscriptionId);
 * </pre>
 */
@Slf4j
@Component
public class StreamingEventBus {

    /**
     * 订阅者映射（按 requestId 分组）
     */
    private static final Map<String, List<Consumer<StreamEvent>>> subscribers = 
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 订阅事件
     * 
     * @param requestId 请求ID
     * @param consumer 事件消费者
     */
    public void subscribe(String requestId, Consumer<StreamEvent> consumer) {
        subscribers.computeIfAbsent(requestId, k -> new CopyOnWriteArrayList<>()).add(consumer);
        log.debug("[EventBus] 订阅事件: requestId={}", requestId);
    }

    /**
     * 取消订阅
     * 
     * @param requestId 请求ID
     */
    public void unsubscribe(String requestId) {
        subscribers.remove(requestId);
        log.debug("[EventBus] 取消订阅: requestId={}", requestId);
    }

    /**
     * 发布事件
     * 
     * @param requestId 请求ID
     * @param event 事件
     */
    public void publish(String requestId, StreamEvent event) {
        List<Consumer<StreamEvent>> consumers = subscribers.get(requestId);
        if (consumers != null && !consumers.isEmpty()) {
            for (Consumer<StreamEvent> consumer : consumers) {
                try {
                    consumer.accept(event);
                } catch (Exception e) {
                    log.warn("[EventBus] 事件消费失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 发布工具调用开始事件
     */
    public void publishToolCallStart(String requestId, String toolName, String description, Object input) {
        publish(requestId, StreamEvent.toolCallStart(toolName, description, input));
    }

    /**
     * 发布工具调用结束事件
     */
    public void publishToolCallEnd(String requestId, String toolName, String description, 
                                   Object input, Object output, long executionTime, 
                                   boolean success, String errorMessage) {
        publish(requestId, StreamEvent.toolCallEnd(toolName, description, input, output, 
                executionTime, success, errorMessage));
    }
}
