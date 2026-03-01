package com.eazyai.ai.nexus.tools.search;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.executor.ExecutionResult;
import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 搜索插件
 * 提供搜索能力（模拟实现，实际可对接搜索引擎API）
 */
@Slf4j
@Component
public class SearchPlugin implements Plugin {

    private final PluginDescriptor descriptor;

    public SearchPlugin() {
        this.descriptor = PluginDescriptor.builder()
                .id("search")
                .name("Search Plugin")
                .version("1.0.0")
                .type("search")
                .description("搜索插件，提供信息检索能力")
                .author("AI Agent Team")
                .capabilities(List.of("search", "retrieval", "information"))
                .parameters(List.of(
                        PluginDescriptor.ParameterDef.builder()
                                .name("keywords")
                                .type("string")
                                .description("搜索关键词")
                                .required(true)
                                .build(),
                        PluginDescriptor.ParameterDef.builder()
                                .name("limit")
                                .type("integer")
                                .description("返回结果数量")
                                .required(false)
                                .defaultValue(5)
                                .build()
                ))
                .config(Map.of())
                .enabled(true)
                .build();
    }

    @Override
    public ExecutionResult execute(Map<String, Object> params, AgentContext context) {
        String keywords = (String) params.get("keywords");
        int limit = params.get("limit") != null ? ((Number) params.get("limit")).intValue() : 5;

        if (keywords == null || keywords.trim().isEmpty()) {
            return ExecutionResult.error("Keywords are required", null);
        }

        // 模拟搜索结果
        List<Map<String, Object>> results = List.of(
                Map.of(
                        "title", "Result 1 for: " + keywords,
                        "content", "This is a simulated search result about " + keywords,
                        "url", "https://example.com/result1",
                        "score", 0.95
                ),
                Map.of(
                        "title", "Result 2 for: " + keywords,
                        "content", "Another simulated result related to " + keywords,
                        "url", "https://example.com/result2",
                        "score", 0.87
                ),
                Map.of(
                        "title", "Result 3 for: " + keywords,
                        "content", "More information about " + keywords,
                        "url", "https://example.com/result3",
                        "score", 0.72
                )
        );

        return ExecutionResult.success(Map.of(
                "keywords", keywords,
                "total", results.size(),
                "results", results.subList(0, Math.min(limit, results.size()))
        ));
    }

    @Override
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean supports(Map<String, Object> params) {
        return params.containsKey("keywords");
    }
}
