package com.eazyai.ai.nexus.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eazyai.ai.nexus.api.dto.ToolExecutionContext;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 搜索工具类
 * 提供网络搜索能力
 */
@Slf4j
@Component
public class SearchTools {

    @Value("${ai.agent.tools.search.api-key:}")
    private String searchApiKey;

    @Value("${ai.agent.tools.search.engine:bing}")
    private String searchEngine;

    @Value("${ai.agent.tools.search.limit:5}")
    private int searchLimit;

    /**
     * 执行网络搜索
     * 
     * @param query 搜索关键词
     * @return 搜索结果摘要
     */
    @Tool(name = "web_search", value = "执行网络搜索，获取实时信息。当需要最新数据、新闻、或不确定的信息时使用。")
    public String webSearch(String query) {
        log.info("执行网络搜索: {}", query);
        long startTime = System.currentTimeMillis();
        
        try {
            String result;
            if ("bing".equalsIgnoreCase(searchEngine)) {
                result = searchBing(query);
            } else {
                result = searchGeneric(query);
            }
            
            ToolExecutionContext.current().recordToolCall(
                "web_search", "网络搜索", query, result, 
                System.currentTimeMillis() - startTime);
            
            return result;
        } catch (Exception e) {
            log.error("搜索失败", e);
            String error = "搜索失败: " + e.getMessage();
            ToolExecutionContext.current().recordToolCallFailure(
                "web_search", "网络搜索", query, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
    }

    /**
     * Bing搜索实现
     */
    protected String searchBing(String query) throws Exception {
        if (searchApiKey == null || searchApiKey.isEmpty()) {
            return "未配置搜索API密钥";
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        String url = String.format("https://api.bing.microsoft.com/v7.0/search?q=%s&count=%d", 
                encodedQuery, searchLimit);

        HttpResponse response = HttpRequest.get(url)
                .header("Ocp-Apim-Subscription-Key", searchApiKey)
                .timeout(10000)
                .execute();

        if (response.getStatus() != 200) {
            return "搜索请求失败: HTTP " + response.getStatus();
        }

        JSONObject result = JSON.parseObject(response.body());
        return parseBingResults(result);
    }

    /**
     * 解析Bing搜索结果
     */
    protected String parseBingResults(JSONObject result) {
        StringBuilder sb = new StringBuilder();
        
        JSONArray webPages = result.getJSONObject("webPages").getJSONArray("value");
        if (webPages == null || webPages.isEmpty()) {
            return "未找到相关结果";
        }

        sb.append("搜索结果:\n\n");
        
        for (int i = 0; i < webPages.size() && i < searchLimit; i++) {
            JSONObject page = webPages.getJSONObject(i);
            sb.append(String.format("%d. %s\n   %s\n   链接: %s\n\n",
                    i + 1,
                    page.getString("name"),
                    page.getString("snippet"),
                    page.getString("url")));
        }

        return sb.toString();
    }

    /**
     * 通用搜索实现（模拟/备用）
     */
    protected String searchGeneric(String query) {
        // 这里可以实现其他搜索引擎或模拟搜索
        return String.format("搜索关键词: %s\n提示: 请配置Bing搜索API密钥以获得真实搜索结果", query);
    }

    /**
     * 搜索特定网站
     *
     * @return 搜索结果
     */
    @Tool(name = "site_search", value = "在特定网站内搜索内容。参数格式: '网站域名|搜索词'")
    public String siteSearch(String params) {
        long startTime = System.currentTimeMillis();
        
        String[] parts = params.split("\\|", 2);
        if (parts.length != 2) {
            String error = "参数格式错误，请使用: '网站域名|搜索词'";
            ToolExecutionContext.current().recordToolCallFailure(
                "site_search", "站内搜索", params, error, 
                System.currentTimeMillis() - startTime);
            return error;
        }
        
        String site = parts[0].trim();
        String query = parts[1].trim();
        String siteQuery = String.format("site:%s %s", site, query);
        
        return webSearch(siteQuery);
    }

    /**
     * 获取当前日期时间
     * 
     * @return 当前日期时间字符串
     */
    @Tool(name = "get_current_time", value = "获取当前系统日期和时间")
    public String getCurrentTime() {
        long startTime = System.currentTimeMillis();
        String result = java.time.LocalDateTime.now().toString();
        
        ToolExecutionContext.current().recordToolCall(
            "get_current_time", "获取时间", null, result, 
            System.currentTimeMillis() - startTime);
        
        return result;
    }
}
