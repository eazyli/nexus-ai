package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 知识库表
 */
@Getter
@Setter
@TableName(value = "ai_knowledge", autoResultMap = true)
public class AiKnowledge {

    @TableId("knowledge_id")
    private String knowledgeId;

    private String knowledgeName;

    /**
     * 知识库类型: document/database/api
     */
    private String knowledgeType;

    private String description;

    private String appId;

    /**
     * 文件列表（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> fileList;

    /**
     * 数据源配置（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> dataSource;

    /**
     * 向量库配置（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> vectorDbConfig;

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
     * 状态: 0处理中/1成功/2失败
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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
