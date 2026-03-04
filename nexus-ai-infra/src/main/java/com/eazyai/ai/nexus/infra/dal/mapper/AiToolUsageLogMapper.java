package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiToolUsageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 工具使用日志 Mapper
 */
@Mapper
public interface AiToolUsageLogMapper extends BaseMapper<AiToolUsageLog> {

    /**
     * 统计工具成功率
     * 返回 Map<toolId, successRate>
     */
    @Select("""
            SELECT tool_id, 
                   CAST(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(*) as success_rate
            FROM ai_tool_usage_log
            WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY tool_id
            HAVING COUNT(*) >= #{minCalls}
            """)
    List<Map<String, Object>> calculateSuccessRate(@Param("days") int days, @Param("minCalls") int minCalls);

    /**
     * 统计应用下各工具调用次数和成功率
     */
    @Select("""
            SELECT tool_id,
                   COUNT(*) as total_calls,
                   SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as success_calls,
                   AVG(execution_time) as avg_execution_time
            FROM ai_tool_usage_log
            WHERE app_id = #{appId}
              AND create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY tool_id
            """)
    List<Map<String, Object>> getToolStatsByApp(@Param("appId") String appId, @Param("days") int days);

    /**
     * 获取最近调用的工具列表（按调用次数排序）
     */
    @Select("""
            SELECT tool_id, COUNT(*) as call_count
            FROM ai_tool_usage_log
            WHERE app_id = #{appId}
              AND create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY tool_id
            ORDER BY call_count DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> getRecentUsedTools(@Param("appId") String appId, @Param("days") int days, @Param("limit") int limit);
}
