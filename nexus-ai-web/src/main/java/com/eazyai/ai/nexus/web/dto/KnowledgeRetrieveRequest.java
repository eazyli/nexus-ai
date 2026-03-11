package com.eazyai.ai.nexus.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 知识库检索请求
 */
@Data
public class KnowledgeRetrieveRequest {

    /**
     * 查询文本
     */
    @NotBlank(message = "查询文本不能为空")
    private String query;

    /**
     * 知识库ID列表
     */
    @NotEmpty(message = "知识库ID列表不能为空")
    private List<String> knowledgeIds;

    /**
     * 返回数量
     */
    private Integer topK = 5;
}
