package com.eazyai.ai.nexus.api.application;

import java.util.List;
import java.util.Optional;

/**
 * 应用仓储接口
 * 
 * <p>定义应用数据访问的抽象接口</p>
 * <p>由 infra 层实现具体的存储访问</p>
 */
public interface AppRepository {

    /**
     * 根据ID查找应用
     */
    Optional<AppDescriptor> findById(String appId);

    /**
     * 根据ID和状态查找应用
     */
    Optional<AppDescriptor> findByAppIdAndStatus(String appId, Integer status);

    /**
     * 根据租户ID查找应用列表
     */
    List<AppDescriptor> findByTenantId(String tenantId);

    /**
     * 查找所有启用的应用
     */
    List<AppDescriptor> findAllEnabled();

    /**
     * 保存应用
     */
    void save(AppDescriptor app);

    /**
     * 更新应用
     */
    void update(AppDescriptor app);

    /**
     * 删除应用
     */
    void deleteById(String appId);
}
