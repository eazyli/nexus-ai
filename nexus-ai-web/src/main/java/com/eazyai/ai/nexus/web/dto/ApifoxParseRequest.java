package com.eazyai.ai.nexus.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Apifox文档解析请求DTO
 */
@Data
@Schema(description = "Apifox文档解析请求")
public class ApifoxParseRequest {

    @NotBlank(message = "文档内容不能为空")
    @Schema(description = "Apifox导出的OpenAPI JSON文档内容", required = true)
    private String documentContent;

    @Schema(description = "基础URL，用于拼接接口路径", example = "http://api.example.com")
    private String baseUrl;

    @Schema(description = "只解析指定标签的接口")
    private List<String> includeTags;

    @Schema(description = "排除指定标签的接口")
    private List<String> excludeTags;

    @Schema(description = "默认请求头")
    private Map<String, String> defaultHeaders;
}
