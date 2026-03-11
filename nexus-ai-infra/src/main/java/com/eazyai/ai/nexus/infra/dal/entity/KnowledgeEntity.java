package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_knowledge")
public class KnowledgeEntity {

    @TableId(type = IdType.INPUT)
    private String knowledgeId;

    /**
     * 知识库名称
     */
    private String knowledgeName;

    /**
     * 知识库类型（document/database/api）
     */
    private String knowledgeType;

    /**
     * 描述
     */
    private String description;

    /**
     * 所属应用ID
     */
    private String appId;

    /**
     * 文件列表（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object fileList;

    /**
     * 数据源配置（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object dataSource;

    /**
     * 向量库配置（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object vectorDbConfig;

    /**
     * Embedding模型ID
     */
    private String embeddingModel;

    /**
     * 切片大小
     */
    private Integer chunkSize;

    /**
     * 切片重叠
     */
    private Integer chunkOverlap;

    /**
     * 状态（0处理中/1成功/2失败）
     */
    private Integer status;

    /**
     * 处理进度（0-100）
     */
    private Integer processProgress;

    /**
     * 文档数量
     */
    private Integer docCount;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
