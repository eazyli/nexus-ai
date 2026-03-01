package com.eazyai.ai.nexus.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能体执行请求DTO
 * 用户交互层请求对象
 */
@Data
@Schema(description = "智能体执行请求")
public class AgentExecuteRequest {

    @NotBlank(message = "查询内容不能为空")
    @Schema(description = "用户查询/问题", required = true, example = "今天北京的天气怎么样？")
    private String query;

    @Schema(description = "应用ID，用于关联工具和场景", example = "wenhuagong")
    private String appId;

    @Schema(description = "任务类型", example = "qa")
    private String taskType;

    @Schema(description = "会话ID，用于上下文保持")
    private String sessionId;

    @Schema(description = "最大迭代次数", example = "10")
    private Integer maxIterations;

    @Schema(description = "超时时间（毫秒）", example = "60000")
    private Long timeout;

    @Schema(description = "期望输出格式（json/markdown/html）", example = "markdown")
    private String outputFormat;

    @Schema(description = "扩展参数")
    private Map<String, Object> params = new HashMap<>();
}
