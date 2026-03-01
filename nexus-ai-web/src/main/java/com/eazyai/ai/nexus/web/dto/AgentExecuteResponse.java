package com.eazyai.ai.nexus.web.dto;

import com.eazyai.ai.nexus.api.dto.AgentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 智能体执行响应DTO
 * 用户交互层响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能体执行响应")
public class AgentExecuteResponse {

    @Schema(description = "是否执行成功")
    private boolean success;

    @Schema(description = "输出结果")
    private String output;

    @Schema(description = "结构化输出")
    private Object structuredOutput;

    @Schema(description = "执行步骤详情")
    private List<AgentResponse.ExecutionStep> steps;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "执行耗时（毫秒）")
    private long executionTime;

    @Schema(description = "使用的插件列表")
    private List<String> usedPlugins;

    @Schema(description = "扩展元数据")
    private Map<String, Object> metadata;
}
