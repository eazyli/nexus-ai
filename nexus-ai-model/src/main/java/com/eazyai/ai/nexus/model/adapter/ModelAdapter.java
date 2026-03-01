package com.eazyai.ai.nexus.model.adapter;

import com.eazyai.ai.nexus.api.model.ModelDescriptor;
import com.eazyai.ai.nexus.api.model.ModelRequest;
import com.eazyai.ai.nexus.api.model.ModelResponse;
import com.eazyai.ai.nexus.model.gateway.ModelGateway;

/**
 * 模型适配器接口
 * 
 * <p>不同模型提供商需要实现此接口</p>
 * <ul>
 *   <li>OpenAI - GPT系列</li>
 *   <li>Azure OpenAI</li>
 *   <li>Anthropic - Claude系列</li>
 *   <li>阿里云 - 通义千问</li>
 *   <li>DeepSeek</li>
 *   <li>Ollama - 本地模型</li>
 * </ul>
 */
public interface ModelAdapter {

    /**
     * 获取提供商名称
     *
     * @return 提供商名称
     */
    String getProvider();

    /**
     * 调用模型
     *
     * @param descriptor 模型描述符
     * @param request    模型请求
     * @return 模型响应
     */
    ModelResponse invoke(ModelDescriptor descriptor, ModelRequest request);

    /**
     * 流式调用模型
     *
     * @param descriptor 模型描述符
     * @param request    模型请求
     * @param callback   流式回调
     */
    void invokeStreaming(ModelDescriptor descriptor, ModelRequest request, 
                         ModelGateway.StreamingCallback callback);

    /**
     * 健康检查
     *
     * @param descriptor 模型描述符
     * @return 是否健康
     */
    boolean healthCheck(ModelDescriptor descriptor);

    /**
     * 是否支持该模型
     *
     * @param descriptor 模型描述符
     * @return 是否支持
     */
    boolean supports(ModelDescriptor descriptor);
}
