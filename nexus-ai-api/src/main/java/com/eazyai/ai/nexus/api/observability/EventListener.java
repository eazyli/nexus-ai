package com.eazyai.ai.nexus.api.observability;

/**
 * 事件监听器接口
 */
public interface EventListener {

    /**
     * 处理事件
     *
     * @param event 事件对象
     */
    void onEvent(AgentEvent event);

    /**
     * 是否支持该类型事件
     *
     * @param type 事件类型
     * @return 是否支持
     */
    default boolean supports(AgentEvent.EventType type) {
        return true;
    }

    /**
     * 获取监听器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取优先级（数值越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }
}
