package com.eazyai.ai.nexus.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 添加文本到知识库请求
 */
@Data
public class KnowledgeAddTextRequest {

    /**
     * 文本内容
     */
    @NotBlank(message = "文本内容不能为空")
    private String text;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}
