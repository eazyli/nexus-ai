package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.KnowledgeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<KnowledgeEntity> {

    /**
     * 按应用ID查询知识库列表
     */
    @Select("SELECT * FROM ai_knowledge WHERE app_id = #{appId}")
    List<KnowledgeEntity> findByAppId(@Param("appId") String appId);

    /**
     * 更新处理状态
     */
    @Update("UPDATE ai_knowledge SET status = #{status}, process_progress = #{progress}, update_time = NOW() WHERE knowledge_id = #{knowledgeId}")
    int updateStatus(@Param("knowledgeId") String knowledgeId, @Param("status") Integer status, @Param("progress") Integer progress);

    /**
     * 更新切片数量
     */
    @Update("UPDATE ai_knowledge SET chunk_count = #{chunkCount}, update_time = NOW() WHERE knowledge_id = #{knowledgeId}")
    int updateChunkCount(@Param("knowledgeId") String knowledgeId, @Param("chunkCount") Integer chunkCount);

    /**
     * 更新文档数量
     */
    @Update("UPDATE ai_knowledge SET doc_count = #{docCount}, update_time = NOW() WHERE knowledge_id = #{knowledgeId}")
    int updateDocCount(@Param("knowledgeId") String knowledgeId, @Param("docCount") Integer docCount);

    /**
     * 增加切片数量
     */
    @Update("UPDATE ai_knowledge SET chunk_count = chunk_count + #{increment}, update_time = NOW() WHERE knowledge_id = #{knowledgeId}")
    int incrementChunkCount(@Param("knowledgeId") String knowledgeId, @Param("increment") Integer increment);
}
