package com.eazyai.ai.nexus.core.output;

import java.util.Map;
import java.util.Optional;

/**
 * 结构化输出引擎接口
 */
public interface StructuredOutputEngine {

    /**
     * 注册输出Schema
     *
     * @param schemaId Schema ID
     * @param schema   Schema定义
     */
    void registerSchema(String schemaId, OutputSchema schema);

    /**
     * 获取Schema
     *
     * @param schemaId Schema ID
     * @return Schema定义
     */
    Optional<OutputSchema> getSchema(String schemaId);

    /**
     * 格式化输出
     *
     * @param content  原始内容
     * @param schemaId 目标Schema ID
     * @return 格式化后的输出
     */
    StructuredOutput format(String content, String schemaId);

    /**
     * 格式化输出（使用Schema对象）
     *
     * @param content 原始内容
     * @param schema  Schema定义
     * @return 格式化后的输出
     */
    StructuredOutput format(String content, OutputSchema schema);

    /**
     * 验证输出
     *
     * @param output   输出数据
     * @param schemaId Schema ID
     * @return 验证结果
     */
    ValidationResult validate(Object output, String schemaId);

    /**
     * 输出Schema定义
     */
    record OutputSchema(
        String schemaId,
        String name,
        String type, // json, xml, csv, markdown
        Map<String, Object> schema,
        String description
    ) {}

    /**
     * 结构化输出结果
     */
    record StructuredOutput(
        boolean success,
        String format,
        Object data,
        String rawContent,
        String errorMessage
    ) {}

    /**
     * 验证结果
     */
    record ValidationResult(
        boolean valid,
        java.util.List<String> errors,
        java.util.List<String> warnings
    ) {}
}
