package com.eazyai.ai.nexus.application.capability;

import com.eazyai.ai.nexus.api.application.CapabilityDescriptor;
import com.eazyai.ai.nexus.infra.dal.entity.AiAbility;
import com.eazyai.ai.nexus.infra.dal.repository.AiAbilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 能力配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapabilityService {

    private final AiAbilityRepository aiAbilityRepository;

    /**
     * 注册能力
     */
    public CapabilityDescriptor registerCapability(CapabilityDescriptor descriptor) {
        AiAbility ability = toEntity(descriptor);
        ability.setCreateTime(LocalDateTime.now());
        ability.setUpdateTime(LocalDateTime.now());
        ability.setStatus(1); // 默认启用
        
        aiAbilityRepository.insert(ability);
        log.info("注册能力: {} - {} (类型: {})", 
                ability.getAbilityId(), ability.getAbilityName(), ability.getAbilityType());
        
        return toDescriptor(ability);
    }

    /**
     * 获取能力
     */
    public Optional<CapabilityDescriptor> getCapability(String capabilityId) {
        return aiAbilityRepository.findById(capabilityId)
                .map(this::toDescriptor);
    }

    /**
     * 获取所有能力
     */
    public List<CapabilityDescriptor> getAllCapabilities() {
        return aiAbilityRepository.findAllEnabled().stream()
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取能力
     */
    public List<CapabilityDescriptor> getCapabilitiesByType(String type) {
        return aiAbilityRepository.findByAbilityType(type).stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 批量获取能力
     */
    public List<CapabilityDescriptor> getCapabilities(Collection<String> capabilityIds) {
        return aiAbilityRepository.findByIds(new ArrayList<>(capabilityIds)).stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 更新能力
     */
    public CapabilityDescriptor updateCapability(String capabilityId, CapabilityDescriptor descriptor) {
        AiAbility existing = aiAbilityRepository.findById(capabilityId)
                .orElseThrow(() -> new IllegalArgumentException("能力不存在: " + capabilityId));
        
        AiAbility ability = toEntity(descriptor);
        ability.setAbilityId(capabilityId);
        ability.setCreateTime(existing.getCreateTime());
        ability.setUpdateTime(LocalDateTime.now());
        ability.setStatus(existing.getStatus());
        
        aiAbilityRepository.updateById(ability);
        log.info("更新能力: {}", capabilityId);
        
        return toDescriptor(ability);
    }

    /**
     * 删除能力
     */
    public void deleteCapability(String capabilityId) {
        aiAbilityRepository.deleteById(capabilityId);
        log.info("删除能力: {}", capabilityId);
    }

    /**
     * 启用能力
     */
    public void enableCapability(String capabilityId) {
        AiAbility ability = aiAbilityRepository.findById(capabilityId).orElse(null);
        if (ability != null) {
            ability.setStatus(1);
            ability.setUpdateTime(LocalDateTime.now());
            aiAbilityRepository.updateById(ability);
            log.info("启用能力: {}", capabilityId);
        }
    }

    /**
     * 禁用能力
     */
    public void disableCapability(String capabilityId) {
        AiAbility ability = aiAbilityRepository.findById(capabilityId).orElse(null);
        if (ability != null) {
            ability.setStatus(0);
            ability.setUpdateTime(LocalDateTime.now());
            aiAbilityRepository.updateById(ability);
            log.info("禁用能力: {}", capabilityId);
        }
    }

    /**
     * 获取能力参数定义
     */
    @SuppressWarnings("unchecked")
    public Optional<List<CapabilityDescriptor.ParamDefinition>> getParamDefinitions(String capabilityId) {
        return getCapability(capabilityId).map(CapabilityDescriptor::getParams);
    }

    /**
     * 实体转描述符
     */
    @SuppressWarnings("unchecked")
    private CapabilityDescriptor toDescriptor(AiAbility ability) {
        if (ability == null) return null;
        
        CapabilityDescriptor descriptor = new CapabilityDescriptor();
        descriptor.setCapabilityId(ability.getAbilityId());
        descriptor.setName(ability.getAbilityName());
        descriptor.setDescription(ability.getAbilityDesc());
        descriptor.setType(ability.getAbilityType());
        descriptor.setEnabled(ability.getStatus() != null && ability.getStatus() == 1);
        
        // 解析配置中的参数定义
        if (ability.getConfig() != null) {
            Map<String, Object> configMap = ability.getConfig();
            descriptor.setConfig(configMap);
            
            if (configMap.get("params") != null) {
                List<Map<String, Object>> paramsList = (List<Map<String, Object>>) configMap.get("params");
                List<CapabilityDescriptor.ParamDefinition> params = paramsList.stream()
                        .map(p -> CapabilityDescriptor.ParamDefinition.builder()
                                .name((String) p.get("name"))
                                .type((String) p.get("type"))
                                .description((String) p.get("description"))
                                .required((Boolean) p.get("required"))
                                .defaultValue(p.get("defaultValue"))
                                .options((List<String>) p.get("options"))
                                .build())
                        .collect(Collectors.toList());
                descriptor.setParams(params);
            }
        }
        
        return descriptor;
    }

    /**
     * 描述符转实体
     */
    private AiAbility toEntity(CapabilityDescriptor descriptor) {
        AiAbility ability = new AiAbility();
        ability.setAbilityId(descriptor.getCapabilityId() != null ? descriptor.getCapabilityId() : UUID.randomUUID().toString());
        ability.setAbilityName(descriptor.getName());
        ability.setAbilityDesc(descriptor.getDescription());
        ability.setAbilityType(descriptor.getType());
        ability.setStatus(descriptor.getEnabled() != null && descriptor.getEnabled() ? 1 : 0);
        
        // 转换配置
        Map<String, Object> configMap = new HashMap<>();
        if (descriptor.getConfig() != null) {
            configMap.putAll(descriptor.getConfig());
        }
        
        if (descriptor.getParams() != null && !descriptor.getParams().isEmpty()) {
            List<Map<String, Object>> paramsList = descriptor.getParams().stream()
                    .map(p -> {
                        Map<String, Object> paramMap = new HashMap<>();
                        paramMap.put("name", p.getName());
                        paramMap.put("type", p.getType());
                        paramMap.put("description", p.getDescription());
                        paramMap.put("required", p.getRequired());
                        paramMap.put("defaultValue", p.getDefaultValue());
                        paramMap.put("options", p.getOptions());
                        return paramMap;
                    })
                    .collect(Collectors.toList());
            configMap.put("params", paramsList);
        }
        
        ability.setConfig(configMap);
        
        return ability;
    }
}
