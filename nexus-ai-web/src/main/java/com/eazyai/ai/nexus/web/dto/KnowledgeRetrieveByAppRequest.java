package com.eazyai.ai.nexus.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按应用检索知识库请求
 */
@Data
public class KnowledgeRetrieveByAppRequest {

    /**
     * 应用ID
     */
    @NotBlank(message = "应用ID不能为空")
    private String appId;

    /**
     * 查询文本
     */
    @NotBlank(message = "查询文本不能为空")
    private String query;

    /**
     * 返回数量
     */
    private Integer topK = 5;
}
