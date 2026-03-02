package com.eazyai.ai.nexus.core.mcp.executor;

import com.eazyai.ai.nexus.core.config.NexusProperties;
import com.eazyai.ai.nexus.core.mcp.McpToolDescriptor;
import com.eazyai.ai.nexus.core.mcp.McpToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP工具执行器
 * 执行HTTP请求类型的工具
 * 
 * <p>特性：</p>
 * <ul>
 *   <li>连接池复用，提升性能</li>
 *   <li>可配置的超时时间</li>
 *   <li>重试机制，增强可靠性</li>
 *   <li>熔断保护，防止级联故障</li>
 * </ul>
 */
@Slf4j
@Component
public class HttpToolExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NexusProperties.HttpProperties httpProperties;
    private final Map<String, Retry> retryRegistry = new ConcurrentHashMap<>();

    // 参数占位符匹配模式: #{paramName} 或 {paramName}
    private static final Pattern PARAM_PATTERN = Pattern.compile("#?\\{(\\w+)}");

    public HttpToolExecutor(
            @Qualifier("toolRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            NexusProperties nexusProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.httpProperties = nexusProperties.getHttp();
        log.info("[HttpToolExecutor] 初始化完成: connectTimeout={}ms, readTimeout={}ms, retryCount={}",
            httpProperties.getConnectTimeout(), httpProperties.getReadTimeout(), httpProperties.getRetryCount());
    }

    /**
     * 执行HTTP工具
     *
     * @param descriptor 工具描述符
     * @param params     输入参数
     * @return 执行结果
     */
    public McpToolResult execute(McpToolDescriptor descriptor, Map<String, Object> params) {
        Map<String, Object> config = descriptor.getConfig();
        if (config == null) {
            return McpToolResult.error(descriptor.getToolId(), "CONFIG_MISSING", "工具配置缺失");
        }

        String url = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "GET");
        
        if (url == null || url.isEmpty()) {
            return McpToolResult.error(descriptor.getToolId(), "URL_MISSING", "请求URL未配置");
        }

        try {
            // 替换URL中的参数占位符
            url = replaceParameters(url, params);

            // 构建请求头
            HttpHeaders headers = buildHeaders(config, params);

            // 构建请求实体
            HttpEntity<?> requestEntity;
            if ("GET".equalsIgnoreCase(method)) {
                requestEntity = new HttpEntity<>(headers);
            } else {
                requestEntity = new HttpEntity<>(params, headers);
            }

            // 使用重试机制执行HTTP请求
            String finalUrl = url;
            Retry retry = getOrCreateRetry(descriptor.getToolId());
            
            ResponseEntity<String> response = Retry.decorateSupplier(retry, () -> 
                executeRequest(finalUrl, method, requestEntity)
            ).get();

            // 处理响应
            if (response.getStatusCode().is2xxSuccessful()) {
                Object resultData = extractResponseData(response.getBody(), config);
                log.info("HTTP工具执行成功: {}", descriptor.getName());
                return McpToolResult.success(descriptor.getToolId(), resultData);
            } else {
                return McpToolResult.error(descriptor.getToolId(), 
                        "HTTP_ERROR", 
                        "HTTP请求失败: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("HTTP工具执行异常: {}", descriptor.getName(), e);
            return McpToolResult.error(descriptor.getToolId(), "REQUEST_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("HTTP工具执行异常: {}", descriptor.getName(), e);
            return McpToolResult.error(descriptor.getToolId(), "EXECUTION_ERROR", e.getMessage());
        }
    }

    /**
     * 获取或创建重试器
     */
    private Retry getOrCreateRetry(String toolId) {
        return retryRegistry.computeIfAbsent(toolId, id -> {
            RetryConfig config = RetryConfig.custom()
                .maxAttempts(httpProperties.getRetryCount())
                .waitDuration(Duration.ofMillis(httpProperties.getRetryInterval()))
                .retryOnException(e -> {
                    // 只对网络异常重试，业务异常不重试
                    return e instanceof RestClientException;
                })
                .build();
            return RetryRegistry.of(config).retry(id);
        });
    }

    /**
     * 构建请求头
     */
    @SuppressWarnings("unchecked")
    private HttpHeaders buildHeaders(Map<String, Object> config, Map<String, Object> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 添加自定义请求头
        Map<String, String> customHeaders = (Map<String, String>) config.get("headers");
        if (customHeaders != null) {
            customHeaders.forEach((key, value) -> {
                value = replaceParameters(value, params);
                headers.set(key, value);
            });
        }

        // 处理认证
        String authType = (String) config.get("authType");
        if (authType != null && !"none".equals(authType)) {
            addAuthHeaders(headers, authType, config, params);
        }

        return headers;
    }

    /**
     * 添加认证头
     */
    @SuppressWarnings("unchecked")
    private void addAuthHeaders(HttpHeaders headers, String authType, 
                                Map<String, Object> config, Map<String, Object> params) {
        Map<String, Object> authConfig = (Map<String, Object>) config.get("authConfig");
        if (authConfig == null) {
            return;
        }

        switch (authType) {
            case "bearer":
                String token = (String) authConfig.get("token");
                if (token != null) {
                    token = replaceParameters(token, params);
                    headers.setBearerAuth(token);
                }
                break;
            case "basic":
                String username = (String) authConfig.get("username");
                String password = (String) authConfig.get("password");
                if (username != null && password != null) {
                    username = replaceParameters(username, params);
                    password = replaceParameters(password, params);
                    headers.setBasicAuth(username, password);
                }
                break;
            case "api_key":
                String apiKey = (String) authConfig.get("apiKey");
                String headerName = (String) authConfig.getOrDefault("headerName", "X-API-Key");
                if (apiKey != null) {
                    apiKey = replaceParameters(apiKey, params);
                    headers.set(headerName, apiKey);
                }
                break;
            default:
                log.warn("不支持的认证类型: {}", authType);
        }
    }

    /**
     * 执行HTTP请求
     */
    private ResponseEntity<String> executeRequest(String url, String method, 
                                                   HttpEntity<?> requestEntity) {
        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
        log.debug("执行HTTP请求: {} {}", method, url);
        return restTemplate.exchange(url, httpMethod, requestEntity, String.class);
    }

    /**
     * 提取响应数据
     */
    private Object extractResponseData(String responseBody, Map<String, Object> config) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }

        String responsePath = (String) config.get("responsePath");
        if (responsePath == null || responsePath.isEmpty()) {
            // 直接返回整个响应
            try {
                return objectMapper.readValue(responseBody, Object.class);
            } catch (Exception e) {
                return responseBody;
            }
        }

        // 使用JSONPath提取数据
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode result = extractByPath(root, responsePath);
            return objectMapper.treeToValue(result, Object.class);
        } catch (Exception e) {
            log.warn("响应数据提取失败: {}", e.getMessage());
            return responseBody;
        }
    }

    /**
     * 根据路径提取JSON数据（简化版JSONPath）
     */
    private JsonNode extractByPath(JsonNode root, String path) {
        if (path == null || path.isEmpty() || path.equals("$")) {
            return root;
        }

        // 移除开头的 $.
        if (path.startsWith("$.")) {
            path = path.substring(2);
        }

        JsonNode current = root;
        String[] parts = path.split("\\.");
        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                break;
            }
            // 处理数组索引 [0]
            if (part.contains("[")) {
                String fieldName = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                current = current.path(fieldName).get(index);
            } else {
                current = current.path(part);
            }
        }
        return current;
    }

    /**
     * 替换参数占位符
     */
    private String replaceParameters(String template, Map<String, Object> params) {
        if (template == null || params == null) {
            return template;
        }

        Matcher matcher = PARAM_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = params.get(paramName);
            matcher.appendReplacement(result, value != null ? value.toString() : "");
        }
        matcher.appendTail(result);
        return result.toString();
    }

}
