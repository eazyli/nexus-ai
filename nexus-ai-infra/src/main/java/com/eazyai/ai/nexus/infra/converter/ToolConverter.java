package com.eazyai.ai.nexus.infra.converter;

import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolVisibility;
import com.eazyai.ai.nexus.api.tool.flow.FlowDefinition;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具转换器
 * 统一处理 AiMcpTool 实体和 ToolDescriptor 之间的转换
 */
@Component
public class ToolConverter {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 实体转描述符
     *
     * @param entity 数据库实体
     * @return 工具描述符
     */
    @SuppressWarnings("unchecked")
    public ToolDescriptor toDescriptor(AiMcpTool entity) {
        if (entity == null) {
            return null;
        }

        // 解析可见性
        ToolVisibility visibility = parseVisibility(entity.getVisibility());
        
        // 解析授权应用列表
        List<String> authorizedApps = parseAuthorizedApps(entity.getPermissionApps());
        
        // 解析工具类型
        ToolDescriptor.ToolType toolType = parseToolType(entity.getToolType());
        
        ToolDescriptor descriptor = ToolDescriptor.builder()
                .toolId(entity.getToolId())
                .appId(entity.getAppId())
                .visibility(visibility)
                .authorizedApps(authorizedApps)
                .name(entity.getToolName())
                .description(entity.getDescription())
                .executorType(entity.getToolType() != null ? entity.getToolType().toLowerCase() : "http")
                .toolType(toolType)
                .enabled(entity.getStatus() != null && entity.getStatus() == 1)
                .retryTimes(entity.getRetryTimes())
                .timeout(entity.getTimeout() != null ? entity.getTimeout().longValue() : null)
                .build();

        if (entity.getConfig() != null) {
            descriptor.setConfig(entity.getConfig());

            // 从配置中提取能力
            Object capabilitiesObj = entity.getConfig().get("capabilities");
            if (capabilitiesObj != null) {
                if (capabilitiesObj instanceof List<?> list) {
                    List<String> capabilities = list.stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                    descriptor.setCapabilities(capabilities);
                }
            }

            // 从配置中提取参数定义
            Object paramsObj = entity.getConfig().get("parameters");
            if (paramsObj != null) {
                List<ToolDescriptor.ParamDefinition> params = new ArrayList<>();

                if (paramsObj instanceof List<?> paramsList) {
                    for (Object item : paramsList) {
                        if (item instanceof ToolDescriptor.ParamDefinition paramDef) {
                            params.add(paramDef);
                        } else if (item instanceof Map<?, ?> map) {
                            params.add(ToolDescriptor.ParamDefinition.builder()
                                    .name((String) map.get("name"))
                                    .type((String) map.get("type"))
                                    .description((String) map.get("description"))
                                    .required((Boolean) map.get("required"))
                                    .defaultValue(map.get("defaultValue"))
                                    .options((List<String>) map.get("options"))
                                    .build());
                        }
                    }
                }
                descriptor.setParameters(params);
            }
            
            // 从配置中提取流程定义
            Object flowDefObj = entity.getConfig().get("flowDefinition");
            if (flowDefObj != null && toolType == ToolDescriptor.ToolType.FLOW) {
                try {
                    FlowDefinition flowDefinition = OBJECT_MAPPER.convertValue(
                            flowDefObj, FlowDefinition.class);
                    descriptor.setFlowDefinition(flowDefinition);
                } catch (Exception e) {
                    // 转换失败，忽略
                }
            }
        }

        return descriptor;
    }

    /**
     * 描述符转实体
     *
     * @param descriptor 工具描述符
     * @return 数据库实体
     */
    public AiMcpTool toEntity(ToolDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        AiMcpTool entity = new AiMcpTool();
        entity.setToolId(descriptor.getToolId());
        entity.setAppId(descriptor.getAppId());
        entity.setVisibility(descriptor.getVisibility() != null ? descriptor.getVisibility().name() : null);
        entity.setPermissionApps(descriptor.getAuthorizedApps() != null ? 
                String.join(",", descriptor.getAuthorizedApps()) : null);
        entity.setToolName(descriptor.getName());
        entity.setDescription(descriptor.getDescription());
        
        // 根据工具类型设置 executorType
        String executorType = descriptor.getExecutorType();
        if (descriptor.getToolType() == ToolDescriptor.ToolType.FLOW) {
            executorType = "flow";
            entity.setToolType("FLOW");
        } else if (executorType != null) {
            entity.setToolType(executorType.toUpperCase());
        } else {
            entity.setToolType("HTTP");
        }
        
        entity.setStatus(Boolean.TRUE.equals(descriptor.getEnabled()) ? 1 : 0);
        entity.setRetryTimes(descriptor.getRetryTimes());
        entity.setTimeout(descriptor.getTimeout() != null ? descriptor.getTimeout().intValue() : null);

        // 转换配置
        Map<String, Object> config = new HashMap<>();
        if (descriptor.getConfig() != null) {
            config.putAll(descriptor.getConfig());
        }
        if (descriptor.getCapabilities() != null) {
            config.put("capabilities", descriptor.getCapabilities());
        }
        if (descriptor.getParameters() != null) {
            config.put("parameters", descriptor.getParameters());
        }
        // 保存流程定义
        if (descriptor.getFlowDefinition() != null) {
            config.put("flowDefinition", descriptor.getFlowDefinition());
        }
        entity.setConfig(config);

        return entity;
    }

    /**
     * 批量转换实体为描述符
     *
     * @param entities 实体列表
     * @return 描述符列表
     */
    public List<ToolDescriptor> toDescriptorList(List<AiMcpTool> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 解析可见性
     */
    private ToolVisibility parseVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return null;
        }
        try {
            return ToolVisibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 解析授权应用列表
     */
    private List<String> parseAuthorizedApps(String permissionApps) {
        if (permissionApps == null || permissionApps.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(permissionApps.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * 解析工具类型
     */
    private ToolDescriptor.ToolType parseToolType(String toolType) {
        if (toolType == null || toolType.isBlank()) {
            return ToolDescriptor.ToolType.ATOMIC;
        }
        if ("FLOW".equalsIgnoreCase(toolType)) {
            return ToolDescriptor.ToolType.FLOW;
        }
        return ToolDescriptor.ToolType.ATOMIC;
    }
}
