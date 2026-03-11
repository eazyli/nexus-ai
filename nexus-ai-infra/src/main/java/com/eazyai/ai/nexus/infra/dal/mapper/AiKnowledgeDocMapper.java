package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiKnowledgeDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 知识库文档 Mapper
 */
@Mapper
public interface AiKnowledgeDocMapper extends BaseMapper<AiKnowledgeDoc> {
    @Update("DELETE from ai_knowledge_doc where knowledge_id = #{knowledgeId}")
    int deleteDoc(@Param("knowledgeId") String knowledgeId);

}
