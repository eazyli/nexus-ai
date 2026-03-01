package com.eazyai.ai.nexus.application.app;

import com.eazyai.ai.nexus.api.application.AppDescriptor;
import com.eazyai.ai.nexus.infra.dal.entity.AiApp;
import com.eazyai.ai.nexus.infra.dal.repository.AiAppRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AiAppRepository aiAppRepository;

    /**
     * 注册应用
     */
    public AppDescriptor registerApp(AppDescriptor descriptor) {
        AiApp app = toEntity(descriptor);
        app.setCreateTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        app.setStatus(1); // 默认启用
        app.setAppSecret(generateAppSecret()); // 自动生成应用密钥
        
        aiAppRepository.insert(app);
        log.info("注册应用: {} - {}", app.getAppId(), app.getAppName());
        
        return toDescriptor(app);
    }

    /**
     * 获取应用
     */
    public Optional<AppDescriptor> getApp(String appId) {
        return aiAppRepository.findById(appId)
                .map(this::toDescriptor);
    }

    /**
     * 获取所有应用
     */
    public List<AppDescriptor> getAllApps() {
        return aiAppRepository.findAllEnabled().stream()
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 更新应用
     */
    public AppDescriptor updateApp(String appId, AppDescriptor descriptor) {
        AiApp existing = aiAppRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("应用不存在: " + appId));
        
        AiApp app = toEntity(descriptor);
        app.setAppId(appId);
        app.setCreateTime(existing.getCreateTime());
        app.setUpdateTime(LocalDateTime.now());
        app.setStatus(existing.getStatus());
        
        aiAppRepository.updateById(app);
        log.info("更新应用: {}", appId);
        
        return toDescriptor(app);
    }

    /**
     * 删除应用
     */
    public void deleteApp(String appId) {
        aiAppRepository.deleteById(appId);
        log.info("删除应用: {}", appId);
    }

    /**
     * 启用应用
     */
    public void enableApp(String appId) {
        AiApp app = aiAppRepository.findById(appId).orElse(null);
        if (app != null) {
            app.setStatus(1);
            app.setUpdateTime(LocalDateTime.now());
            aiAppRepository.updateById(app);
            log.info("启用应用: {}", appId);
        }
    }

    /**
     * 禁用应用
     */
    public void disableApp(String appId) {
        AiApp app = aiAppRepository.findById(appId).orElse(null);
        if (app != null) {
            app.setStatus(0);
            app.setUpdateTime(LocalDateTime.now());
            aiAppRepository.updateById(app);
            log.info("禁用应用: {}", appId);
        }
    }

    /**
     * 检查应用是否有效
     */
    public boolean isAppValid(String appId) {
        return aiAppRepository.findByAppIdAndStatus(appId, 1).isPresent();
    }

    /**
     * 生成应用密钥
     */
    private String generateAppSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取应用配置
     */
    public Optional<AppDescriptor.AppConfig> getAppConfig(String appId) {
        return getApp(appId).map(AppDescriptor::getConfig);
    }

    /**
     * 实体转描述符
     */
    private AppDescriptor toDescriptor(AiApp app) {
        if (app == null) return null;
        
        AppDescriptor descriptor = new AppDescriptor();
        descriptor.setAppId(app.getAppId());
        descriptor.setName(app.getAppName());
        descriptor.setDescription(app.getDescription());
        descriptor.setType(app.getAppType());
        descriptor.setTenantId(app.getTenantId());
        descriptor.setEnabled(app.getStatus() != null && app.getStatus() == 1);
        
        // 解析能力ID列表
        if (app.getAbilityIds() != null && !app.getAbilityIds().isEmpty()) {
            descriptor.setSceneIds(Arrays.asList(app.getAbilityIds().split(",")));
        }
        
        // 构建配置
        AppDescriptor.AppConfig config = AppDescriptor.AppConfig.builder()
                .defaultModelId(app.getDefaultModelId())
                .systemPrompt(app.getSystemPrompt())
                .temperature(app.getTemperature() != null ? app.getTemperature().doubleValue() : null)
                .maxTokens(app.getMaxTokens())
                .extra(app.getExtraConfig())
                .build();
        
        // 限流配置
        if (app.getQpsLimit() != null || app.getDailyLimit() != null) {
            AppDescriptor.RateLimitConfig rateLimit = AppDescriptor.RateLimitConfig.builder()
                    .requestsPerSecond(app.getQpsLimit())
                    .requestsPerDay(app.getDailyLimit())
                    .build();
            config.setRateLimit(rateLimit);
        }
        descriptor.setConfig(config);
        
        return descriptor;
    }

    /**
     * 描述符转实体
     */
    private AiApp toEntity(AppDescriptor descriptor) {
        AiApp app = new AiApp();
        app.setAppId(descriptor.getAppId() != null ? descriptor.getAppId() : UUID.randomUUID().toString());
        app.setAppName(descriptor.getName());
        app.setDescription(descriptor.getDescription());
        app.setAppType(descriptor.getType());
        app.setTenantId(descriptor.getTenantId());
        app.setStatus(descriptor.getEnabled() != null && descriptor.getEnabled() ? 1 : 0);
        
        if (descriptor.getSceneIds() != null && !descriptor.getSceneIds().isEmpty()) {
            app.setAbilityIds(String.join(",", descriptor.getSceneIds()));
        }
        
        if (descriptor.getConfig() != null) {
            AppDescriptor.AppConfig config = descriptor.getConfig();
            app.setDefaultModelId(config.getDefaultModelId());
            app.setSystemPrompt(config.getSystemPrompt());
            app.setMaxTokens(config.getMaxTokens());
            if (config.getTemperature() != null) {
                app.setTemperature(BigDecimal.valueOf(config.getTemperature()));
            }
            app.setExtraConfig(config.getExtra());
            
            if (config.getRateLimit() != null) {
                app.setQpsLimit(config.getRateLimit().getRequestsPerSecond());
                app.setDailyLimit(config.getRateLimit().getRequestsPerDay());
            }
        }
        
        return app;
    }
}
