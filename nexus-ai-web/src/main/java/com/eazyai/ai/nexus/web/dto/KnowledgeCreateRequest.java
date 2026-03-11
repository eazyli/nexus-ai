package com.eazyai.ai.nexus.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 创建知识库请求
 */
@Data
public class KnowledgeCreateRequest {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    private String knowledgeName;

    /**
     * 所属应用ID（可选）
     */
    private String appId;

    /**
     * 知识库类型
     */
    private String knowledgeType = "document";

    /**
     * 描述
     */
    private String description;

    /**
     * 配置参数
     */
    private Map<String, Object> config;
}
