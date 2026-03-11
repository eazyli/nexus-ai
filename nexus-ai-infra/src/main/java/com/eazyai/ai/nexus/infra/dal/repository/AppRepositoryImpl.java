package com.eazyai.ai.nexus.infra.dal.repository;

import com.eazyai.ai.nexus.api.application.AppDescriptor;
import com.eazyai.ai.nexus.api.application.AppRepository;
import com.eazyai.ai.nexus.infra.dal.entity.AiApp;
import com.eazyai.ai.nexus.infra.dal.mapper.AiAppMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 应用仓储实现
 * 
 * <p>实现 api 层定义的 AppRepository 接口</p>
 * <p>负责 AppDescriptor 与 AiApp Entity 之间的转换</p>
 */
@Repository
@RequiredArgsConstructor
public class AppRepositoryImpl implements AppRepository {

    private final AiAppMapper aiAppMapper;

    @Override
    public Optional<AppDescriptor> findById(String appId) {
        return Optional.ofNullable(aiAppMapper.selectById(appId))
                .map(this::toDescriptor);
    }

    @Override
    public Optional<AppDescriptor> findByAppIdAndStatus(String appId, Integer status) {
        return findAllEnabled().stream()
                .filter(app -> appId.equals(app.getAppId()))
                .findFirst();
    }

    @Override
    public List<AppDescriptor> findByTenantId(String tenantId) {
        return aiAppMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiApp>()
                        .eq(AiApp::getTenantId, tenantId)
        ).stream().map(this::toDescriptor).collect(Collectors.toList());
    }

    @Override
    public List<AppDescriptor> findAllEnabled() {
        return aiAppMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiApp>()
                        .eq(AiApp::getStatus, 1)
        ).stream().map(this::toDescriptor).collect(Collectors.toList());
    }

    @Override
    public void save(AppDescriptor descriptor) {
        AiApp app = toEntity(descriptor);
        app.setCreateTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        app.setStatus(1);
        if (app.getAppSecret() == null || app.getAppSecret().isEmpty()) {
            app.setAppSecret(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        aiAppMapper.insert(app);
    }

    @Override
    public void update(AppDescriptor descriptor) {
        AiApp app = toEntity(descriptor);
        app.setUpdateTime(LocalDateTime.now());
        aiAppMapper.updateById(app);
    }

    @Override
    public void deleteById(String appId) {
        aiAppMapper.deleteById(appId);
    }

    /**
     * Entity 转 Descriptor
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

        // 构建配置
        AppDescriptor.AppConfig.AppConfigBuilder configBuilder = AppDescriptor.AppConfig.builder()
                .defaultModelId(app.getDefaultModelId())
                .systemPrompt(app.getSystemPrompt())
                .temperature(app.getTemperature() != null ? app.getTemperature().doubleValue() : null)
                .maxTokens(app.getMaxTokens())
                .extra(app.getExtraConfig())
                .greeting(app.getGreeting())
                .sampleQuestions(app.getSampleQuestions());

        if (app.getVariables() != null) {
            configBuilder.variables(convertVariables(app.getVariables()));
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
     * Descriptor 转 Entity
     */
    private AiApp toEntity(AppDescriptor descriptor) {
        AiApp app = new AiApp();
        app.setAppId(descriptor.getAppId());
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
     * 转换变量定义
     */
    @SuppressWarnings("unchecked")
    private java.util.Map<String, AppDescriptor.VariableDefinition> convertVariables(java.util.Map<String, Object> variablesMap) {
        java.util.Map<String, AppDescriptor.VariableDefinition> result = new java.util.HashMap<>();

        variablesMap.forEach((name, obj) -> {
            if (obj instanceof java.util.Map) {
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                AppDescriptor.VariableDefinition.VariableDefinitionBuilder builder = AppDescriptor.VariableDefinition.builder()
                        .name(name)
                        .type((String) map.getOrDefault("type", "string"))
                        .defaultValue(map.get("defaultValue"))
                        .required((Boolean) map.getOrDefault("required", false))
                        .description((String) map.get("description"));

                Object validation = map.get("validation");
                if (validation instanceof java.util.Map) {
                    java.util.Map<String, Object> validationMap = (java.util.Map<String, Object>) validation;
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
     * 将 VariableDefinition Map 转换为 Object Map（用于存储）
     */
    private java.util.Map<String, Object> convertVariablesToMap(java.util.Map<String, AppDescriptor.VariableDefinition> variables) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        variables.forEach((name, def) -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("type", def.getType());
            map.put("defaultValue", def.getDefaultValue());
            map.put("required", def.getRequired());
            map.put("description", def.getDescription());
            
            if (def.getValidation() != null) {
                AppDescriptor.VariableDefinition.ValidationRule rule = def.getValidation();
                java.util.Map<String, Object> validationMap = new java.util.HashMap<>();
                validationMap.put("minLength", rule.getMinLength());
                validationMap.put("maxLength", rule.getMaxLength());
                validationMap.put("pattern", rule.getPattern());
                validationMap.put("enumValues", rule.getEnumValues());
                map.put("validation", validationMap);
            }
            
            result.put(name, map);
        });
        
        return result;
    }
}
