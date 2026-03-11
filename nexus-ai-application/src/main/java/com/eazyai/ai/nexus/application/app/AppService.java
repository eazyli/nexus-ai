package com.eazyai.ai.nexus.application.app;

import com.eazyai.ai.nexus.api.application.AppDescriptor;
import com.eazyai.ai.nexus.api.application.AppRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用管理服务
 * 
 * <p>应用层服务，负责应用的业务编排</p>
 * <p>依赖 api 层定义的 Repository 接口，由 infra 层实现</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;

    /**
     * 注册应用
     */
    public AppDescriptor registerApp(AppDescriptor descriptor) {
        if (descriptor.getAppId() == null) {
            descriptor.setAppId(UUID.randomUUID().toString());
        }
        descriptor.setEnabled(true);
        if (descriptor.getCollaborationMode() == null || descriptor.getCollaborationMode().isEmpty()) {
            descriptor.setCollaborationMode("single");
        }
        
        appRepository.save(descriptor);
        log.info("注册应用: {} - {}", descriptor.getAppId(), descriptor.getName());
        
        return descriptor;
    }

    /**
     * 获取应用
     */
    public Optional<AppDescriptor> getApp(String appId) {
        return appRepository.findById(appId);
    }

    /**
     * 获取所有应用
     */
    public List<AppDescriptor> getAllApps() {
        return appRepository.findAllEnabled();
    }

    /**
     * 按协作模式获取应用
     */
    public List<AppDescriptor> getAppsByCollaborationMode(String collaborationMode) {
        return appRepository.findAllEnabled().stream()
                .filter(app -> collaborationMode.equals(app.getCollaborationMode()))
                .collect(Collectors.toList());
    }

    /**
     * 按能力标签获取应用（用于多智能体匹配）
     */
    public List<AppDescriptor> getAppsByCapability(String capability) {
        return appRepository.findAllEnabled().stream()
                .filter(app -> app.getCapabilities() != null && app.getCapabilities().contains(capability))
                .collect(Collectors.toList());
    }

    /**
     * 更新应用
     */
    public AppDescriptor updateApp(String appId, AppDescriptor descriptor) {
        AppDescriptor existing = appRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("应用不存在: " + appId));
        
        descriptor.setAppId(appId);
        descriptor.setEnabled(existing.getEnabled());
        
        appRepository.update(descriptor);
        log.info("更新应用: {}", appId);
        
        return descriptor;
    }

    /**
     * 更新应用交互配置（开场白、示例问题）
     */
    public void updateInteraction(String appId, String greeting, List<String> sampleQuestions) {
        AppDescriptor app = appRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("应用不存在: " + appId));
        
        if (app.getConfig() == null) {
            AppDescriptor.AppConfig config = AppDescriptor.AppConfig.builder().build();
            app.setConfig(config);
        }
        app.getConfig().setGreeting(greeting);
        app.getConfig().setSampleQuestions(sampleQuestions);
        
        appRepository.update(app);
        log.info("更新应用交互配置: {}", appId);
    }

    /**
     * 删除应用
     */
    public void deleteApp(String appId) {
        appRepository.deleteById(appId);
        log.info("删除应用: {}", appId);
    }

    /**
     * 启用应用
     */
    public void enableApp(String appId) {
        AppDescriptor app = appRepository.findById(appId).orElse(null);
        if (app != null) {
            app.setEnabled(true);
            appRepository.update(app);
            log.info("启用应用: {}", appId);
        }
    }

    /**
     * 禁用应用
     */
    public void disableApp(String appId) {
        AppDescriptor app = appRepository.findById(appId).orElse(null);
        if (app != null) {
            app.setEnabled(false);
            appRepository.update(app);
            log.info("禁用应用: {}", appId);
        }
    }

    /**
     * 检查应用是否有效
     */
    public boolean isAppValid(String appId) {
        return appRepository.findByAppIdAndStatus(appId, 1).isPresent();
    }

    /**
     * 获取应用配置
     */
    public Optional<AppDescriptor.AppConfig> getAppConfig(String appId) {
        return getApp(appId).map(AppDescriptor::getConfig);
    }
}
