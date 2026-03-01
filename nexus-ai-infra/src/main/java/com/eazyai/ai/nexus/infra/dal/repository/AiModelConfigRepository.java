package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiModelConfig;
import com.eazyai.ai.nexus.infra.dal.mapper.AiModelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型配置 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiModelConfigRepository {

    private final AiModelConfigMapper mapper;

    public Optional<AiModelConfig> findById(String modelId) {
        return Optional.ofNullable(mapper.selectById(modelId));
    }

    public List<AiModelConfig> findByProvider(String provider) {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getProvider, provider);
        return mapper.selectList(wrapper);
    }

    public List<AiModelConfig> findByModelType(String modelType) {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getModelType, modelType);
        return mapper.selectList(wrapper);
    }

    public List<AiModelConfig> findAllEnabled() {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getStatus, 1);
        return mapper.selectList(wrapper);
    }

    public Optional<AiModelConfig> findEnabledByModelId(String modelId) {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getModelId, modelId)
               .eq(AiModelConfig::getStatus, 1);
        return Optional.ofNullable(mapper.selectOne(wrapper));
    }

    public int insert(AiModelConfig config) {
        return mapper.insert(config);
    }

    public int updateById(AiModelConfig config) {
        return mapper.updateById(config);
    }

    public int deleteById(String modelId) {
        return mapper.deleteById(modelId);
    }
}
