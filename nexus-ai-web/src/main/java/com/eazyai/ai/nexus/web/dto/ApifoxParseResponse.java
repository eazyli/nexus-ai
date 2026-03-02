package com.eazyai.ai.nexus.web.dto;

import com.eazyai.ai.nexus.core.mcp.McpToolDescriptor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Apifox文档解析响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Apifox文档解析响应")
public class ApifoxParseResponse {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "解析出的接口数量")
    private Integer totalEndpoints;

    @Schema(description = "解析出的工具定义列表")
    private List<ParsedTool> tools;

    @Schema(description = "解析警告信息")
    private List<String> warnings;

    @Schema(description = "错误信息")
    private String error;

    /**
     * 解析出的工具
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "解析出的工具定义")
    public static class ParsedTool {
        @Schema(description = "工具名称")
        private String name;

        @Schema(description = "工具描述")
        private String description;

        @Schema(description = "请求URL")
        private String url;

        @Schema(description = "请求方法")
        private String method;

        @Schema(description = "请求头")
        private Map<String, String> headers;

        @Schema(description = "参数定义")
        private List<McpToolDescriptor.ParamDefinition> parameters;

        @Schema(description = "响应数据提取路径")
        private String responsePath;

        @Schema(description = "能力标签")
        private List<String> capabilities;

        @Schema(description = "原始路径")
        private String originalPath;

        @Schema(description = "原始操作ID")
        private String originalOperationId;

        @Schema(description = "所属标签")
        private List<String> tags;
    }
}
