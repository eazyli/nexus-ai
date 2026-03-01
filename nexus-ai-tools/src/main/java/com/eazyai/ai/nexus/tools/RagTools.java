package com.eazyai.ai.nexus.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG工具类
 * 提供向量检索和知识库查询能力
 */
@Slf4j
@Component
public class RagTools {

    @Value("${ai.agent.rag.elasticsearch.host:localhost}")
    private String esHost;

    @Value("${ai.agent.rag.elasticsearch.port:9200}")
    private int esPort;

    @Value("${ai.agent.rag.index:knowledge_base}")
    private String indexName;

    @Value("${ai.agent.rag.top-k:5}")
    private int topK;

    private RestHighLevelClient esClient;

    @PostConstruct
    public void init() {
        try {
            esClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(esHost, esPort))
            );
            log.info("Elasticsearch客户端初始化成功: {}:{}", esHost, esPort);
        } catch (Exception e) {
            log.error("Elasticsearch客户端初始化失败", e);
        }
    }

    @PreDestroy
    public void close() {
        if (esClient != null) {
            try {
                esClient.close();
                log.info("Elasticsearch客户端已关闭");
            } catch (IOException e) {
                log.error("关闭Elasticsearch客户端失败", e);
            }
        }
    }

    /**
     * 从知识库检索相关信息
     * 
     * @param query 查询内容
     * @return 检索结果
     */
    @Tool(name = "query_knowledge", value = "从知识库中检索相关信息。当需要查询内部文档、FAQ、或特定领域知识时使用。")
    public String queryKnowledge(String query) {
        log.info("查询知识库: {}", query);
        long startTime = System.currentTimeMillis();
        
        if (esClient == null) {
            String error = "错误: Elasticsearch客户端未初始化";
            ToolExecutionContext.current().recordToolCallFailure(
                "query_knowledge", "知识库检索", query, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }

        try {
            // 构建搜索请求
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            // 多字段匹配查询
            sourceBuilder.query(QueryBuilders.multiMatchQuery(query, 
                    "title^3", "content", "keywords^2", "tags"));
            
            // 高亮显示
            HighlightBuilder highlightBuilder = new HighlightBuilder()
                    .field("content", 150, 3)
                    .preTags("<mark>")
                    .postTags("</mark>");
            sourceBuilder.highlighter(highlightBuilder);
            
            // 分页
            sourceBuilder.size(topK);

            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(sourceBuilder);

            // 执行搜索
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            
            String result = parseSearchResults(response);
            long execTime = System.currentTimeMillis() - startTime;
            
            // 记录工具调用
            ToolExecutionContext.current().recordToolCall(
                "query_knowledge", "知识库检索", query, result, execTime);
            
            return result;

        } catch (Exception e) {
            log.error("知识库查询失败", e);
            String error = "查询失败: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "query_knowledge", "知识库检索", query, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * 向量检索（需要向量模型支持）
     * 
     * @param query 查询内容
     * @return 相似度最高的文档
     */
    @Tool(name = "vector_search", value = "使用向量相似度检索相关文档。适合语义搜索和概念匹配。")
    public String vectorSearch(String query) {
        log.info("向量检索: {}", query);
        long startTime = System.currentTimeMillis();
        
        if (esClient == null) {
            String error = "错误: Elasticsearch客户端未初始化";
            ToolExecutionContext.current().recordToolCallFailure(
                "vector_search", "向量检索", query, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }

        try {
            // 这里假设已使用外部向量模型生成向量
            // 实际应用中需要调用Embedding模型
            float[] queryVector = generateQueryVector(query);
            
            if (queryVector == null) {
                String error = "错误: 无法生成查询向量";
                ToolExecutionContext.current().recordToolCallFailure(
                    "vector_search", "向量检索", query, error, 
                    System.currentTimeMillis() - startTime);
                return error;
            }

            // 构建kNN查询请求（ES 8.0+）
            JSONObject knnQuery = new JSONObject();
            knnQuery.put("field", "content_vector");
            knnQuery.put("query_vector", queryVector);
            knnQuery.put("k", topK);
            knnQuery.put("num_candidates", 100);

            JSONObject queryJson = new JSONObject();
            queryJson.put("knn", knnQuery);

            Request request = new Request("POST", "/" + indexName + "/_search");
            request.setJsonEntity(queryJson.toJSONString());

            org.elasticsearch.client.Response response = esClient.getLowLevelClient().performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            
            String result = parseVectorResults(JSON.parseObject(responseBody));
            long execTime = System.currentTimeMillis() - startTime;
            
            // 记录工具调用
            ToolExecutionContext.current().recordToolCall(
                "vector_search", "向量检索", query, result, execTime);
            
            return result;

        } catch (Exception e) {
            log.error("向量检索失败", e);
            String error = "向量检索失败: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "vector_search", "向量检索", query, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * 按类别检索
     * 
     * @param params 格式: "类别|查询内容"
     * @return 检索结果
     */
    @Tool(name = "query_by_category", value = "按类别检索知识库。参数格式: '类别|查询内容'")
    public String queryByCategory(String params) {
        long startTime = System.currentTimeMillis();
        
        String[] parts = params.split("\\|", 2);
        if (parts.length != 2) {
            String error = "参数格式错误，请使用: '类别|查询内容'";
            ToolExecutionContext.current().recordToolCallFailure(
                "query_by_category", "分类检索", params, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }

        String category = parts[0].trim();
        String query = parts[1].trim();

        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            // 布尔查询：必须匹配类别，同时匹配查询内容
            sourceBuilder.query(QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("category", category))
                    .must(QueryBuilders.multiMatchQuery(query, "title", "content", "keywords")));
            
            sourceBuilder.size(topK);

            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(sourceBuilder);

            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            String result = parseSearchResults(response);
            long execTime = System.currentTimeMillis() - startTime;
            
            // 记录工具调用
            ToolExecutionContext.current().recordToolCall(
                "query_by_category", "分类检索", params, result, execTime);
            
            return result;

        } catch (Exception e) {
            log.error("分类查询失败", e);
            String error = "查询失败: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "query_by_category", "分类检索", params, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * 解析搜索结果
     */
    protected String parseSearchResults(SearchResponse response) {
        SearchHit[] hits = response.getHits().getHits();
        
        if (hits.length == 0) {
            return "未找到相关信息";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("从知识库找到 ").append(hits.length).append(" 条相关信息:\n\n");

        for (int i = 0; i < hits.length; i++) {
            SearchHit hit = hits[i];
            JSONObject source = JSON.parseObject(hit.getSourceAsString());
            
            sb.append("[").append(i + 1).append("] ");
            sb.append(source.getString("title")).append("\n");
            sb.append("    来源: ").append(source.getString("source")).append("\n");
            sb.append("    相关性: ").append(String.format("%.2f", hit.getScore())).append("\n");
            
            // 获取高亮内容或原文
            String content;
            if (hit.getHighlightFields() != null && hit.getHighlightFields().containsKey("content")) {
                org.elasticsearch.common.text.Text[] fragments = hit.getHighlightFields().get("content").getFragments();
                content = Arrays.stream(fragments)
                        .map(org.elasticsearch.common.text.Text::toString)
                        .collect(Collectors.joining("..."));
            } else {
                content = source.getString("content");
                if (content != null && content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
            }
            sb.append("    内容: ").append(content).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 解析向量搜索结果
     */
    protected String parseVectorResults(JSONObject response) {
        JSONArray hits = response.getJSONObject("hits").getJSONArray("hits");
        
        if (hits.isEmpty()) {
            return "未找到相似文档";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("向量检索找到 ").append(hits.size()).append(" 条相似文档:\n\n");

        for (int i = 0; i < hits.size(); i++) {
            JSONObject hit = hits.getJSONObject(i);
            JSONObject source = hit.getJSONObject("_source");
            
            sb.append("[").append(i + 1).append("] ");
            sb.append(source.getString("title")).append("\n");
            sb.append("    相似度: ").append(String.format("%.4f", hit.getDouble("_score"))).append("\n");
            
            String content = source.getString("content");
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            sb.append("    内容: ").append(content).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 生成查询向量（简化示例）
     * 实际应用中应调用Embedding服务
     */
    protected float[] generateQueryVector(String query) {
        // TODO: 集成Embedding模型生成向量
        // 这里返回null，表示需要外部实现
        log.warn("向量生成需要集成Embedding模型");
        return null;
    }

    /**
     * 检查索引是否存在
     */
    public boolean indexExists() {
        try {
            return esClient.indices().exists(
                    new org.elasticsearch.client.indices.GetIndexRequest(indexName), 
                    RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("检查索引失败", e);
            return false;
        }
    }
}
