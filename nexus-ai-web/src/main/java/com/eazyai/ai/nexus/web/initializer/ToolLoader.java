package com.eazyai.ai.nexus.web.initializer;

import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.McpToolRepository;
import com.eazyai.ai.nexus.api.tool.McpToolRepository.ToolEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具加载器
 * 启动时从数据库加载工具到内存
 * 
 * <p>依赖 api 层定义的 McpToolRepository 接口，不再直接依赖 infra 层</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolLoader {

    private final ToolBus toolBus;
    private final McpToolRepository toolRepository;

    @PostConstruct
    public void loadTools() {
        log.info("========== 开始从数据库加载工具 ==========");
        List<ToolEntity> tools = toolRepository.findAllEnabled();
        log.info("数据库中启用的工具数量: {}", tools.size());
        
        for (ToolEntity tool : tools) {
            ToolDescriptor descriptor = toDescriptor(tool);
            toolBus.registerTool(descriptor);
            log.info("已加载工具: {} (toolId={}, appId={}, type={})", 
                tool.toolName(), tool.toolId(), tool.appId(), tool.toolType());
        }
        
        log.info("========== 完成加载 {} 个工具 ==========", tools.size());
    }

    /**
     * ToolEntity 转 ToolDescriptor
     */
    private ToolDescriptor toDescriptor(ToolEntity entity) {
        if (entity == null) return null;
        return ToolDescriptor.builder()
                .toolId(entity.toolId())
                .appId(entity.appId())
                .name(entity.toolName())
                .description(entity.description())
                .executorType(entity.toolType().toLowerCase())
                .config(entity.config())
                .retryTimes(entity.retryTimes())
                .timeout(entity.timeout() != null ? entity.timeout().longValue() : null)
                .enabled(entity.status() != null && entity.status() == 1)
                .build();
    }
}
