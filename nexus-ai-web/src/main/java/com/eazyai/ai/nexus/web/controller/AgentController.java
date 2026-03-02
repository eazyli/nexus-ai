package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.api.dto.AgentRequest;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
import com.eazyai.ai.nexus.api.react.ThoughtEvent;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import com.eazyai.ai.nexus.core.engine.ReActEngine;
import com.eazyai.ai.nexus.web.dto.AgentExecuteRequest;
import com.eazyai.ai.nexus.web.dto.AgentExecuteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 智能体API控制器
 * 用户交互层 - 提供REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Tag(name = "智能体接口", description = "AI Agent执行和管理接口")
public class AgentController {

    @Autowired
    private ReActEngine reActEngine;

    @Autowired
    private PluginRegistry pluginRegistry;

    /**
     * 执行智能体任务
     */
    @PostMapping("/execute")
    @Operation(summary = "执行智能体任务", description = "执行智能体任务，自动完成意图理解、规划、调度、执行和结果整合")
    public AgentExecuteResponse execute(@Valid @RequestBody AgentExecuteRequest request) {
        log.info("收到执行请求: {}", request.getQuery());

        AgentRequest agentRequest = AgentRequest.builder()
                .query(request.getQuery())
                .appId(request.getAppId())
                .taskType(request.getTaskType())
                .sessionId(request.getSessionId())
                .maxIterations(request.getMaxIterations() != null ? request.getMaxIterations() : 10)
                .timeout(request.getTimeout() != null ? request.getTimeout() : 60000)
                .params(request.getParams())
                .outputFormat(request.getOutputFormat())
                .build();

        // 控制台打印 ReAct 步骤
        Consumer<ThoughtEvent> thinkingLogger = createThinkingLogger();
        AgentResponse response = reActEngine.execute(agentRequest, thinkingLogger);

        return AgentExecuteResponse.builder()
                .sessionId(response.getSessionId())
                .success(response.isSuccess())
                .output(response.getOutput())
                .structuredOutput(response.getStructuredOutput())
                .errorMessage(response.getErrorMessage())
                .steps(response.getSteps())
                .executionTime(response.getExecutionTime())
                .usedPlugins(response.getUsedPlugins())
                .metadata(response.getMetadata())
                .build();
    }

    /**
     * 获取所有可用插件
     */
    @GetMapping("/plugins")
    @Operation(summary = "获取插件列表", description = "获取所有已注册的插件")
    public List<PluginDescriptor> listPlugins() {
        return pluginRegistry.getAllPlugins();
    }

    /**
     * 按类型获取插件
     */
    @GetMapping("/plugins/type/{type}")
    @Operation(summary = "按类型获取插件", description = "根据类型筛选插件")
    public List<PluginInfo> getPluginsByType(@PathVariable("type") String type) {
        return pluginRegistry.findByType(type).stream()
                .map(p -> new PluginInfo(p.getDescriptor()))
                .collect(Collectors.toList());
    }

    /**
     * 按能力获取插件
     */
    @GetMapping("/plugins/capability/{capability}")
    @Operation(summary = "按能力获取插件", description = "根据能力筛选插件")
    public List<PluginInfo> getPluginsByCapability(@PathVariable("capability") String capability) {
        return pluginRegistry.findByCapability(capability).stream()
                .map(p -> new PluginInfo(p.getDescriptor()))
                .collect(Collectors.toList());
    }

    /**
     * 插件信息DTO
     */
    @lombok.Data
    public static class PluginInfo {
        private String id;
        private String name;
        private String version;
        private String type;
        private String description;
        private List<String> capabilities;
        private boolean enabled;

        public PluginInfo(PluginDescriptor descriptor) {
            this.id = descriptor.getId();
            this.name = descriptor.getName();
            this.version = descriptor.getVersion();
            this.type = descriptor.getType();
            this.description = descriptor.getDescription();
            this.capabilities = descriptor.getCapabilities();
            this.enabled = descriptor.isEnabled();
        }
    }

    /**
     * 创建控制台打印 ReAct 步骤的回调
     */
    private Consumer<ThoughtEvent> createThinkingLogger() {
        return event -> {
            StringBuilder sb = new StringBuilder();
            switch (event.getType()) {
                case THINKING_START:
                    sb.append("🚀 开始思考: ").append(event.getContent());
                    break;
                case THOUGHT:
                    sb.append("💭 思考: ").append(event.getContent());
                    break;
                case TOOL_SELECTED:
                    sb.append("🔧 选择工具: ").append(event.getToolName())
                      .append(" | 输入: ").append(event.getToolInput());
                    break;
                case TOOL_EXECUTING:
                    sb.append("⚙️ 执行工具: ").append(event.getToolName());
                    break;
                case TOOL_RESULT:
                    sb.append(event.isSuccess() ? "✅" : "❌")
                      .append(" 工具结果 [").append(event.getToolName()).append("]: ")
                      .append(event.getToolOutput());
                    if (!event.isSuccess() && event.getErrorMessage() != null) {
                        sb.append(" | 错误: ").append(event.getErrorMessage());
                    }
                    break;
                case REFLECTION_START:
                    sb.append("🔍 开始反思...");
                    break;
                case REFLECTION:
                    sb.append("💡 反思: ").append(event.getContent());
                    break;
                case FINAL_ANSWER:
                    sb.append("🎯 最终答案: ").append(event.getContent());
                    break;
                case ERROR:
                    sb.append("❌ 错误: ").append(event.getErrorMessage());
                    break;
                default:
                    sb.append("📝 ").append(event.getType()).append(": ").append(event.getContent());
            }
            log.info("[ReAct] {}", sb);
        };
    }
}
