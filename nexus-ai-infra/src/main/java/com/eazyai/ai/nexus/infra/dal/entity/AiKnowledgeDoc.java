package com.eazyai.ai.nexus.infra.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库文档表
 */
@Getter
@Setter
@TableName("ai_knowledge_doc")
public class AiKnowledgeDoc {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String knowledgeId;

    private String fileName;

    private String filePath;

    /**
     * 文件类型: pdf/word/excel/txt
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 状态: 0处理中/1成功/2失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
