package com.eazyai.ai.nexus.tools.rag;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.executor.ExecutionResult;
import com.eazyai.ai.nexus.api.plugin.Plugin;
import com.eazyai.ai.nexus.api.plugin.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RAG检索插件
 * 提供基于向量检索的增强生成能力（模拟实现）
 */
@Slf4j
@Component
public class RagPlugin implements Plugin {

    private final PluginDescriptor descriptor;

    public RagPlugin() {
        this.descriptor = PluginDescriptor.builder()
                .id("rag_retriever")
                .name("RAG Retriever Plugin")
                .version("1.0.0")
                .type("rag")
                .description("RAG检索插件，基于向量检索提供相关知识")
                .author("AI Agent Team")
                .capabilities(List.of("rag", "retrieval", "knowledge", "vector_search"))
                .parameters(List.of(
                        PluginDescriptor.ParameterDef.builder()
                                .name("query")
                                .type("string")
                                .description("查询语句")
                                .required(true)
                                .build(),
                        PluginDescriptor.ParameterDef.builder()
                                .name("topK")
                                .type("integer")
                                .description("返回Top K个结果")
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
        String query = (String) params.get("query");
        int topK = params.get("topK") != null ? ((Number) params.get("topK")).intValue() : 5;

        if (query == null || query.trim().isEmpty()) {
            return ExecutionResult.error("Query is required", null);
        }

        // 模拟RAG检索结果
        List<Map<String, Object>> documents = List.of(
                Map.of(
                        "id", "doc_001",
                        "content", "This is a retrieved document related to: " + query,
                        "metadata", Map.of("source", "knowledge_base", "page", 12),
                        "score", 0.92
                ),
                Map.of(
                        "id", "doc_002",
                        "content", "Another relevant document about " + query,
                        "metadata", Map.of("source", "knowledge_base", "page", 34),
                        "score", 0.85
                ),
                Map.of(
                        "id", "doc_003",
                        "content", "Additional context for " + query,
                        "metadata", Map.of("source", "wiki", "url", "https://wiki.example.com/page"),
                        "score", 0.78
                )
        );

        return ExecutionResult.success(Map.of(
                "query", query,
                "total", documents.size(),
                "documents", documents.subList(0, Math.min(topK, documents.size()))
        ));
    }

    @Override
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean supports(Map<String, Object> params) {
        return params.containsKey("query");
    }
}
