package com.eazyai.ai.nexus.core.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具使用历史服务
 * 记录工具调用历史，提供成功率统计，辅助LLM进行工具选择
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolUsageHistoryService {

    private final ToolUsageLogRepository repository;

    /**
     * 成功率缓存（toolId -> successRate）
     */
    private final Map<String, Double> successRateCache = new ConcurrentHashMap<>();

    /**
     * 缓存更新时间
     */
    private volatile long cacheUpdateTime = 0;

    /**
     * 缓存有效期（5分钟）
     */
    private static final long CACHE_TTL = 5 * 60 * 1000;

    /**
     * 统计时间窗口（天）
     */
    private static final int STATS_DAYS = 30;

    /**
     * 最小调用次数阈值
     */
    private static final int MIN_CALLS = 3;

    /**
     * 记录工具调用日志（异步）
     *
     * @param toolId        工具ID
     * @param appId         应用ID
     * @param sessionId     会话ID
     * @param userId        用户ID
     * @param requestId     请求ID
     * @param success       是否成功
     * @param errorCode     错误码
     * @param errorMsg      错误信息
     * @param executionTime 执行耗时
     * @param retryCount    重试次数
     */
    @Async
    public void recordUsage(String toolId, String appId, String sessionId, String userId,
                            String requestId, boolean success, String errorCode, String errorMsg,
                            Long executionTime, Integer retryCount) {
        try {
            SimpleToolUsageLog logEntry = new SimpleToolUsageLog();
            logEntry.setToolId(toolId);
            logEntry.setAppId(appId);
            logEntry.setSessionId(sessionId);
            logEntry.setUserId(userId);
            logEntry.setRequestId(requestId);
            logEntry.setSuccess(success ? 1 : 0);
            logEntry.setErrorCode(errorCode);
            logEntry.setErrorMsg(errorMsg);
            logEntry.setExecutionTime(executionTime);
            logEntry.setRetryCount(retryCount);
            logEntry.setCreateTime(LocalDateTime.now());

            repository.insert(logEntry);
            log.debug("记录工具使用日志: toolId={}, success={}", toolId, success);
        } catch (Exception e) {
            log.error("记录工具使用日志失败: toolId={}", toolId, e);
        }
    }

    /**
     * 获取工具成功率
     * 
     * @param toolId 工具ID
     * @return 成功率（0-1），无历史数据返回null
     */
    public Double getSuccessRate(String toolId) {
        refreshCacheIfNeeded();
        return successRateCache.get(toolId);
    }

    /**
     * 批量获取工具成功率
     *
     * @param toolIds 工具ID列表
     * @return toolId -> successRate
     */
    public Map<String, Double> getSuccessRates(List<String> toolIds) {
        refreshCacheIfNeeded();
        Map<String, Double> result = new HashMap<>();
        for (String toolId : toolIds) {
            Double rate = successRateCache.get(toolId);
            if (rate != null) {
                result.put(toolId, rate);
            }
        }
        return result;
    }

    /**
     * 获取所有工具成功率
     */
    public Map<String, Double> getAllSuccessRates() {
        refreshCacheIfNeeded();
        return new HashMap<>(successRateCache);
    }

    /**
     * 获取应用下工具使用统计（供LLM参考）
     * 返回格式：
     * {
     *   "toolId1": {"totalCalls": 100, "successCalls": 95, "successRate": 0.95, "avgTime": 150},
     *   "toolId2": {...}
     * }
     *
     * @param appId 应用ID
     * @return 工具统计信息
     */
    public Map<String, Map<String, Object>> getToolStatsForApp(String appId) {
        List<Map<String, Object>> stats = repository.getToolStatsByApp(appId, STATS_DAYS);
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Map<String, Object> stat : stats) {
            String toolId = (String) stat.get("tool_id");
            Long totalCalls = ((Number) stat.get("total_calls")).longValue();
            Long successCalls = ((Number) stat.get("success_calls")).longValue();
            // 使用 Number 类型处理 BigDecimal
            Double avgTime = stat.get("avg_execution_time") != null 
                ? ((Number) stat.get("avg_execution_time")).doubleValue() 
                : null;

            Map<String, Object> toolStat = new HashMap<>();
            toolStat.put("totalCalls", totalCalls);
            toolStat.put("successCalls", successCalls);
            toolStat.put("successRate", totalCalls > 0 ? (double) successCalls / totalCalls : 0);
            toolStat.put("avgExecutionTime", avgTime != null ? avgTime.longValue() : 0);

            result.put(toolId, toolStat);
        }

        return result;
    }

    /**
     * 获取应用最近最常使用的工具列表
     *
     * @param appId 应用ID
     * @param limit 返回数量
     * @return 工具ID列表（按调用次数降序）
     */
    public List<String> getRecentUsedTools(String appId, int limit) {
        List<Map<String, Object>> tools = repository.getRecentUsedTools(appId, STATS_DAYS, limit);
        return tools.stream()
                .map(m -> (String) m.get("tool_id"))
                .toList();
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        try {
            Map<String, Double> newCache = repository.getSuccessRates(STATS_DAYS, MIN_CALLS);
            successRateCache.clear();
            successRateCache.putAll(newCache);
            cacheUpdateTime = System.currentTimeMillis();
            log.info("刷新工具成功率缓存，共{}个工具", newCache.size());
        } catch (Exception e) {
            log.error("刷新工具成功率缓存失败", e);
        }
    }

    /**
     * 检查并刷新缓存
     */
    private void refreshCacheIfNeeded() {
        if (System.currentTimeMillis() - cacheUpdateTime > CACHE_TTL) {
            synchronized (this) {
                if (System.currentTimeMillis() - cacheUpdateTime > CACHE_TTL) {
                    refreshCache();
                }
            }
        }
    }

    /**
     * 格式化工具统计信息供LLM使用
     *
     * @param appId 应用ID
     * @return 格式化的统计字符串
     */
    public String formatToolStatsForLLM(String appId) {
        Map<String, Map<String, Object>> stats = getToolStatsForApp(appId);
        if (stats.isEmpty()) {
            return "暂无工具使用历史数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 工具使用历史统计（近30天）\n");
        sb.append("| 工具ID | 调用次数 | 成功率 | 平均耗时(ms) |\n");
        sb.append("|--------|----------|--------|-------------|\n");

        stats.forEach((toolId, stat) -> {
            Double successRate = (Double) stat.get("successRate");
            sb.append(String.format("| %s | %d | %.1f%% | %d |\n",
                    toolId,
                    stat.get("totalCalls"),
                    successRate * 100,
                    stat.get("avgExecutionTime")));
        });

        return sb.toString();
    }
}
