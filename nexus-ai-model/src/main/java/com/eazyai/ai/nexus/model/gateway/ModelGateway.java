package com.eazyai.ai.nexus.model.gateway;

import com.eazyai.ai.nexus.api.model.ModelDescriptor;
import com.eazyai.ai.nexus.api.model.ModelRequest;
import com.eazyai.ai.nexus.api.model.ModelResponse;
import com.eazyai.ai.nexus.api.model.ModelCapability;

import java.util.List;
import java.util.Optional;

/**
 * 模型网关接口
 * 
 * <p>核心能力：</p>
 * <ul>
 *   <li>统一模型调用入口，屏蔽底层差异</li>
 *   <li>模型路由和负载均衡</li>
 *   <li>多模型适配器管理</li>
 *   <li>流式输出支持</li>
 * </ul>
 */
public interface ModelGateway {

    /**
     * 调用模型
     *
     * @param request 模型请求
     * @return 模型响应
     */
    ModelResponse invoke(ModelRequest request);

    /**
     * 流式调用模型
     *
     * @param request  模型请求
     * @param callback 流式回调
     */
    void invokeStreaming(ModelRequest request, StreamingCallback callback);

    /**
     * 根据能力选择模型
     *
     * @param capability 需要的能力
     * @return 模型描述符
     */
    Optional<ModelDescriptor> selectModel(ModelCapability capability);

    /**
     * 根据模型ID获取模型
     *
     * @param modelId 模型ID
     * @return 模型描述符
     */
    Optional<ModelDescriptor> getModel(String modelId);

    /**
     * 获取所有可用模型
     *
     * @return 模型列表
     */
    List<ModelDescriptor> getAvailableModels();

    /**
     * 注册模型
     *
     * @param descriptor 模型描述符
     */
    void registerModel(ModelDescriptor descriptor);

    /**
     * 注销模型
     *
     * @param modelId 模型ID
     */
    void unregisterModel(String modelId);

    /**
     * 健康检查
     *
     * @param modelId 模型ID
     * @return 是否健康
     */
    boolean healthCheck(String modelId);

    /**
     * 流式回调接口
     */
    interface StreamingCallback {
        /**
         * 收到token
         */
        void onToken(String token);

        /**
         * 完成
         */
        void onComplete(ModelResponse response);

        /**
         * 错误
         */
        void onError(Throwable error);
    }
}
