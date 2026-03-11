package com.eazyai.ai.nexus.core.rag.embedding;

import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ModelScope Embedding 模型
 * 
 * <p>使用 ModelScope 公网 API 生成文本向量</p>
 */
@Slf4j
public class ModelScopeEmbeddingModel implements EmbeddingModel {

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final int dimension;
    private final RestTemplate restTemplate = new RestTemplate();

    public ModelScopeEmbeddingModel(EmbeddingProperties properties) {
        this.baseUrl = properties.getBaseUrl();
        this.apiKey = properties.getApiKey();
        this.modelName = properties.getModel();
        this.dimension = properties.getDimension();
        log.info("[ModelScopeEmbeddingModel] 初始化完成, baseUrl: {}, model: {}, dimension: {}", 
                baseUrl, modelName, dimension);
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? new float[dimension] : results.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        log.debug("[ModelScopeEmbeddingModel] 开始生成向量, 文本数量: {}", texts.size());

        try {
            // 准备请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);  // OpenAI兼容格式需要 Bearer 前缀

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("input", texts);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String requestUrl = baseUrl + "/embeddings";
            /*log.info("[ModelScopeEmbeddingModel] 请求URL: {}, 请求头: {}, 请求体: {}",
                    requestUrl, headers, body);*/

            // 调用 ModelScope Embedding API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    requestUrl, entity, Map.class);
            
            //log.info("[ModelScopeEmbeddingModel] 响应: {}", response);

            // 解析响应
            List<float[]> embeddings = new ArrayList<>();
            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");

                for (Map<String, Object> item : dataList) {
                    @SuppressWarnings("unchecked")
                    List<Number> vector = (List<Number>) item.get("embedding");
                    float[] embedding = new float[vector.size()];
                    for (int i = 0; i < vector.size(); i++) {
                        embedding[i] = vector.get(i).floatValue();
                    }
                    embeddings.add(embedding);
                }
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.debug("[ModelScopeEmbeddingModel] 向量生成完成, 耗时: {}ms, 返回 {} 个向量", costTime, embeddings.size());

            return embeddings;

        } catch (Exception e) {
            log.error("[ModelScopeEmbeddingModel] 向量生成失败: {}", e.getMessage(), e);
            
            // 添加更详细的错误信息捕获
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException httpEx = (HttpClientErrorException) e;
                log.error("[ModelScopeEmbeddingModel] HTTP状态码: {}, 响应头: {}, 响应体: {}", 
                        httpEx.getStatusCode(), httpEx.getResponseHeaders(), httpEx.getResponseBodyAsString());
            }
            
            throw new RuntimeException("ModelScope Embedding 调用失败", e);
        }
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public String modelName() {
        return modelName;
    }
}
