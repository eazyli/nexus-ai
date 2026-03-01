package com.eazyai.ai.nexus.infra.converter;

import com.eazyai.ai.nexus.core.mcp.McpToolDescriptor;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP工具转换器
 * 统一处理 AiMcpTool 实体和 McpToolDescriptor 之间的转换
 */
@Component
public class McpToolConverter {

    /**
     * 实体转描述符
     *
     * @param entity 数据库实体
     * @return 工具描述符
     */
    @SuppressWarnings("unchecked")
    public McpToolDescriptor toDescriptor(AiMcpTool entity) {
        if (entity == null) {
            return null;
        }

        McpToolDescriptor descriptor = McpToolDescriptor.builder()
                .toolId(entity.getToolId())
                .appId(entity.getAppId())
                .name(entity.getToolName())
                .description(entity.getDescription())
                .type(entity.getToolType() != null ? entity.getToolType().toLowerCase() : "http")
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
                List<McpToolDescriptor.ParamDefinition> params = new ArrayList<>();

                if (paramsObj instanceof List<?> paramsList) {
                    for (Object item : paramsList) {
                        if (item instanceof McpToolDescriptor.ParamDefinition paramDef) {
                            params.add(paramDef);
                        } else if (item instanceof Map<?, ?> map) {
                            params.add(McpToolDescriptor.ParamDefinition.builder()
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
        }

        return descriptor;
    }

    /**
     * 描述符转实体
     *
     * @param descriptor 工具描述符
     * @return 数据库实体
     */
    public AiMcpTool toEntity(McpToolDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        AiMcpTool entity = new AiMcpTool();
        entity.setToolId(descriptor.getToolId());
        entity.setAppId(descriptor.getAppId());
        entity.setToolName(descriptor.getName());
        entity.setDescription(descriptor.getDescription());
        entity.setToolType(descriptor.getType() != null ? descriptor.getType().toUpperCase() : "HTTP");
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
        entity.setConfig(config);

        return entity;
    }

    /**
     * 批量转换实体为描述符
     *
     * @param entities 实体列表
     * @return 描述符列表
     */
    public List<McpToolDescriptor> toDescriptorList(List<AiMcpTool> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }
}
