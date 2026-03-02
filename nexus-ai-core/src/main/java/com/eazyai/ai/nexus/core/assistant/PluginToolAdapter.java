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
        String description = buildEnhancedDescription();

        return ToolSpecification.builder()
                .name(toolName)
                .description(description)
                .build();
    }

    /**
     * 构建增强的工具描述
     * 包含基础描述、触发条件、使用指导、示例等信息
     */
    private String buildEnhancedDescription() {
        StringBuilder sb = new StringBuilder();

        // 1. 基础描述
        sb.append(descriptor.getDescription() != null
                ? descriptor.getDescription()
                : "执行 " + descriptor.getName() + " 插件");

        // 2. 触发条件
        if (descriptor.getTriggerConditions() != null && !descriptor.getTriggerConditions().isEmpty()) {
            sb.append("\n\n【何时使用】\n").append(descriptor.getTriggerConditions());
        }

        // 3. 参数说明
        if (descriptor.getParameters() != null && !descriptor.getParameters().isEmpty()) {
            sb.append("\n\n【参数说明】");
            for (PluginDescriptor.ParameterDef param : descriptor.getParameters()) {
                sb.append("\n- ").append(param.getName())
                        .append(" (").append(param.getType()).append(")")
                        .append(param.isRequired() ? " [必填]" : " [可选]");
                if (param.getDescription() != null) {
                    sb.append(": ").append(param.getDescription());
                }
                if (param.getDefaultValue() != null) {
                    sb.append("，默认值: ").append(param.getDefaultValue());
                }
                if (param.getExample() != null) {
                    sb.append("，示例: ").append(param.getExample());
                }
            }
        }

        // 4. 使用指导
        if (descriptor.getGuidance() != null && !descriptor.getGuidance().isEmpty()) {
            sb.append("\n\n【使用指导】\n").append(descriptor.getGuidance());
        }

        // 5. 使用示例
        if (descriptor.getExamples() != null && !descriptor.getExamples().isEmpty()) {
            sb.append("\n\n【使用示例】");
            int exampleIndex = 1;
            for (PluginDescriptor.UsageExample example : descriptor.getExamples()) {
                sb.append("\n\n示例").append(exampleIndex++).append(": ").append(example.getScenario());
                if (example.getUserInput() != null) {
                    sb.append("\n用户输入: \"").append(example.getUserInput()).append("\"");
                }
                if (example.getToolArguments() != null) {
                    sb.append("\n调用参数: ").append(example.getToolArguments());
                }
                if (example.getExpectedOutput() != null) {
                    sb.append("\n预期输出: ").append(example.getExpectedOutput());
                }
                if (example.getNotes() != null) {
                    sb.append("\n备注: ").append(example.getNotes());
                }
            }
        }

        // 6. 工具协作提示
        if ((descriptor.getPreRequisiteTools() != null && !descriptor.getPreRequisiteTools().isEmpty())
                || (descriptor.getFollowUpTools() != null && !descriptor.getFollowUpTools().isEmpty())) {
            sb.append("\n\n【工具协作】");
            if (!descriptor.getPreRequisiteTools().isEmpty()) {
                sb.append("\n前置工具: ").append(String.join(", ", descriptor.getPreRequisiteTools()));
            }
            if (!descriptor.getFollowUpTools().isEmpty()) {
                sb.append("\n后续工具: ").append(String.join(", ", descriptor.getFollowUpTools()));
            }
        }

        // 7. 错误处理
        if (descriptor.getErrorHandling() != null && !descriptor.getErrorHandling().isEmpty()) {
            sb.append("\n\n【错误处理】\n").append(descriptor.getErrorHandling());
        }

        // 8. 执行特性
        sb.append("\n\n【执行特性】");
        sb.append("\n- 幂等性: ").append(descriptor.isIdempotent() ? "是（可重复调用）" : "否");
        if (descriptor.getEstimatedDuration() != null) {
            sb.append("\n- 预计耗时: ").append(descriptor.getEstimatedDuration()).append("ms");
        }
        sb.append("\n- 优先级: ").append(descriptor.getPriority());

        return sb.toString();
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
