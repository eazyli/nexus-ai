package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiCallLog;
import com.eazyai.ai.nexus.infra.dal.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI调用日志 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiCallLogRepository {

    private final AiCallLogMapper mapper;

    public List<AiCallLog> findByAppId(String appId, int limit) {
        LambdaQueryWrapper<AiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiCallLog::getAppId, appId)
               .orderByDesc(AiCallLog::getCreateTime)
               .last("LIMIT " + limit);
        return mapper.selectList(wrapper);
    }

    public List<AiCallLog> findBySessionId(String sessionId) {
        LambdaQueryWrapper<AiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiCallLog::getSessionId, sessionId)
               .orderByDesc(AiCallLog::getCreateTime);
        return mapper.selectList(wrapper);
    }

    public List<AiCallLog> findByUserId(String userId, int limit) {
        LambdaQueryWrapper<AiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiCallLog::getUserId, userId)
               .orderByDesc(AiCallLog::getCreateTime)
               .last("LIMIT " + limit);
        return mapper.selectList(wrapper);
    }

    public long countByAppIdAndTimeRange(String appId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<AiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiCallLog::getAppId, appId)
               .between(AiCallLog::getCreateTime, start, end);
        return mapper.selectCount(wrapper);
    }

    public long countByAppIdToday(String appId) {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = LocalDateTime.now();
        return countByAppIdAndTimeRange(appId, start, end);
    }

    public int insert(AiCallLog log) {
        return mapper.insert(log);
    }
}
