package com.eazyai.ai.nexus.web.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 函数工具注册请求
 */
@Data
public class FunctionToolRegisterRequest {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 函数类型: bean/script/reflection
     */
    private String functionType = "bean";

    /**
     * Bean名称（functionType=bean时必填）
     */
    private String beanName;

    /**
     * 方法名称（functionType=bean或reflection时必填）
     */
    private String methodName;

    /**
     * 类名（functionType=reflection时必填）
     */
    private String className;

    /**
     * 是否静态方法（functionType=reflection时生效）
     */
    private Boolean staticMethod = false;

    /**
     * 脚本语言（functionType=script时必填）: groovy/javascript/python
     */
    private String scriptLanguage;

    /**
     * 脚本内容（functionType=script时必填）
     */
    private String script;

    /**
     * 参数映射配置
     * key: 方法参数名, value: 工具参数名
     */
    private Map<String, String> paramMapping;

    /**
     * 参数定义
     */
    private List<ParamDefinition> parameters;

    /**
     * 工具可见性: PRIVATE/PUBLIC/SHARED
     */
    private String visibility = "PRIVATE";

    /**
     * 授权应用列表（visibility=SHARED时生效）
     */
    private List<String> authorizedApps;

    /**
     * 能力标签
     */
    private List<String> capabilities;

    /**
     * 超时时间（毫秒）
     */
    private Long timeout = 10000L;

    @Data
    public static class ParamDefinition {
        private String name;
        private String type = "string";
        private String description;
        private Boolean required = false;
        private Object defaultValue;
        private List<String> options;
    }
}
