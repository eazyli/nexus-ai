package com.eazyai.ai.nexus.core.tool;

import java.util.List;
import java.util.Map;

/**
 * 工具使用日志仓储接口 - 核心层定义
 */
public interface ToolUsageLogRepository {

    /**
     * 插入日志
     */
    int insert(ToolUsageLog log);

    /**
     * 计算工具成功率（近N天，最少M次调用）
     */
    Map<String, Double> getSuccessRates(int days, int minCalls);

    /**
     * 获取应用下各工具统计信息
     */
    List<Map<String, Object>> getToolStatsByApp(String appId, int days);

    /**
     * 获取最近使用的工具列表
     */
    List<Map<String, Object>> getRecentUsedTools(String appId, int days, int limit);
}
