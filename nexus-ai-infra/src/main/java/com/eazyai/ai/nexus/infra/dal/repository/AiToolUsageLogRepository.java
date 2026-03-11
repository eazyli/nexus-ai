package com.eazyai.ai.nexus.infra.dal.repository;

import com.eazyai.ai.nexus.core.tool.ToolUsageLog;
import com.eazyai.ai.nexus.core.tool.ToolUsageLogRepository;
import com.eazyai.ai.nexus.infra.dal.entity.AiToolUsageLog;
import com.eazyai.ai.nexus.infra.dal.mapper.AiToolUsageLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具使用日志 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiToolUsageLogRepository implements ToolUsageLogRepository {

    private final AiToolUsageLogMapper mapper;

    @Override
    public int insert(ToolUsageLog log) {
        if (log instanceof AiToolUsageLog aiLog) {
            return mapper.insert(aiLog);
        }
        // 如果不是 AiToolUsageLog 类型，转换后插入
        AiToolUsageLog aiLog = new AiToolUsageLog();
        aiLog.setToolId(log.getToolId());
        aiLog.setAppId(log.getAppId());
        aiLog.setSessionId(log.getSessionId());
        aiLog.setUserId(log.getUserId());
        aiLog.setRequestId(log.getRequestId());
        aiLog.setSuccess(log.getSuccess());
        aiLog.setErrorCode(log.getErrorCode());
        aiLog.setErrorMsg(log.getErrorMsg());
        aiLog.setExecutionTime(log.getExecutionTime());
        aiLog.setRetryCount(log.getRetryCount());
        aiLog.setCreateTime(log.getCreateTime());
        return mapper.insert(aiLog);
    }

    @Override
    public Map<String, Double> getSuccessRates(int days, int minCalls) {
        Map<String, Double> result = new HashMap<>();
        List<Map<String, Object>> stats = mapper.calculateSuccessRate(days, minCalls);
        for (Map<String, Object> stat : stats) {
            String toolId = (String) stat.get("tool_id");
            // 使用 Number 类型处理 BigDecimal
            Double successRate = stat.get("success_rate") != null
                ? ((Number) stat.get("success_rate")).doubleValue()
                : null;
            if (successRate != null) {
                result.put(toolId, successRate);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getToolStatsByApp(String appId, int days) {
        return mapper.getToolStatsByApp(appId, days);
    }

    @Override
    public List<Map<String, Object>> getRecentUsedTools(String appId, int days, int limit) {
        return mapper.getRecentUsedTools(appId, days, limit);
    }
}
