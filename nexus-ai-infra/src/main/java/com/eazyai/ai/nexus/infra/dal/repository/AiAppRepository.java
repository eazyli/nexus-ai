package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiApp;
import com.eazyai.ai.nexus.infra.dal.mapper.AiAppMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 应用管理 Repository
 */
@Repository
@RequiredArgsConstructor
public class AiAppRepository {

    private final AiAppMapper aiAppMapper;

    public Optional<AiApp> findById(String appId) {
        return Optional.ofNullable(aiAppMapper.selectById(appId));
    }

    public Optional<AiApp> findByAppIdAndStatus(String appId, Integer status) {
        LambdaQueryWrapper<AiApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApp::getAppId, appId).eq(AiApp::getStatus, status);
        return Optional.ofNullable(aiAppMapper.selectOne(wrapper));
    }

    public List<AiApp> findByTenantId(String tenantId) {
        LambdaQueryWrapper<AiApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApp::getTenantId, tenantId);
        return aiAppMapper.selectList(wrapper);
    }

    public List<AiApp> findAllEnabled() {
        LambdaQueryWrapper<AiApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApp::getStatus, 1);
        return aiAppMapper.selectList(wrapper);
    }

    public int insert(AiApp app) {
        return aiAppMapper.insert(app);
    }

    public int updateById(AiApp app) {
        return aiAppMapper.updateById(app);
    }

    public int deleteById(String appId) {
        return aiAppMapper.deleteById(appId);
    }
}
