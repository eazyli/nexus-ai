package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiApp;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用管理 Mapper
 */
@Mapper
public interface AiAppMapper extends BaseMapper<AiApp> {
}
