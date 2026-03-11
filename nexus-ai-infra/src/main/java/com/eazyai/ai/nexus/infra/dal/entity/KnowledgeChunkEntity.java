package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库切片实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_knowledge_chunk")
public class KnowledgeChunkEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 切片ID（唯一标识）
     */
    private String chunkId;

    /**
     * 知识库ID
     */
    private String knowledgeId;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 切片序号
     */
    private Integer chunkIndex;

    /**
     * 起始偏移量
     */
    private Integer startOffset;

    /**
     * 结束偏移量
     */
    private Integer endOffset;

    /**
     * 元数据（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private java.util.Map<String, Object> metadata;

    /**
     * 向量ID（ES中的_id）
     */
    private String vectorId;

    /**
     * Token数量
     */
    private Integer tokenCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
