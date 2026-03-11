package com.eazyai.ai.nexus.core.rag.rerank;

import com.eazyai.ai.nexus.core.rag.RerankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 阿里云 DashScope Rerank 服务
 * 
 * <p>使用阿里云DashScope API进行文档重排序</p>
 * 
 * <p>API文档：https://help.aliyun.com/document_detail/2854995.html</p>
 */
@Slf4j
public class DashScopeRerankService implements RerankService {

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final int topK;
    private final RestTemplate restTemplate = new RestTemplate();

    public DashScopeRerankService(RerankProperties properties) {
        // 阿里云Rerank API固定端点
        this.baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";
        this.apiKey = properties.getApiKey();
        this.modelName = properties.getModel();
        this.topK = properties.getTopK();
        log.info("[DashScopeRerankService] 初始化完成, model: {}, topK: {}", modelName, topK);
    }

    @Override
    public List<RerankResult> rerank(String query, List<RerankDocument> documents) {
        return rerank(query, documents, topK);
    }

    @Override
    public List<RerankResult> rerank(String query, List<RerankDocument> documents, int topK) {
        // 添加详细日志追踪问题
        log.info("[DashScopeRerankService] rerank被调用, query长度: {}, documents数量: {}", 
                query != null ? query.length() : -1, documents != null ? documents.size() : -1);
        
        if (documents == null || documents.isEmpty()) {
            log.warn("[DashScopeRerankService] documents为null或空");
            return List.of();
        }

        // 验证参数有效性
        if (query == null || query.trim().isEmpty()) {
            log.error("[DashScopeRerankService] query为空或null，跳过rerank. query='{}'", query);
            return List.of();
        }
        
        // 检查documents中是否有空内容
        List<RerankDocument> validDocuments = documents.stream()
                .filter(doc -> {
                    if (doc == null) {
                        log.warn("[DashScopeRerankService] 发现null文档");
                        return false;
                    }
                    boolean valid = doc.content() != null && !doc.content().trim().isEmpty();
                    if (!valid) {
                        log.warn("[DashScopeRerankService] 发现空内容文档: id={}, content='{}'", 
                                doc.id(), doc.content());
                    }
                    return valid;
                })
                .toList();
        
        if (validDocuments.isEmpty()) {
            log.error("[DashScopeRerankService] 所有document内容为空，跳过rerank. 原始文档数: {}", documents.size());
            return List.of();
        }
        
        if (validDocuments.size() < documents.size()) {
            log.warn("[DashScopeRerankService] 过滤掉 {} 条空内容文档", documents.size() - validDocuments.size());
        }

        long startTime = System.currentTimeMillis();
        log.info("[DashScopeRerankService] 开始重排序, query长度: {}, 文档数: {}/{} (有效/总数)", 
                query.length(), validDocuments.size(), documents.size());

        try {
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建documents数组（字符串数组格式）
            List<String> docsList = new ArrayList<>();
            for (RerankDocument doc : validDocuments) {
                docsList.add(doc.content());
            }

            // 构建请求体
            Map<String, Object> input = new HashMap<>();
            input.put("query", query);  // query是字符串
            input.put("documents", docsList);  // documents是字符串数组

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("return_documents", true);
            parameters.put("top_n", Math.min(topK, documents.size()));

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("input", input);
            body.put("parameters", parameters);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 调用阿里云DashScope Rerank API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(baseUrl, entity, Map.class);

            // 解析响应
            List<RerankResult> results = new ArrayList<>();
            if (response != null && response.containsKey("output")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                
                if (output.containsKey("results")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> resultList = (List<Map<String, Object>>) output.get("results");

                    for (Map<String, Object> item : resultList) {
                        int index = ((Number) item.get("index")).intValue();
                        double relevanceScore = ((Number) item.get("relevance_score")).doubleValue();

                        if (index >= 0 && index < validDocuments.size()) {
                            RerankDocument doc = validDocuments.get(index);
                            results.add(new RerankResult(
                                    doc.id(),
                                    doc.content(),
                                    doc.initialScore(),
                                    relevanceScore,
                                    index
                            ));
                        }
                    }
                }
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.debug("[DashScopeRerankService] 重排序完成, 耗时: {}ms, 返回 {} 条结果", costTime, results.size());

            return results;

        } catch (Exception e) {
            log.error("[DashScopeRerankService] 重排序失败: {}", e.getMessage(), e);
            throw new RuntimeException("DashScope Rerank 调用失败", e);
        }
    }
}
