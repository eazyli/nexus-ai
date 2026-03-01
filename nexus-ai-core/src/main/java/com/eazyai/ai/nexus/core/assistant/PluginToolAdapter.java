package com.eazyai.ai.nexus.core.assistant;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.executor.ExecutionResult;
import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件工具适配器
 * 将 Plugin 接口适配为 LangChain4j @Tool
 * 
 * <p>功能：</p>
 * <ul>
 *   <li>将 Plugin 包装为可被 AiServices 识别的工具</li>
 *   <li>自动生成工具规范（ToolSpecification）</li>
 *   <li>提供统一的执行入口</li>
 * </ul>
 */
@Slf4j
public class PluginToolAdapter {

    private final Plugin plugin;
    private final PluginDescriptor descriptor;

    public PluginToolAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.descriptor = plugin.getDescriptor();
    }

    /**
     * 执行插件
     * 动态生成的工具名称基于插件ID
     */
    @Tool(value = "执行插件操作")
    public String execute(String input) {
        log.info("执行插件: {} - 输入: {}", descriptor.getId(), input);
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("input", input);
            params.put("query", input);
            
            AgentContext context = AgentContext.builder()
                .userInput(input)
                .build();
            
            ExecutionResult result = plugin.execute(params, context);
            
            if (result.isSuccess()) {
                Object data = result.getData();
                return data != null ? data.toString() : "执行成功";
            } else {
                return "执行失败: " + result.getErrorMessage();
            }
        } catch (Exception e) {
            log.error("插件执行失败: {}", descriptor.getId(), e);
            return "执行异常: " + e.getMessage();
        }
    }

    /**
     * 获取工具规范
     * 用于向 LLM 描述工具的能力
     */
    public ToolSpecification toToolSpecification() {
        String toolName = sanitizeToolName(descriptor.getId());
        String description = descriptor.getDescription() != null 
                ? descriptor.getDescription() 
                : "执行 " + descriptor.getName() + " 插件";
        
        return ToolSpecification.builder()
                .name(toolName)
                .description(description)
                .build();
    }

    /**
     * 获取插件描述
     */
    public String getDescription() {
        return descriptor.getDescription();
    }

    /**
     * 获取插件ID
     */
    public String getPluginId() {
        return descriptor.getId();
    }

    /**
     * 获取原始插件
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * 清理工具名称（移除特殊字符）
     */
    private String sanitizeToolName(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
