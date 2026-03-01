package com.eazyai.ai.nexus.core.integrator;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.dto.AgentResponse;
import com.eazyai.ai.nexus.api.integrator.ResultIntegrator;
import com.eazyai.ai.nexus.api.scheduler.ScheduleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 默认结果整合器
 * 整合各步骤执行结果，生成最终响应
 */
@Slf4j
@Component
public class DefaultResultIntegrator implements ResultIntegrator {

    @Override
    public AgentResponse integrate(ScheduleResult scheduleResult, AgentContext context) {
        AgentResponse.AgentResponseBuilder responseBuilder = AgentResponse.builder();

        // 设置执行步骤详情
        List<AgentResponse.ExecutionStep> steps = scheduleResult.getStepResults().stream()
                .map(this::convertStep)
                .collect(Collectors.toList());
        responseBuilder.steps(steps);

        // 判断是否成功
        boolean success = scheduleResult.isSuccess();
        responseBuilder.success(success);

        if (!success) {
            // 构建错误信息
            String errorMsg = scheduleResult.getErrorMessage();
            if (errorMsg == null) {
                errorMsg = steps.stream()
                        .filter(s -> !s.isSuccess())
                        .map(AgentResponse.ExecutionStep::getErrorMessage)
                        .collect(Collectors.joining("; "));
            }
            responseBuilder.errorMessage(errorMsg);
            return responseBuilder.build();
        }

        // 构建输出结果
        String output = buildOutput(scheduleResult);
        responseBuilder.output(output);

        // 构建结构化输出
        Map<String, Object> structuredOutput = buildStructuredOutput(scheduleResult);
        responseBuilder.structuredOutput(structuredOutput);

        // 设置执行时间
        responseBuilder.executionTime(scheduleResult.getExecutionTime());

        // 收集使用的插件
        List<String> usedPlugins = scheduleResult.getStepResults().stream()
                .map(ScheduleResult.StepResult::getPluginName)
                .distinct()
                .collect(Collectors.toList());
        responseBuilder.usedPlugins(usedPlugins);

        return responseBuilder.build();
    }

    @Override
    public Object formatOutput(Object data, String format) {
        if (format == null || data == null) {
            return data;
        }

        return switch (format.toLowerCase()) {
            case "json" -> com.alibaba.fastjson.JSON.toJSONString(data);
            case "markdown" -> convertToMarkdown(data);
            case "html" -> convertToHtml(data);
            default -> data;
        };
    }

    /**
     * 转换步骤结果
     */
    private AgentResponse.ExecutionStep convertStep(ScheduleResult.StepResult stepResult) {
        return AgentResponse.ExecutionStep.builder()
                .stepNumber(0) // 稍后设置
                .stage(stepResult.getStepId())
                .description("Execute plugin: " + stepResult.getPluginName())
                .output(stepResult.getOutput())
                .startTime(stepResult.getStartTime())
                .endTime(stepResult.getEndTime())
                .success(stepResult.isSuccess())
                .errorMessage(stepResult.getErrorMessage())
                .build();
    }

    /**
     * 构建输出文本
     */
    private String buildOutput(ScheduleResult scheduleResult) {
        StringBuilder output = new StringBuilder();

        for (ScheduleResult.StepResult step : scheduleResult.getStepResults()) {
            if (step.getOutput() != null) {
                if (output.length() > 0) {
                    output.append("\n\n");
                }
                output.append(step.getOutput());
            }
        }

        return output.length() > 0 ? output.toString() : "Execution completed with no output";
    }

    /**
     * 构建结构化输出
     */
    private Map<String, Object> buildStructuredOutput(ScheduleResult scheduleResult) {
        Map<String, Object> result = new HashMap<>();

        for (int i = 0; i < scheduleResult.getStepResults().size(); i++) {
            ScheduleResult.StepResult step = scheduleResult.getStepResults().get(i);
            result.put(step.getStepId(), step.getOutput());
        }

        return result;
    }

    /**
     * 转换为Markdown格式
     */
    private String convertToMarkdown(Object data) {
        if (data instanceof Map) {
            StringBuilder md = new StringBuilder();
            ((Map<?, ?>) data).forEach((k, v) -> {
                md.append("## ").append(k).append("\n\n");
                md.append(v).append("\n\n");
            });
            return md.toString();
        }
        return data.toString();
    }

    /**
     * 转换为HTML格式
     */
    private String convertToHtml(Object data) {
        if (data instanceof Map) {
            StringBuilder html = new StringBuilder("<div>");
            ((Map<?, ?>) data).forEach((k, v) -> {
                html.append("<h3>").append(k).append("</h3>");
                html.append("<p>").append(v).append("</p>");
            });
            html.append("</div>");
            return html.toString();
        }
        return "<p>" + data + "</p>";
    }
}
