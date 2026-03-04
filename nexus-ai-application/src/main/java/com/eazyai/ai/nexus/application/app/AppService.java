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
        
        // 设置默认协作模式
        if (app.getCollaborationMode() == null || app.getCollaborationMode().isEmpty()) {
            app.setCollaborationMode("single");
        }
        
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
     * 按协作模式获取应用
     */
    public List<AppDescriptor> getAppsByCollaborationMode(String collaborationMode) {
        return aiAppRepository.findAllEnabled().stream()
                .filter(app -> collaborationMode.equals(app.getCollaborationMode()))
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 按能力标签获取应用（用于多智能体匹配）
     */
    public List<AppDescriptor> getAppsByCapability(String capability) {
        return aiAppRepository.findAllEnabled().stream()
                .filter(app -> app.getCapabilities() != null && app.getCapabilities().contains(capability))
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
        app.setAppSecret(existing.getAppSecret()); // 保留原有密钥
        
        aiAppRepository.updateById(app);
        log.info("更新应用: {}", appId);
        
        return toDescriptor(app);
    }

    /**
     * 更新应用交互配置（开场白、示例问题）
     */
    public void updateInteraction(String appId, String greeting, List<String> sampleQuestions) {
        AiApp app = aiAppRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("应用不存在: " + appId));
        
        app.setGreeting(greeting);
        app.setSampleQuestions(sampleQuestions);
        app.setUpdateTime(LocalDateTime.now());
        
        aiAppRepository.updateById(app);
        log.info("更新应用交互配置: {}", appId);
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
        descriptor.setCapabilities(app.getCapabilities());
        descriptor.setCollaborationMode(app.getCollaborationMode());
        descriptor.setExecutionConfig(app.getExecutionConfig());
        descriptor.setPriority(app.getPriority());
        descriptor.setIcon(app.getIcon());
        
        // 解析能力ID列表
        if (app.getAbilityIds() != null && !app.getAbilityIds().isEmpty()) {
            descriptor.setSceneIds(Arrays.asList(app.getAbilityIds().split(",")));
        }
        
        // 构建配置
        AppDescriptor.AppConfig.AppConfigBuilder configBuilder = AppDescriptor.AppConfig.builder()
                .defaultModelId(app.getDefaultModelId())
                .systemPrompt(app.getSystemPrompt())
                .temperature(app.getTemperature() != null ? app.getTemperature().doubleValue() : null)
                .maxTokens(app.getMaxTokens())
                .extra(app.getExtraConfig())
                .greeting(app.getGreeting())
                .sampleQuestions(app.getSampleQuestions());
        
        // 转换变量定义
        if (app.getVariables() != null) {
            Map<String, AppDescriptor.VariableDefinition> variables = convertVariables(app.getVariables());
            configBuilder.variables(variables);
        }
        
        AppDescriptor.AppConfig config = configBuilder.build();
        
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
     * 转换变量定义
     */
    @SuppressWarnings("unchecked")
    private Map<String, AppDescriptor.VariableDefinition> convertVariables(Map<String, Object> variablesMap) {
        Map<String, AppDescriptor.VariableDefinition> result = new HashMap<>();
        
        variablesMap.forEach((name, obj) -> {
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                AppDescriptor.VariableDefinition.VariableDefinitionBuilder builder = AppDescriptor.VariableDefinition.builder()
                        .name(name)
                        .type((String) map.getOrDefault("type", "string"))
                        .defaultValue(map.get("defaultValue"))
                        .required((Boolean) map.getOrDefault("required", false))
                        .description((String) map.get("description"));
                
                Object validation = map.get("validation");
                if (validation instanceof Map) {
                    Map<String, Object> validationMap = (Map<String, Object>) validation;
                    AppDescriptor.VariableDefinition.ValidationRule rule = AppDescriptor.VariableDefinition.ValidationRule.builder()
                            .minLength((Integer) validationMap.get("minLength"))
                            .maxLength((Integer) validationMap.get("maxLength"))
                            .pattern((String) validationMap.get("pattern"))
                            .enumValues((List<String>) validationMap.get("enumValues"))
                            .build();
                    builder.validation(rule);
                }
                
                result.put(name, builder.build());
            }
        });
        
        return result;
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
        app.setCapabilities(descriptor.getCapabilities());
        app.setCollaborationMode(descriptor.getCollaborationMode() != null ? descriptor.getCollaborationMode() : "single");
        app.setExecutionConfig(descriptor.getExecutionConfig());
        app.setPriority(descriptor.getPriority());
        app.setIcon(descriptor.getIcon());
        
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
            app.setGreeting(config.getGreeting());
            app.setSampleQuestions(config.getSampleQuestions());
            
            // 转换变量定义
            if (config.getVariables() != null) {
                app.setVariables(convertVariablesToMap(config.getVariables()));
            }
            
            if (config.getRateLimit() != null) {
                app.setQpsLimit(config.getRateLimit().getRequestsPerSecond());
                app.setDailyLimit(config.getRateLimit().getRequestsPerDay());
            }
        }
        
        return app;
    }

    /**
     * 转换变量定义到Map
     */
    private Map<String, Object> convertVariablesToMap(Map<String, AppDescriptor.VariableDefinition> variables) {
        Map<String, Object> result = new HashMap<>();
        
        variables.forEach((name, def) -> {
            Map<String, Object> varMap = new HashMap<>();
            varMap.put("name", def.getName());
            varMap.put("type", def.getType());
            varMap.put("defaultValue", def.getDefaultValue());
            varMap.put("required", def.getRequired());
            varMap.put("description", def.getDescription());
            
            if (def.getValidation() != null) {
                Map<String, Object> validationMap = new HashMap<>();
                validationMap.put("minLength", def.getValidation().getMinLength());
                validationMap.put("maxLength", def.getValidation().getMaxLength());
                validationMap.put("pattern", def.getValidation().getPattern());
                validationMap.put("enumValues", def.getValidation().getEnumValues());
                varMap.put("validation", validationMap);
            }
            
            result.put(name, varMap);
        });
        
        return result;
    }
}
