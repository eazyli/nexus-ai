package com.eazyai.ai.nexus.infra.rag.embedding;

import com.eazyai.ai.nexus.infra.rag.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地HTTP Embedding服务
 * 支持TEI、Infinity、FastGPT等本地部署的Embedding服务
 * 兼容BGE、M3E等开源模型
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.agent.embedding.type", havingValue = "http")
public class HttpEmbeddingService implements EmbeddingService {

    @Value("${ai.agent.embedding.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${ai.agent.embedding.model:bge-large-zh-v1.5}")
    private String modelName;

    @Value("${ai.agent.embedding.dimension:1024}")
    private int dimension;

    @Value("${ai.agent.embedding.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public float[] embed(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("input", text);
            body.put("model", modelName);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/v1/embeddings", entity, Map.class);

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                if (!dataList.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    List<Double> vector = (List<Double>) dataList.get(0).get("embedding");
                    float[] result = new float[vector.size()];
                    for (int i = 0; i < vector.size(); i++) {
                        result[i] = vector.get(i).floatValue();
                    }
                    return result;
                }
            }

            throw new RuntimeException("Embedding响应格式错误");
        } catch (Exception e) {
            log.error("HTTP Embedding调用失败: {}", e.getMessage());
            throw new RuntimeException("生成向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        
        // 批量调用
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("input", texts);
            body.put("model", modelName);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/v1/embeddings", entity, Map.class);

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                
                // 按index排序
                dataList.sort((a, b) -> {
                    Integer idxA = (Integer) a.get("index");
                    Integer idxB = (Integer) b.get("index");
                    return idxA.compareTo(idxB);
                });

                for (Map<String, Object> item : dataList) {
                    @SuppressWarnings("unchecked")
                    List<Double> vector = (List<Double>) item.get("embedding");
                    float[] result = new float[vector.size()];
                    for (int i = 0; i < vector.size(); i++) {
                        result[i] = vector.get(i).floatValue();
                    }
                    results.add(result);
                }
            }

            return results;
        } catch (Exception e) {
            log.warn("批量Embedding失败，降级为逐个处理: {}", e.getMessage());
            // 降级为逐个处理
            for (String text : texts) {
                try {
                    results.add(embed(text));
                } catch (Exception ex) {
                    log.warn("单个文本嵌入失败，跳过");
                    results.add(new float[dimension]);
                }
            }
            return results;
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
