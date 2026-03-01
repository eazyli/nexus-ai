package com.eazyai.ai.nexus.api.observability;

import java.util.Map;

/**
 * 指标收集器接口
 */
public interface MetricsCollector {

    /**
     * 记录计数
     *
     * @param name 指标名称
     * @param count 计数值
     * @param tags 标签
     */
    void count(String name, long count, Map<String, String> tags);

    /**
     * 记录计时
     *
     * @param name 指标名称
     * @param duration 耗时（毫秒）
     * @param tags 标签
     */
    void timing(String name, long duration, Map<String, String> tags);

    /**
     * 记录 gauge 值
     *
     * @param name 指标名称
     * @param value 值
     * @param tags 标签
     */
    void gauge(String name, double value, Map<String, String> tags);

    /**
     * 记录直方图
     *
     * @param name 指标名称
     * @param value 值
     * @param tags 标签
     */
    void histogram(String name, long value, Map<String, String> tags);

    /**
     * 默认实现 - 记录计数
     */
    default void increment(String name, Map<String, String> tags) {
        count(name, 1, tags);
    }
}
