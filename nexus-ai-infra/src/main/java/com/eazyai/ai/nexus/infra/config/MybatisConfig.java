package com.eazyai.ai.nexus.infra.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类
 * 
 * <p>负责 Mapper 接口扫描，属于基础设施层配置</p>
 */
@Configuration
@MapperScan("com.eazyai.ai.nexus.infra.dal.mapper")
public class MybatisConfig {
}
