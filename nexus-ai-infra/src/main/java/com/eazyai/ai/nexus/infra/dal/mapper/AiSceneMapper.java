package com.eazyai.ai.nexus.infra.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eazyai.ai.nexus.infra.dal.entity.AiScene;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场景配置 Mapper
 */
@Mapper
public interface AiSceneMapper extends BaseMapper<AiScene> {
}
