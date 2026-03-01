package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiMemory;
import com.eazyai.ai.nexus.infra.dal.mapper.AiMemoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记忆 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiMemoryRepository {

    private final AiMemoryMapper mapper;

    public List<AiMemory> findBySessionId(String sessionId) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getSessionId, sessionId)
               .orderByAsc(AiMemory::getCreateTime);
        return mapper.selectList(wrapper);
    }

    public List<AiMemory> findByUserIdAndAppId(String userId, String appId) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
               .eq(AiMemory::getAppId, appId)
               .orderByDesc(AiMemory::getCreateTime);
        return mapper.selectList(wrapper);
    }

    public List<AiMemory> findLongTermMemory(String userId, String appId, int limit) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getUserId, userId)
               .eq(AiMemory::getAppId, appId)
               .eq(AiMemory::getMemoryType, "long")
               .orderByDesc(AiMemory::getCreateTime)
               .last("LIMIT " + limit);
        return mapper.selectList(wrapper);
    }

    public List<AiMemory> findRecentSessionMemory(String sessionId, int limit) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getSessionId, sessionId)
               .eq(AiMemory::getMemoryType, "short")
               .orderByDesc(AiMemory::getCreateTime)
               .last("LIMIT " + limit);
        return mapper.selectList(wrapper);
    }

    public int deleteBySessionId(String sessionId) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMemory::getSessionId, sessionId);
        return mapper.delete(wrapper);
    }

    public int deleteExpiredMemory() {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AiMemory::getExpireTime, LocalDateTime.now());
        return mapper.delete(wrapper);
    }

    public int insert(AiMemory memory) {
        return mapper.insert(memory);
    }

    public int insertBatch(List<AiMemory> memories) {
        int count = 0;
        for (AiMemory memory : memories) {
            count += mapper.insert(memory);
        }
        return count;
    }
}
