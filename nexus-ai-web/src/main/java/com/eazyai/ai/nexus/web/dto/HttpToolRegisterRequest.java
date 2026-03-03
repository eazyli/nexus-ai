package com.eazyai.ai.nexus.web.dto;

import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * HTTP工具注册请求DTO
 */
@Data
@Schema(description = "HTTP工具注册请求")
public class HttpToolRegisterRequest {

    @Schema(description = "所属应用ID", example = "wenhuagong")
    private String appId;

    @Schema(description = "工具ID（可选，不填则自动生成）")
    private String toolId;

    @NotBlank(message = "工具名称不能为空")
    @Schema(description = "工具名称", required = true, example = "reportStatistics")
    private String name;

    @NotBlank(message = "工具描述不能为空")
    @Schema(description = "工具描述", required = true, example = "查询业务数据报表统计")
    private String description;

    @Schema(description = "工具能力标签", example = "[\"report\", \"statistics\", \"query\"]")
    private List<String> capabilities;

    @Schema(description = "参数定义")
    private List<ToolDescriptor.ParamDefinition> parameters;

    @NotBlank(message = "请求URL不能为空")
    @Schema(description = "请求URL", required = true, example = "http://business-system/api/report/statistics")
    private String url;

    @Schema(description = "请求方法", example = "GET", allowableValues = {"GET", "POST", "PUT", "DELETE"})
    private String method;

    @Schema(description = "请求头", example = "{\"Content-Type\": \"application/json\"}")
    private Map<String, String> headers;

    @Schema(description = "认证类型", example = "none", allowableValues = {"none", "basic", "bearer", "api_key"})
    private String authType;

    @Schema(description = "认证配置")
    private Map<String, Object> authConfig;

    @Schema(description = "响应数据提取路径（JSONPath）", example = "$.data")
    private String responsePath;

    @Schema(description = "重试次数", example = "3")
    private Integer retryTimes;

    @Schema(description = "超时时间（毫秒）", example = "30000")
    private Long timeout;
}
