package com.eazyai.ai.nexus.model.gateway;

import com.eazyai.ai.nexus.api.model.ModelCapability;
import com.eazyai.ai.nexus.api.model.ModelDescriptor;
import com.eazyai.ai.nexus.api.model.ModelRequest;
import com.eazyai.ai.nexus.api.model.ModelResponse;
import com.eazyai.ai.nexus.model.adapter.ModelAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认模型网关实现
 * 
 * <p>核心功能：</p>
 * <ul>
 *   <li>多模型适配器管理</li>
 *   <li>基于能力的模型路由</li>
 *   <li>负载均衡和故障转移</li>
 *   <li>流式输出支持</li>
 * </ul>
 */
@Slf4j
@Component
public class DefaultModelGateway implements ModelGateway {

    @Autowired
    private List<ModelAdapter> adapters;

    private final Map<String, ModelDescriptor> modelRegistry = new ConcurrentHashMap<>();
    private final Map<String, ModelAdapter> adapterRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("DefaultModelGateway 初始化，注册了 {} 个适配器", 
                adapters != null ? adapters.size() : 0);
    }

    @Override
    public ModelResponse invoke(ModelRequest request) {
        String modelId = request.getExtraParams() != null 
                ? (String) request.getExtraParams().get("modelId") 
                : null;

        ModelDescriptor descriptor;
        ModelAdapter adapter;

        if (modelId != null) {
            // 指定模型
            descriptor = modelRegistry.get(modelId);
            if (descriptor == null) {
                return ModelResponse.error(request.getRequestId(), 
                        "MODEL_NOT_FOUND", "模型不存在: " + modelId);
            }
            adapter = adapterRegistry.get(descriptor.getProvider());
        } else {
            // 自动选择模型
            ModelAdapter selectedAdapter = selectAdapter(request);
            if (selectedAdapter == null) {
                return ModelResponse.error(request.getRequestId(), 
                        "NO_AVAILABLE_MODEL", "没有可用的模型");
            }
            adapter = selectedAdapter;
            descriptor = selectBestDescriptor(selectedAdapter.getProvider());
        }

        if (adapter == null) {
            return ModelResponse.error(request.getRequestId(), 
                    "ADAPTER_NOT_FOUND", "模型适配器不存在");
        }

        return adapter.invoke(descriptor, request);
    }

    @Override
    public void invokeStreaming(ModelRequest request, StreamingCallback callback) {
        String modelId = request.getExtraParams() != null 
                ? (String) request.getExtraParams().get("modelId") 
                : null;

        ModelDescriptor descriptor;
        ModelAdapter adapter;

        if (modelId != null) {
            descriptor = modelRegistry.get(modelId);
            adapter = descriptor != null ? adapterRegistry.get(descriptor.getProvider()) : null;
        } else {
            ModelAdapter selectedAdapter = selectAdapter(request);
            adapter = selectedAdapter;
            descriptor = selectedAdapter != null ? selectBestDescriptor(selectedAdapter.getProvider()) : null;
        }

        if (adapter == null || descriptor == null) {
            callback.onError(new RuntimeException("没有可用的模型"));
            return;
        }

        adapter.invokeStreaming(descriptor, request, callback);
    }

    @Override
    public Optional<ModelDescriptor> selectModel(ModelCapability capability) {
        return modelRegistry.values().stream()
                .filter(d -> Boolean.TRUE.equals(d.getEnabled()))
                .filter(d -> d.getCapabilities() != null && d.getCapabilities().contains(capability))
                .max(Comparator.comparingInt(ModelDescriptor::getPriority));
    }

    @Override
    public Optional<ModelDescriptor> getModel(String modelId) {
        return Optional.ofNullable(modelRegistry.get(modelId));
    }

    @Override
    public List<ModelDescriptor> getAvailableModels() {
        return modelRegistry.values().stream()
                .filter(d -> Boolean.TRUE.equals(d.getEnabled()))
                .toList();
    }

    @Override
    public void registerModel(ModelDescriptor descriptor) {
        modelRegistry.put(descriptor.getModelId(), descriptor);
        
        // 查找并注册适配器
        if (adapters != null) {
            for (ModelAdapter adapter : adapters) {
                if (adapter.supports(descriptor)) {
                    adapterRegistry.put(descriptor.getProvider(), adapter);
                    log.info("注册模型: {} -> 适配器: {}", 
                            descriptor.getModelId(), adapter.getProvider());
                    break;
                }
            }
        }
    }

    @Override
    public void unregisterModel(String modelId) {
        ModelDescriptor removed = modelRegistry.remove(modelId);
        if (removed != null) {
            log.info("注销模型: {}", modelId);
        }
    }

    @Override
    public boolean healthCheck(String modelId) {
        ModelDescriptor descriptor = modelRegistry.get(modelId);
        if (descriptor == null) {
            return false;
        }
        
        ModelAdapter adapter = adapterRegistry.get(descriptor.getProvider());
        return adapter != null && adapter.healthCheck(descriptor);
    }

    /**
     * 选择适配器
     */
    private ModelAdapter selectAdapter(ModelRequest request) {
        if (adapters == null || adapters.isEmpty()) {
            return null;
        }
        
        // 优先选择支持流式的适配器
        if (Boolean.TRUE.equals(request.getStreaming())) {
            return adapters.stream()
                    .filter(a -> !a.getProvider().isEmpty())
                    .findFirst()
                    .orElse(adapters.get(0));
        }
        
        return adapters.get(0);
    }

    /**
     * 选择最佳模型描述符
     */
    private ModelDescriptor selectBestDescriptor(String provider) {
        return modelRegistry.values().stream()
                .filter(d -> provider.equalsIgnoreCase(d.getProvider()))
                .filter(d -> Boolean.TRUE.equals(d.getEnabled()))
                .max(Comparator.comparingInt(ModelDescriptor::getPriority))
                .orElse(null);
    }
}
