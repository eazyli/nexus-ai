package com.eazyai.ai.nexus.web.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP工具注册请求
 */
@Data
public class McpToolRegisterRequest {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * MCP服务器地址
     */
    private String serverUrl;

    /**
     * 传输方式: http 或 sse
     */
    private String transport = "http";

    /**
     * 自定义请求头
     */
    private Map<String, String> headers;

    /**
     * 超时时间（毫秒）
     */
    private Long timeout = 30000L;

    /**
     * 是否自动发现并注册所有工具
     * true: 发现服务器上所有工具并注册
     * false: 只注册指定的工具
     */
    private boolean autoDiscover = true;

    /**
     * 指定要注册的工具名称列表（autoDiscover=false时生效）
     */
    private List<String> toolNames;

    /**
     * 工具可见性: PRIVATE/PUBLIC/SHARED
     */
    private String visibility = "PRIVATE";

    /**
     * 授权应用列表（visibility=SHARED时生效）
     */
    private List<String> authorizedApps;
}
