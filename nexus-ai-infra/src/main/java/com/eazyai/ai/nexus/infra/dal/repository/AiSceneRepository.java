package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiScene;
import com.eazyai.ai.nexus.infra.dal.mapper.AiSceneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 场景配置 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiSceneRepository {

    private final AiSceneMapper mapper;

    public Optional<AiScene> findById(String sceneId) {
        return Optional.ofNullable(mapper.selectById(sceneId));
    }

    public List<AiScene> findByAppId(String appId) {
        LambdaQueryWrapper<AiScene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiScene::getAppId, appId);
        return mapper.selectList(wrapper);
    }

    public List<AiScene> findBySceneType(String sceneType) {
        LambdaQueryWrapper<AiScene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiScene::getSceneType, sceneType);
        return mapper.selectList(wrapper);
    }

    public List<AiScene> findAllEnabled() {
        LambdaQueryWrapper<AiScene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiScene::getStatus, 1)
               .orderByDesc(AiScene::getPriority);
        return mapper.selectList(wrapper);
    }

    public List<AiScene> findEnabledByAppId(String appId) {
        LambdaQueryWrapper<AiScene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiScene::getAppId, appId)
               .eq(AiScene::getStatus, 1)
               .orderByDesc(AiScene::getPriority);
        return mapper.selectList(wrapper);
    }

    public int insert(AiScene scene) {
        return mapper.insert(scene);
    }

    public int updateById(AiScene scene) {
        return mapper.updateById(scene);
    }

    public int deleteById(String sceneId) {
        return mapper.deleteById(sceneId);
    }
}
