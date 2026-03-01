package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiKnowledge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper
 */
@Mapper
public interface AiKnowledgeMapper extends BaseMapper<AiKnowledge> {
}
