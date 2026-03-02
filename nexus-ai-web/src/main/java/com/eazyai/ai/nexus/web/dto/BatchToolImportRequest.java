package com.eazyai.ai.nexus.web.dto;

import com.eazyai.ai.nexus.core.mcp.McpToolDescriptor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 批量导入工具请求DTO
 */
@Data
@Schema(description = "批量导入工具请求")
public class BatchToolImportRequest {

    @NotBlank(message = "目标应用ID不能为空")
    @Schema(description = "目标应用ID", required = true, example = "wenhuagong")
    private String targetAppId;

    @Schema(description = "工具定义列表")
    private List<ToolDefinition> tools;

    @Schema(description = "是否覆盖已存在的工具", example = "false")
    private Boolean overwrite;

    /**
     * 工具定义
     */
    @Data
    @Schema(description = "工具定义")
    public static class ToolDefinition {
        @Schema(description = "工具名称", example = "getUserInfo")
        private String name;

        @Schema(description = "工具描述", example = "获取用户信息")
        private String description;

        @Schema(description = "请求URL", example = "http://api.example.com/user/info")
        private String url;

        @Schema(description = "请求方法", example = "GET", allowableValues = {"GET", "POST", "PUT", "DELETE"})
        private String method;

        @Schema(description = "请求头")
        private Map<String, String> headers;

        @Schema(description = "参数定义")
        private List<McpToolDescriptor.ParamDefinition> parameters;

        @Schema(description = "响应数据提取路径（JSONPath）", example = "$.data")
        private String responsePath;

        @Schema(description = "能力标签")
        private List<String> capabilities;
    }
}
