package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiSession;
import com.eazyai.ai.nexus.infra.dal.mapper.AiSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 会话 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiSessionRepository {

    private final AiSessionMapper mapper;

    public Optional<AiSession> findById(String sessionId) {
        return Optional.ofNullable(mapper.selectById(sessionId));
    }

    public List<AiSession> findByUserIdAndAppId(String userId, String appId) {
        LambdaQueryWrapper<AiSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSession::getUserId, userId)
               .eq(AiSession::getAppId, appId)
               .orderByDesc(AiSession::getUpdateTime);
        return mapper.selectList(wrapper);
    }

    public List<AiSession> findActiveByUserId(String userId, String appId) {
        LambdaQueryWrapper<AiSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSession::getUserId, userId)
               .eq(AiSession::getAppId, appId)
               .eq(AiSession::getStatus, 1)
               .orderByDesc(AiSession::getUpdateTime);
        return mapper.selectList(wrapper);
    }

    public int updateMessageCount(String sessionId, int increment) {
        AiSession session = mapper.selectById(sessionId);
        if (session != null) {
            session.setMessageCount(session.getMessageCount() + increment);
            return mapper.updateById(session);
        }
        return 0;
    }

    public int insert(AiSession session) {
        return mapper.insert(session);
    }

    public int updateById(AiSession session) {
        return mapper.updateById(session);
    }

    public int deleteById(String sessionId) {
        return mapper.deleteById(sessionId);
    }
}
