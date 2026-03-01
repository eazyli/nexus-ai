package com.eazyai.ai.nexus.api.model;

/**
 * 模型能力枚举
 */
public enum ModelCapability {
    
    /**
     * 文本对话
     */
    CHAT("文本对话"),
    
    /**
     * 流式输出
     */
    STREAMING("流式输出"),
    
    /**
     * 函数调用/工具调用
     */
    FUNCTION_CALLING("函数调用"),
    
    /**
     * 视觉理解
     */
    VISION("视觉理解"),
    
    /**
     * 向量嵌入
     */
    EMBEDDING("向量嵌入"),
    
    /**
     * 重排序
     */
    RERANK("重排序"),
    
    /**
     * 图像生成
     */
    IMAGE_GENERATION("图像生成"),
    
    /**
     * 代码生成
     */
    CODE_GENERATION("代码生成"),
    
    /**
     * JSON结构化输出
     */
    JSON_OUTPUT("JSON结构化输出"),
    
    /**
     * 长上下文
     */
    LONG_CONTEXT("长上下文");

    private final String description;

    ModelCapability(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
