package com.eazyai.ai.nexus.core.registry;

import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 默认插件注册中心
 * 管理插件的注册、发现和生命周期
 */
@Slf4j
@Component
public class DefaultPluginRegistry implements PluginRegistry {

    // 插件存储 (pluginId -> plugin)
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();

    // 插件描述符存储 (pluginId -> descriptor)
    private final Map<String, PluginDescriptor> descriptors = new ConcurrentHashMap<>();

    // 类型索引 (type -> list of pluginIds)
    private final Map<String, List<String>> typeIndex = new ConcurrentHashMap<>();

    // 能力索引 (capability -> list of pluginIds)
    private final Map<String, List<String>> capabilityIndex = new ConcurrentHashMap<>();

    // 监听器列表
    private final List<PluginChangeListener> listeners = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        log.info("Plugin registry initialized");
    }

    @Override
    public void register(PluginDescriptor descriptor, Plugin plugin) {
        String pluginId = descriptor.getId();

        // 注销已存在的同名插件
        if (plugins.containsKey(pluginId)) {
            log.warn("Plugin {} already exists, unregistering old version", pluginId);
            unregister(pluginId);
        }

        // 存储插件
        plugins.put(pluginId, plugin);
        descriptors.put(pluginId, descriptor);

        // 更新索引
        typeIndex.computeIfAbsent(descriptor.getType(), k -> new CopyOnWriteArrayList<>()).add(pluginId);

        for (String capability : descriptor.getCapabilities()) {
            capabilityIndex.computeIfAbsent(capability, k -> new CopyOnWriteArrayList<>()).add(pluginId);
        }

        // 初始化插件
        try {
            plugin.initialize(descriptor.getConfig());
        } catch (Exception e) {
            log.error("Failed to initialize plugin: {}", pluginId, e);
        }

        // 通知监听器
        notifyListeners(l -> l.onPluginRegistered(descriptor));

        log.info("Plugin registered: {} (type: {}, version: {})",
                pluginId, descriptor.getType(), descriptor.getVersion());
    }

    @Override
    public void unregister(String pluginId) {
        PluginDescriptor descriptor = descriptors.get(pluginId);
        if (descriptor == null) {
            return;
        }

        Plugin plugin = plugins.get(pluginId);

        // 销毁插件
        try {
            if (plugin != null) {
                plugin.destroy();
            }
        } catch (Exception e) {
            log.error("Failed to destroy plugin: {}", pluginId, e);
        }

        // 移除存储
        plugins.remove(pluginId);
        descriptors.remove(pluginId);

        // 更新索引
        typeIndex.getOrDefault(descriptor.getType(), Collections.emptyList()).remove(pluginId);
        for (String capability : descriptor.getCapabilities()) {
            capabilityIndex.getOrDefault(capability, Collections.emptyList()).remove(pluginId);
        }

        // 通知监听器
        notifyListeners(l -> l.onPluginUnregistered(descriptor));

        log.info("Plugin unregistered: {}", pluginId);
    }

    @Override
    public Optional<Plugin> getPlugin(String pluginId) {
        PluginDescriptor descriptor = descriptors.get(pluginId);
        if (descriptor == null || !descriptor.isEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(plugins.get(pluginId));
    }

    @Override
    public List<Plugin> findByType(String type) {
        List<String> pluginIds = typeIndex.getOrDefault(type, Collections.emptyList());
        return pluginIds.stream()
                .map(this::getPlugin)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Plugin> findByCapability(String capability) {
        List<String> pluginIds = capabilityIndex.getOrDefault(capability, Collections.emptyList());
        return pluginIds.stream()
                .map(this::getPlugin)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<PluginDescriptor> getAllPlugins() {
        return new ArrayList<>(descriptors.values());
    }

    @Override
    public Optional<PluginDescriptor> getDescriptor(String pluginId) {
        return Optional.ofNullable(descriptors.get(pluginId));
    }

    @Override
    public boolean exists(String pluginId) {
        return descriptors.containsKey(pluginId);
    }

    @Override
    public boolean loadPlugin(String source) {
        // TODO: 实现从外部源加载插件（如JAR文件）
        log.warn("Load plugin from external source not implemented: {}", source);
        return false;
    }

    @Override
    public boolean unloadPlugin(String pluginId) {
        if (!exists(pluginId)) {
            return false;
        }
        unregister(pluginId);
        return true;
    }

    @Override
    public void enable(String pluginId) {
        PluginDescriptor descriptor = descriptors.get(pluginId);
        if (descriptor != null) {
            descriptor.setEnabled(true);
            notifyListeners(l -> l.onPluginEnabled(descriptor));
            log.info("Plugin enabled: {}", pluginId);
        }
    }

    @Override
    public void disable(String pluginId) {
        PluginDescriptor descriptor = descriptors.get(pluginId);
        if (descriptor != null) {
            descriptor.setEnabled(false);
            notifyListeners(l -> l.onPluginDisabled(descriptor));
            log.info("Plugin disabled: {}", pluginId);
        }
    }

    @Override
    public void subscribe(PluginChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * 通知监听器
     */
    private void notifyListeners(java.util.function.Consumer<PluginChangeListener> action) {
        for (PluginChangeListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                log.error("Plugin change listener failed", e);
            }
        }
    }
}
