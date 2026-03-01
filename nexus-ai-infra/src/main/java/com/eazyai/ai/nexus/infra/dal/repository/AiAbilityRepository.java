package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiAbility;
import com.eazyai.ai.nexus.infra.dal.mapper.AiAbilityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI能力配置 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiAbilityRepository {

    private final AiAbilityMapper mapper;

    public Optional<AiAbility> findById(String abilityId) {
        return Optional.ofNullable(mapper.selectById(abilityId));
    }

    public List<AiAbility> findByAbilityType(String abilityType) {
        LambdaQueryWrapper<AiAbility> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAbility::getAbilityType, abilityType);
        return mapper.selectList(wrapper);
    }

    public List<AiAbility> findAllEnabled() {
        LambdaQueryWrapper<AiAbility> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAbility::getStatus, 1);
        return mapper.selectList(wrapper);
    }

    public List<AiAbility> findByIds(List<String> abilityIds) {
        if (abilityIds == null || abilityIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(abilityIds);
    }

    public int insert(AiAbility ability) {
        return mapper.insert(ability);
    }

    public int updateById(AiAbility ability) {
        return mapper.updateById(ability);
    }

    public int deleteById(String abilityId) {
        return mapper.deleteById(abilityId);
    }
}
