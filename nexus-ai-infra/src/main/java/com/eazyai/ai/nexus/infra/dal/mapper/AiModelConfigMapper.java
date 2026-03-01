package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiModelConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模型配置 Mapper
 */
@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfig> {
}
