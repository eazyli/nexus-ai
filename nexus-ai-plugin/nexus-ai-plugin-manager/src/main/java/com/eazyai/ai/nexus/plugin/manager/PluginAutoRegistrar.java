package com.eazyai.ai.nexus.plugin.manager;

import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.registry.PluginRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 插件自动注册器
 * Spring容器启动时自动将所有Plugin实现注册到注册中心
 */
@Slf4j
@Component
public class PluginAutoRegistrar implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private PluginRegistry registry;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Starting plugin auto-registration...");

        // 查找所有Plugin实现
        Map<String, Plugin> pluginBeans = applicationContext.getBeansOfType(Plugin.class);

        int registeredCount = 0;
        for (Map.Entry<String, Plugin> entry : pluginBeans.entrySet()) {
            Plugin plugin = entry.getValue();
            try {
                registry.register(plugin.getDescriptor(), plugin);
                registeredCount++;
            } catch (Exception e) {
                log.error("Failed to register plugin: {}", entry.getKey(), e);
            }
        }

        log.info("Plugin auto-registration completed. Registered {} plugins", registeredCount);

        // 打印已注册插件列表
        if (registeredCount > 0) {
            log.info("Registered plugins:");
            registry.getAllPlugins().forEach(desc ->
                    log.info("  - {} (type: {}, capabilities: {})",
                            desc.getName(), desc.getType(), desc.getCapabilities()));
        }
    }
}
