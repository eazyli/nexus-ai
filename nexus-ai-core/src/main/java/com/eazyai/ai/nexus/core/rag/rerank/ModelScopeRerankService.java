package com.eazyai.ai.nexus.core.rag.rerank;

import com.eazyai.ai.nexus.core.rag.RerankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ModelScope Rerank 服务
 * 
 * <p>使用 ModelScope 公网 API 进行文档重排序</p>
 */
@Slf4j
public class ModelScopeRerankService implements RerankService {

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final int topK;
    private final RestTemplate restTemplate = new RestTemplate();

    public ModelScopeRerankService(RerankProperties properties) {
        this.baseUrl = properties.getBaseUrl();
        this.apiKey = properties.getApiKey();
        this.modelName = properties.getModel();
        this.topK = properties.getTopK();
        log.info("[ModelScopeRerankService] 初始化完成, baseUrl: {}, model: {}", baseUrl, modelName);
    }

    @Override
    public List<RerankService.RerankResult> rerank(String query, List<RerankService.RerankDocument> documents) {
        return rerank(query, documents, topK);
    }

    @Override
    public List<RerankService.RerankResult> rerank(String query, List<RerankService.RerankDocument> documents, int topK) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        log.debug("[ModelScopeRerankService] 开始重排序, query: {}, 文档数: {}", query, documents.size());

        try {
            // 准备请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            List<String> docTexts = documents.stream()
                    .map(RerankService.RerankDocument::content)
                    .toList();

            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            body.put("documents", docTexts);
            body.put("model", modelName);
            body.put("top_n", Math.min(topK, documents.size()));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 调用 ModelScope Rerank API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/rerank", entity, Map.class);

            // 解析响应
            List<RerankService.RerankResult> results = new ArrayList<>();
            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.get("results");

                for (Map<String, Object> item : resultList) {
                    int index = ((Number) item.get("index")).intValue();
                    double score = ((Number) item.get("relevance_score")).doubleValue();

                    if (index >= 0 && index < documents.size()) {
                        RerankService.RerankDocument doc = documents.get(index);
                        results.add(new RerankService.RerankResult(
                                doc.id(),
                                doc.content(),
                                doc.initialScore(),
                                score,
                                index
                        ));
                    }
                }
            }

            // 按重排序分数降序排列
            results.sort(Comparator.comparingDouble(RerankService.RerankResult::rerankScore).reversed());

            // 限制返回数量
            if (topK > 0 && results.size() > topK) {
                results = new ArrayList<>(results.subList(0, topK));
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.debug("[ModelScopeRerankService] 重排序完成, 耗时: {}ms, 返回 {} 条结果", costTime, results.size());

            return results;

        } catch (Exception e) {
            log.error("[ModelScopeRerankService] 重排序失败: {}", e.getMessage(), e);
            throw new RuntimeException("ModelScope Rerank 调用失败", e);
        }
    }
}
