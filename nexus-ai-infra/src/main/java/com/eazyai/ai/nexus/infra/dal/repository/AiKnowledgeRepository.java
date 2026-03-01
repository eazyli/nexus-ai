package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiKnowledge;
import com.eazyai.ai.nexus.infra.dal.mapper.AiKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识库 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiKnowledgeRepository {

    private final AiKnowledgeMapper mapper;

    public Optional<AiKnowledge> findById(String knowledgeId) {
        return Optional.ofNullable(mapper.selectById(knowledgeId));
    }

    public List<AiKnowledge> findByAppId(String appId) {
        LambdaQueryWrapper<AiKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledge::getAppId, appId);
        return mapper.selectList(wrapper);
    }

    public List<AiKnowledge> findByKnowledgeType(String knowledgeType) {
        LambdaQueryWrapper<AiKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledge::getKnowledgeType, knowledgeType);
        return mapper.selectList(wrapper);
    }

    public List<AiKnowledge> findAllEnabled() {
        LambdaQueryWrapper<AiKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledge::getStatus, 1);
        return mapper.selectList(wrapper);
    }

    public List<AiKnowledge> findByIds(List<String> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(knowledgeIds);
    }

    public int insert(AiKnowledge knowledge) {
        return mapper.insert(knowledge);
    }

    public int updateById(AiKnowledge knowledge) {
        return mapper.updateById(knowledge);
    }

    public int deleteById(String knowledgeId) {
        return mapper.deleteById(knowledgeId);
    }
}
