package com.eazyai.ai.nexus.web.initializer;

import com.eazyai.ai.nexus.infra.converter.ToolConverter;
import com.eazyai.ai.nexus.api.tool.ToolBus;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.infra.dal.entity.AiMcpTool;
import com.eazyai.ai.nexus.infra.dal.repository.AiMcpToolRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具加载器
 * 启动时从数据库加载工具到内存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolLoader {

    private final ToolBus toolBus;
    private final AiMcpToolRepository aiMcpToolRepository;
    private final ToolConverter toolConverter;

    @PostConstruct
    public void loadTools() {
        log.info("========== 开始从数据库加载工具 ==========");
        List<AiMcpTool> tools = aiMcpToolRepository.findAllEnabled();
        log.info("数据库中启用的工具数量: {}", tools.size());
        
        for (AiMcpTool tool : tools) {
            ToolDescriptor descriptor = toolConverter.toDescriptor(tool);
            toolBus.registerTool(descriptor);
            log.info("已加载工具: {} (toolId={}, appId={}, type={})", 
                tool.getToolName(), tool.getToolId(), tool.getAppId(), tool.getToolType());
        }
        
        log.info("========== 完成加载 {} 个工具 ==========", tools.size());
    }
}
