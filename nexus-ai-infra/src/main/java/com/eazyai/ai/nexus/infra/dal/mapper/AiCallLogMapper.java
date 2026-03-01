package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI调用日志 Mapper
 */
@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLog> {
}
