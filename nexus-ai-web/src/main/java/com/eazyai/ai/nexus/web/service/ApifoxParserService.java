package com.eazyai.ai.nexus.web.service;

import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.web.dto.ApifoxParseRequest;
import com.eazyai.ai.nexus.web.dto.ApifoxParseResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Apifox/OpenAPI文档解析服务
 * 支持解析OpenAPI 3.0格式的文档，将API端点转换为工具定义
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApifoxParserService {

    private final ObjectMapper objectMapper;

    /**
     * 解析Apifox导出的OpenAPI文档
     */
    public ApifoxParseResponse parse(ApifoxParseRequest request) {
        log.info("开始解析Apifox文档, baseUrl: {}", request.getBaseUrl());

        try {
            JsonNode doc = objectMapper.readTree(request.getDocumentContent());

            // 支持OpenAPI 3.0格式
            JsonNode paths = doc.path("paths");
            if (paths.isMissingNode()) {
                return ApifoxParseResponse.builder()
                        .success(false)
                        .error("文档格式错误: 缺少paths节点")
                        .build();
            }

            List<ApifoxParseResponse.ParsedTool> tools = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            String baseUrl = StringUtils.hasText(request.getBaseUrl()) ?
                    request.getBaseUrl().replaceAll("/$", "") : "";

            // 遍历所有路径
            Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.fields();
            while (pathIterator.hasNext()) {
                Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
                String path = pathEntry.getKey();
                JsonNode methods = pathEntry.getValue();

                // 遍历每个HTTP方法
                for (String httpMethod : Arrays.asList("get", "post", "put", "delete", "patch")) {
                    JsonNode operation = methods.path(httpMethod);
                    if (!operation.isMissingNode()) {
                        try {
                            ApifoxParseResponse.ParsedTool tool = parseOperation(
                                    path, httpMethod.toUpperCase(), operation,
                                    baseUrl, request);

                            // 过滤标签
                            if (shouldInclude(tool, request)) {
                                tools.add(tool);
                            }
                        } catch (Exception e) {
                            warnings.add(String.format("解析接口失败 %s %s: %s",
                                    httpMethod.toUpperCase(), path, e.getMessage()));
                        }
                    }
                }
            }

            log.info("文档解析完成: 解析出{}个工具", tools.size());

            return ApifoxParseResponse.builder()
                    .success(true)
                    .totalEndpoints(tools.size())
                    .tools(tools)
                    .warnings(warnings)
                    .build();

        } catch (Exception e) {
            log.error("解析文档失败", e);
            return ApifoxParseResponse.builder()
                    .success(false)
                    .error("解析文档失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析单个操作
     */
    private ApifoxParseResponse.ParsedTool parseOperation(
            String path, String method, JsonNode operation,
            String baseUrl, ApifoxParseRequest request) {

        String operationId = getText(operation, "operationId");
        String summary = getText(operation, "summary");
        String description = getText(operation, "description");

        // 生成工具名称
        String toolName = generateToolName(operationId, method, path);

        // 工具描述
        String toolDescription = StringUtils.hasText(summary) ? summary :
                (StringUtils.hasText(description) ? description :
                        String.format("%s %s", method, path));

        // 解析参数
        List<ToolDescriptor.ParamDefinition> parameters = parseParameters(operation);

        // 解析标签
        List<String> tags = parseTags(operation);

        // 构建完整URL
        String url = baseUrl + path;

        // 合并默认请求头
        Map<String, String> headers = new HashMap<>();
        if (request.getDefaultHeaders() != null) {
            headers.putAll(request.getDefaultHeaders());
        }
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        return ApifoxParseResponse.ParsedTool.builder()
                .name(toolName)
                .description(toolDescription)
                .url(url)
                .method(method)
                .headers(headers)
                .parameters(parameters)
                .responsePath("$.data")
                .capabilities(generateCapabilities(method, tags))
                .originalPath(path)
                .originalOperationId(operationId)
                .tags(tags)
                .build();
    }

    /**
     * 解析参数定义
     */
    private List<ToolDescriptor.ParamDefinition> parseParameters(JsonNode operation) {
        List<ToolDescriptor.ParamDefinition> parameters = new ArrayList<>();
        JsonNode paramsNode = operation.path("parameters");
        JsonNode requestBody = operation.path("requestBody");

        // 解析路径参数和查询参数
        if (paramsNode.isArray()) {
            for (JsonNode param : paramsNode) {
                String in = getText(param, "in");
                String name = getText(param, "name");
                String desc = getText(param, "description");
                boolean required = param.path("required").asBoolean(false);
                String type = getParamType(param.path("schema"));

                parameters.add(ToolDescriptor.ParamDefinition.builder()
                        .name(name)
                        .type(type)
                        .description(StringUtils.hasText(desc) ? desc :
                                String.format("%s参数: %s", in, name))
                        .required(required)
                        .build());
            }
        }

        // 解析请求体参数
        if (!requestBody.isMissingNode()) {
            JsonNode content = requestBody.path("content").path("application/json");
            if (!content.isMissingNode()) {
                JsonNode schema = content.path("schema");
                parseSchemaProperties(schema, parameters);
            }
        }

        return parameters;
    }

    /**
     * 解析Schema属性
     */
    private void parseSchemaProperties(JsonNode schema,
                                       List<ToolDescriptor.ParamDefinition> parameters) {
        JsonNode properties = schema.path("properties");
        if (properties.isMissingNode()) {
            return;
        }

        JsonNode required = schema.path("required");
        Set<String> requiredFields = new HashSet<>();
        if (required.isArray()) {
            for (JsonNode req : required) {
                requiredFields.add(req.asText());
            }
        }

        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            JsonNode fieldSchema = field.getValue();
            String type = getParamType(fieldSchema);
            String desc = getText(fieldSchema, "description");

            parameters.add(ToolDescriptor.ParamDefinition.builder()
                    .name(name)
                    .type(type)
                    .description(StringUtils.hasText(desc) ? desc :
                            String.format("%s参数", name))
                    .required(requiredFields.contains(name))
                    .build());
        }
    }

    /**
     * 解析标签
     */
    private List<String> parseTags(JsonNode operation) {
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = operation.path("tags");
        if (tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                tags.add(tag.asText());
            }
        }
        return tags;
    }

    /**
     * 生成工具名称
     */
    private String generateToolName(String operationId, String method, String path) {
        if (StringUtils.hasText(operationId)) {
            return operationId.replaceAll("[^a-zA-Z0-9_]", "_");
        }
        // 从路径生成名称
        String name = path.replaceAll("[{}]", "")
                .replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
        return method.toLowerCase() + "_" + name;
    }

    /**
     * 生成能力标签
     */
    private List<String> generateCapabilities(String method, List<String> tags) {
        List<String> capabilities = new ArrayList<>();

        // 根据HTTP方法推断能力
        switch (method.toUpperCase()) {
            case "GET":
                capabilities.add("query");
                capabilities.add("read");
                break;
            case "POST":
                capabilities.add("create");
                capabilities.add("write");
                break;
            case "PUT":
            case "PATCH":
                capabilities.add("update");
                capabilities.add("write");
                break;
            case "DELETE":
                capabilities.add("delete");
                capabilities.add("write");
                break;
        }

        // 添加API标签作为能力
        if (tags != null) {
            capabilities.addAll(tags);
        }

        return capabilities;
    }

    /**
     * 判断是否应该包含该工具
     */
    private boolean shouldInclude(ApifoxParseResponse.ParsedTool tool, ApifoxParseRequest request) {
        List<String> includeTags = request.getIncludeTags();
        List<String> excludeTags = request.getExcludeTags();

        // 如果有包含标签，必须匹配
        if (includeTags != null && !includeTags.isEmpty()) {
            boolean hasIncludeTag = tool.getTags().stream()
                    .anyMatch(includeTags::contains);
            if (!hasIncludeTag) {
                return false;
            }
        }

        // 如果有排除标签，不能匹配
        if (excludeTags != null && !excludeTags.isEmpty()) {
            boolean hasExcludeTag = tool.getTags().stream()
                    .anyMatch(excludeTags::contains);
            if (hasExcludeTag) {
                return false;
            }
        }

        return true;
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() ? null : value.asText();
    }

    private String getParamType(JsonNode schema) {
        if (schema.isMissingNode()) {
            return "string";
        }
        String type = getText(schema, "type");
        if (!StringUtils.hasText(type)) {
            return "string";
        }
        switch (type) {
            case "integer":
                return "integer";
            case "number":
                return "number";
            case "boolean":
                return "boolean";
            case "array":
                return "array";
            case "object":
                return "object";
            default:
                return "string";
        }
    }
}
