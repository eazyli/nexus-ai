package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiMemory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 记忆 Mapper
 */
@Mapper
public interface AiMemoryMapper extends BaseMapper<AiMemory> {
}
