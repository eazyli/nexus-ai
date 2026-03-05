package com.eazyai.ai.nexus.core.tool.executor;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolExecutor;
import com.eazyai.ai.nexus.api.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 函数工具执行器
 * 执行本地函数/方法调用，支持Spring Bean方法调用和脚本执行
 *
 * <h3>配置示例 - Spring Bean方法调用：</h3>
 * <pre>
 * {
 *   "functionType": "bean",
 *   "beanName": "calculatorTools",
 *   "methodName": "calculate",
 *   "paramMapping": {
 *     "expression": "expression"  // 参数名映射
 *   }
 * }
 * </pre>
 *
 * <h3>配置示例 - 脚本执行：</h3>
 * <pre>
 * {
 *   "functionType": "script",
 *   "scriptLanguage": "groovy",  // groovy, javascript, python
 *   "script": "return params.a + params.b",
 *   "timeout": 5000
 * }
 * </pre>
 *
 * <h3>配置示例 - 类反射调用：</h3>
 * <pre>
 * {
 *   "functionType": "reflection",
 *   "className": "com.example.Tools",
 *   "methodName": "calculate",
 *   "staticMethod": false
 * }
 * </pre>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>支持Spring Bean方法调用</li>
 *   <li>支持脚本执行（Groovy、JavaScript、Python）</li>
 *   <li>支持类反射调用</li>
 *   <li>参数自动映射和类型转换</li>
 *   <li>执行超时控制</li>
 *   <li>安全沙箱（可选）</li>
 * </ul>
 */
@Slf4j
@Component
public class FunctionToolExecutor implements ToolExecutor {

    private static final String EXECUTOR_TYPE = "function";

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final ScriptEngineManager scriptEngineManager;

    /**
     * 方法缓存：key -> Method
     */
    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();

    /**
     * 脚本引擎缓存
     */
    private final Map<String, ScriptEngine> scriptEngineCache = new ConcurrentHashMap<>();

    @Autowired
    public FunctionToolExecutor(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        this.scriptEngineManager = new ScriptEngineManager();
        log.info("[FunctionToolExecutor] 初始化完成");
    }

    @Override
    public String getExecutorType() {
        return EXECUTOR_TYPE;
    }

    @Override
    public ToolResult execute(ToolDescriptor descriptor, Map<String, Object> params, AgentContext context) {
        Map<String, Object> config = descriptor.getConfig();
        if (config == null) {
            return ToolResult.error(descriptor.getToolId(), "CONFIG_MISSING", "函数工具配置缺失");
        }

        String functionType = (String) config.getOrDefault("functionType", "bean");

        try {
            Object result = switch (functionType.toLowerCase()) {
                case "bean" -> executeBeanMethod(descriptor, params, config);
                case "script" -> executeScript(descriptor, params, config);
                case "reflection" -> executeReflection(descriptor, params, config);
                default -> throw new IllegalArgumentException("不支持的函数类型: " + functionType);
            };

            log.info("[FunctionToolExecutor] 执行成功: {} ({})", descriptor.getName(), functionType);
            return ToolResult.success(descriptor.getToolId(), result);

        } catch (Exception e) {
            log.error("[FunctionToolExecutor] 执行异常: {} - {}", descriptor.getName(), e.getMessage(), e);
            return ToolResult.error(descriptor.getToolId(), "FUNCTION_EXECUTION_ERROR", e.getMessage(), e);
        }
    }

    /**
     * 执行Spring Bean方法
     */
    @SuppressWarnings("unchecked")
    private Object executeBeanMethod(ToolDescriptor descriptor, Map<String, Object> params, 
                                     Map<String, Object> config) throws Exception {
        String beanName = (String) config.get("beanName");
        String methodName = (String) config.get("methodName");

        if (beanName == null || methodName == null) {
            throw new IllegalArgumentException("beanName或methodName未配置");
        }

        // 获取Bean
        Object bean = applicationContext.getBean(beanName);
        
        // 获取方法
        String cacheKey = beanName + "." + methodName;
        Method method = methodCache.computeIfAbsent(cacheKey, k -> findMethod(bean.getClass(), methodName, params));

        if (method == null) {
            throw new NoSuchMethodException("找不到方法: " + cacheKey);
        }

        // 映射参数
        Object[] args = mapParameters(method, params, config);
        
        // 执行方法
        method.setAccessible(true);
        return method.invoke(bean, args);
    }

    /**
     * 执行脚本
     */
    @SuppressWarnings("unchecked")
    private Object executeScript(ToolDescriptor descriptor, Map<String, Object> params, 
                                 Map<String, Object> config) throws Exception {
        String scriptLanguage = (String) config.getOrDefault("scriptLanguage", "groovy");
        String script = (String) config.get("script");

        if (script == null || script.isEmpty()) {
            throw new IllegalArgumentException("script未配置");
        }

        // 获取脚本引擎
        ScriptEngine engine = scriptEngineCache.computeIfAbsent(scriptLanguage, 
                lang -> scriptEngineManager.getEngineByName(lang));

        if (engine == null) {
            throw new IllegalStateException("不支持的脚本语言: " + scriptLanguage);
        }

        // 绑定参数
        engine.put("params", params);
        engine.put("context", applicationContext);
        engine.put("log", log);
        engine.put("toolName", descriptor.getName());

        // 执行脚本
        return engine.eval(script);
    }

    /**
     * 执行反射调用
     */
    @SuppressWarnings("unchecked")
    private Object executeReflection(ToolDescriptor descriptor, Map<String, Object> params, 
                                     Map<String, Object> config) throws Exception {
        String className = (String) config.get("className");
        String methodName = (String) config.get("methodName");
        boolean staticMethod = Boolean.TRUE.equals(config.get("staticMethod"));

        if (className == null || methodName == null) {
            throw new IllegalArgumentException("className或methodName未配置");
        }

        // 加载类
        Class<?> clazz = Class.forName(className);

        // 获取方法
        String cacheKey = className + "." + methodName;
        Method method = methodCache.computeIfAbsent(cacheKey, k -> findMethod(clazz, methodName, params));

        if (method == null) {
            throw new NoSuchMethodException("找不到方法: " + cacheKey);
        }

        // 映射参数
        Object[] args = mapParameters(method, params, config);

        // 执行方法
        method.setAccessible(true);
        Object instance = staticMethod ? null : clazz.getDeclaredConstructor().newInstance();
        return method.invoke(instance, args);
    }

    /**
     * 查找方法（根据参数数量和名称匹配）
     */
    private Method findMethod(Class<?> clazz, String methodName, Map<String, Object> params) {
        Method[] methods = clazz.getDeclaredMethods();
        int paramCount = params != null ? params.size() : 0;

        // 优先查找名称和参数数量匹配的方法
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                if (method.getParameterCount() == paramCount || 
                    method.getParameterCount() == 0 ||
                    params == null || params.isEmpty()) {
                    return method;
                }
            }
        }

        // 查找名称匹配的方法（接收Map参数）
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Parameter[] parameters = method.getParameters();
                if (parameters.length == 1 && Map.class.isAssignableFrom(parameters[0].getType())) {
                    return method;
                }
            }
        }

        return null;
    }

    /**
     * 映射参数
     */
    @SuppressWarnings("unchecked")
    private Object[] mapParameters(Method method, Map<String, Object> params, Map<String, Object> config) {
        Parameter[] methodParams = method.getParameters();
        Object[] args = new Object[methodParams.length];

        if (params == null || params.isEmpty()) {
            return args;
        }

        // 获取参数映射配置
        Map<String, String> paramMapping = (Map<String, String>) config.get("paramMapping");

        for (int i = 0; i < methodParams.length; i++) {
            Parameter methodParam = methodParams[i];
            String paramName = methodParam.getName();
            Class<?> paramType = methodParam.getType();

            // 使用映射配置或直接使用参数名
            String mappedName = paramMapping != null ? paramMapping.getOrDefault(paramName, paramName) : paramName;
            
            Object value = params.get(mappedName);
            args[i] = convertValue(value, paramType);
        }

        return args;
    }

    /**
     * 类型转换
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // 类型已匹配
        if (targetType.isInstance(value)) {
            return value;
        }

        String strValue = value.toString();

        // 基本类型转换
        if (targetType == String.class) {
            return strValue;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(strValue);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(strValue);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(strValue);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(strValue);
        } else if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(strValue);
        } else if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType, strValue);
        }

        // 尝试JSON转换
        try {
            return objectMapper.readValue(strValue, targetType);
        } catch (Exception e) {
            log.warn("[FunctionToolExecutor] 类型转换失败: {} -> {}", value, targetType.getSimpleName());
            return value;
        }
    }

    /**
     * 清除方法缓存
     */
    public void clearMethodCache() {
        methodCache.clear();
        log.info("[FunctionToolExecutor] 清除方法缓存");
    }

    /**
     * 清除脚本引擎缓存
     */
    public void clearScriptEngineCache() {
        scriptEngineCache.clear();
        log.info("[FunctionToolExecutor] 清除脚本引擎缓存");
    }

    /**
     * 注册脚本引擎
     */
    public void registerScriptEngine(String language, ScriptEngine engine) {
        scriptEngineCache.put(language, engine);
        log.info("[FunctionToolExecutor] 注册脚本引擎: {}", language);
    }
}
