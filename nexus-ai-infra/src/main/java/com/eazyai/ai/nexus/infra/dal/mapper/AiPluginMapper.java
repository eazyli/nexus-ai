package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiPlugin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 插件 Mapper
 */
@Mapper
public interface AiPluginMapper extends BaseMapper<AiPlugin> {
}
