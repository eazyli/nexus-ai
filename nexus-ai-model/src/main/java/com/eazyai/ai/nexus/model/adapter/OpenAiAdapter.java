package com.eazyai.ai.nexus.model.adapter;

import com.eazyai.ai.nexus.api.model.ModelDescriptor;
import com.eazyai.ai.nexus.api.model.ModelRequest;
import com.eazyai.ai.nexus.api.model.ModelResponse;
import com.eazyai.ai.nexus.model.gateway.ModelGateway;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAI 模型适配器
 * 支持 GPT-4, GPT-3.5-turbo 等模型
 */
@Slf4j
@Component
public class OpenAiAdapter implements ModelAdapter {

    private final Map<String, ChatLanguageModel> chatModels = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingModels = new ConcurrentHashMap<>();

    @Override
    public String getProvider() {
        return "openai";
    }

    @Override
    public ModelResponse invoke(ModelDescriptor descriptor, ModelRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            ChatLanguageModel chatModel = getOrCreateChatModel(descriptor);
            
            List<ChatMessage> messages = buildMessages(request);
            
            Response<AiMessage> response = chatModel.generate(messages);
            
            return ModelResponse.builder()
                    .requestId(request.getRequestId())
                    .modelId(descriptor.getModelId())
                    .content(response.content().text())
                    .success(true)
                    .inputTokens(response.tokenUsage() != null ? response.tokenUsage().inputTokenCount() : 0)
                    .outputTokens(response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : 0)
                    .totalTokens(response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : 0)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .finishReason(response.finishReason() != null ? response.finishReason().name() : "stop")
                    .build();
                    
        } catch (Exception e) {
            log.error("OpenAI模型调用失败: {}", descriptor.getModelId(), e);
            return ModelResponse.builder()
                    .requestId(request.getRequestId())
                    .modelId(descriptor.getModelId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .errorCode("MODEL_ERROR")
                    .responseTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public void invokeStreaming(ModelDescriptor descriptor, ModelRequest request, 
                                ModelGateway.StreamingCallback callback) {
        try {
            StreamingChatLanguageModel streamingModel = getOrCreateStreamingModel(descriptor);
            
            List<ChatMessage> messages = buildMessages(request);
            
            streamingModel.generate(messages, new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    if (token != null && !token.isEmpty()) {
                        callback.onToken(token);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    ModelResponse modelResponse = ModelResponse.builder()
                            .requestId(request.getRequestId())
                            .modelId(descriptor.getModelId())
                            .content(response.content().text())
                            .success(true)
                            .build();
                    callback.onComplete(modelResponse);
                }

                @Override
                public void onError(Throwable error) {
                    callback.onError(error);
                }
            });
            
        } catch (Exception e) {
            log.error("OpenAI流式调用失败: {}", descriptor.getModelId(), e);
            callback.onError(e);
        }
    }

    @Override
    public boolean healthCheck(ModelDescriptor descriptor) {
        try {
            ChatLanguageModel model = getOrCreateChatModel(descriptor);
            Response<AiMessage> response = model.generate(UserMessage.from("ping"));
            return response != null && response.content() != null;
        } catch (Exception e) {
            log.warn("模型健康检查失败: {}", descriptor.getModelId(), e);
            return false;
        }
    }

    @Override
    public boolean supports(ModelDescriptor descriptor) {
        return "openai".equalsIgnoreCase(descriptor.getProvider());
    }

    /**
     * 获取或创建聊天模型
     */
    private ChatLanguageModel getOrCreateChatModel(ModelDescriptor descriptor) {
        return chatModels.computeIfAbsent(descriptor.getModelId(), id -> {
            Map<String, Object> config = descriptor.getConfig();
            String apiKey = config != null ? (String) config.get("apiKey") : null;
            String baseUrl = config != null ? (String) config.get("baseUrl") : null;
            
            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .apiKey(apiKey != null ? apiKey : System.getenv("OPENAI_API_KEY"))
                    .modelName(descriptor.getName())
                    .timeout(Duration.ofSeconds(60));
            
            if (baseUrl != null) {
                builder.baseUrl(baseUrl);
            }
            
            return builder.build();
        });
    }

    /**
     * 获取或创建流式模型
     */
    private StreamingChatLanguageModel getOrCreateStreamingModel(ModelDescriptor descriptor) {
        return streamingModels.computeIfAbsent(descriptor.getModelId() + "_streaming", id -> {
            Map<String, Object> config = descriptor.getConfig();
            String apiKey = config != null ? (String) config.get("apiKey") : null;
            String baseUrl = config != null ? (String) config.get("baseUrl") : null;
            
            OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                    .apiKey(apiKey != null ? apiKey : System.getenv("OPENAI_API_KEY"))
                    .modelName(descriptor.getName())
                    .timeout(Duration.ofSeconds(60));
            
            if (baseUrl != null) {
                builder.baseUrl(baseUrl);
            }
            
            return builder.build();
        });
    }

    /**
     * 构建消息列表
     */
    private List<ChatMessage> buildMessages(ModelRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统提示
        if (request.getSystemPrompt() != null) {
            messages.add(SystemMessage.from(request.getSystemPrompt()));
        }
        
        // 添加对话历史
        if (request.getMessages() != null) {
            for (ModelRequest.Message msg : request.getMessages()) {
                switch (msg.getRole().toLowerCase()) {
                    case "system":
                        messages.add(SystemMessage.from(msg.getContent()));
                        break;
                    case "user":
                        messages.add(UserMessage.from(msg.getContent()));
                        break;
                    case "assistant":
                        messages.add(AiMessage.from(msg.getContent()));
                        break;
                }
            }
        }
        
        // 添加当前输入
        if (request.getInput() != null) {
            messages.add(UserMessage.from(request.getInput()));
        }
        
        return messages;
    }
}
