package com.eazyai.ai.nexus.api.registry;

import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;

import java.util.List;
import java.util.Optional;

/**
 * 插件注册中心接口
 * 负责插件的注册、发现和生命周期管理
 */
public interface PluginRegistry {

    /**
     * 注册插件
     *
     * @param descriptor 插件描述符
     * @param plugin 插件实例
     */
    void register(PluginDescriptor descriptor, Plugin plugin);

    /**
     * 注销插件
     *
     * @param pluginId 插件ID
     */
    void unregister(String pluginId);

    /**
     * 获取插件
     *
     * @param pluginId 插件ID
     * @return 插件实例
     */
    Optional<Plugin> getPlugin(String pluginId);

    /**
     * 根据类型查找插件
     *
     * @param type 插件类型
     * @return 插件列表
     */
    List<Plugin> findByType(String type);

    /**
     * 根据能力查找插件
     *
     * @param capability 能力名称
     * @return 插件列表
     */
    List<Plugin> findByCapability(String capability);

    /**
     * 获取所有已注册插件
     *
     * @return 插件描述符列表
     */
    List<PluginDescriptor> getAllPlugins();

    /**
     * 获取插件描述符
     *
     * @param pluginId 插件ID
     * @return 插件描述符
     */
    Optional<PluginDescriptor> getDescriptor(String pluginId);

    /**
     * 检查插件是否存在
     *
     * @param pluginId 插件ID
     * @return 是否存在
     */
    boolean exists(String pluginId);

    /**
     * 加载插件（从外部源）
     *
     * @param source 插件源（文件路径、URL等）
     * @return 是否加载成功
     */
    boolean loadPlugin(String source);

    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 是否卸载成功
     */
    boolean unloadPlugin(String pluginId);

    /**
     * 启用插件
     *
     * @param pluginId 插件ID
     */
    void enable(String pluginId);

    /**
     * 禁用插件
     *
     * @param pluginId 插件ID
     */
    void disable(String pluginId);

    /**
     * 订阅插件变更事件
     *
     * @param listener 监听器
     */
    void subscribe(PluginChangeListener listener);

    /**
     * 插件变更监听器
     */
    interface PluginChangeListener {
        void onPluginRegistered(PluginDescriptor descriptor);
        void onPluginUnregistered(PluginDescriptor descriptor);
        void onPluginEnabled(PluginDescriptor descriptor);
        void onPluginDisabled(PluginDescriptor descriptor);
    }
}
