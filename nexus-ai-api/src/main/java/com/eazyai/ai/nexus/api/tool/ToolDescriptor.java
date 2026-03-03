package com.eazyai.ai.nexus.api.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 统一工具描述符
 * 描述工具的元数据信息，支持多协议接入
 *
 * <p>工具描述符是工具的"身份证"，包含工具的基本信息、参数定义、配置等。</p>
 * <p>不同协议(MCP/OpenAI/LangChain)的工具都可以映射到此统一描述符。</p>
 *
 * <h3>协议映射示例：</h3>
 * <pre>
 * MCP Tool → ToolDescriptor
 *   - name → name
 *   - inputSchema → parameters
 *   - annotations → capabilities
 *
 * OpenAI Function → ToolDescriptor
 *   - function.name → name
 *   - function.parameters → parameters
 *   - function.description → description
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工具唯一标识
     */
    private String toolId;

    /**
     * 所属应用/服务ID
     */
    private String appId;

    /**
     * 工具名称（用于LLM识别）
     */
    private String name;

    /**
     * 工具描述（用于LLM理解工具用途）
     */
    private String description;

    /**
     * 执行器类型
     * 决定由哪个执行器执行此工具
     * 如: http, db, function, grpc, dubbo, mcp_client等
     */
    private String executorType;

    /**
     * 协议来源
     * 标识工具来自哪个协议层
     * 如: mcp, openai, langchain, internal
     */
    private String protocol;

    /**
     * 工具能力标签
     * 用于工具发现和匹配
     */
    private List<String> capabilities;

    /**
     * 输入参数定义
     */
    private List<ParamDefinition> parameters;

    /**
     * 返回值定义
     */
    private ReturnDefinition returns;

    /**
     * 工具执行配置
     * 不同执行器类型有不同的配置结构
     *
     * <p>HTTP执行器配置示例：</p>
     * <pre>
     * {
     *   "url": "https://api.example.com/users/#{userId}",
     *   "method": "GET",
     *   "headers": {"Authorization": "Bearer #{token}"},
     *   "timeout": 5000
     * }
     * </pre>
     *
     * <p>DB执行器配置示例：</p>
     * <pre>
     * {
     *   "datasourceId": "primary-db",
     *   "sqlTemplate": "SELECT * FROM users WHERE id = #{userId}",
     *   "queryType": "SELECT"
     * }
     * </pre>
     */
    private Map<String, Object> config;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 超时时间（毫秒）
     */
    private Long timeout;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否幂等
     * 用于判断是否可以重试
     */
    private Boolean idempotent;

    /**
     * 工具版本
     */
    private String version;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParamDefinition implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 参数名称
         */
        private String name;

        /**
         * 参数类型 (string, number, integer, boolean, array, object)
         */
        private String type;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 是否必填
         */
        private Boolean required;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 枚举选项
         */
        private List<String> options;

        /**
         * JSON Schema定义（用于复杂类型）
         */
        private Map<String, Object> schema;
    }

    /**
     * 返回值定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnDefinition implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 返回类型
         */
        private String type;

        /**
         * 返回值描述
         */
        private String description;

        /**
         * JSON Schema定义
         */
        private Map<String, Object> schema;
    }

    /**
     * 判断是否已启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * 判断是否幂等
     */
    public boolean isIdempotent() {
        return Boolean.TRUE.equals(idempotent);
    }
}
