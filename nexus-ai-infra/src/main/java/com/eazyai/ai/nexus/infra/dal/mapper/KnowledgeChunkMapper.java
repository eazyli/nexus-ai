package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.KnowledgeChunkEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库切片 Mapper
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkEntity> {

    /**
     * 按知识库ID查询切片
     */
    @Select("SELECT * FROM ai_knowledge_chunk WHERE knowledge_id = #{knowledgeId} ORDER BY chunk_index")
    List<KnowledgeChunkEntity> findByKnowledgeId(@Param("knowledgeId") String knowledgeId);

    /**
     * 按文档ID查询切片
     */
    @Select("SELECT * FROM ai_knowledge_chunk WHERE doc_id = #{docId} ORDER BY chunk_index")
    List<KnowledgeChunkEntity> findByDocId(@Param("docId") Long docId);

    /**
     * 删除知识库的所有切片
     */
    @Delete("DELETE FROM ai_knowledge_chunk WHERE knowledge_id = #{knowledgeId}")
    int deleteByKnowledgeId(@Param("knowledgeId") String knowledgeId);

    /**
     * 删除文档的所有切片
     */
    @Delete("DELETE FROM ai_knowledge_chunk WHERE doc_id = #{docId}")
    int deleteByDocId(@Param("docId") Long docId);

    /**
     * 统计知识库切片数量
     */
    @Select("SELECT COUNT(*) FROM ai_knowledge_chunk WHERE knowledge_id = #{knowledgeId}")
    int countByKnowledgeId(@Param("knowledgeId") String knowledgeId);

    /**
     * 统计文档切片数量
     */
    @Select("SELECT COUNT(*) FROM ai_knowledge_chunk WHERE doc_id = #{docId}")
    int countByDocId(@Param("docId") Long docId);
}
