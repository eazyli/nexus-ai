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
                .description("网络搜索插件，提供实时信息检索能力，用于获取最新的网络信息")
                .author("AI Agent Team")
                .capabilities(List.of("search", "retrieval", "information", "web"))
                .parameters(List.of(
                        PluginDescriptor.ParameterDef.builder()
                                .name("keywords")
                                .type("string")
                                .description("搜索关键词，建议使用简洁、精确的关键词")
                                .required(true)
                                .example("Spring AI最新版本特性")
                                .build(),
                        PluginDescriptor.ParameterDef.builder()
                                .name("limit")
                                .type("integer")
                                .description("返回结果数量限制")
                                .required(false)
                                .defaultValue(5)
                                .validation(Map.of("min", 1, "max", 20))
                                .example(3)
                                .build()
                ))
                // 触发条件
                .triggerConditions("""
                        当用户问题满足以下条件之一时，应调用此工具：
                        1. 需要获取最新的新闻、事件或实时信息
                        2. 问题涉及近期发生的动态或变化（如"最近"、"最新"）
                        3. 本地知识库或RAG检索无法找到相关信息
                        4. 用户明确要求搜索或联网查询
                        5. 需要验证某个信息的准确性或时效性
                        """)
                // 使用指导
                .guidance("""
                        最佳实践：
                        1. 关键词应简洁、精确，避免过长或过于复杂的查询语句
                        2. 建议将用户问题转化为2-5个核心关键词
                        3. 对于专业领域问题，建议添加领域限定词
                        4. limit建议设置为3-5个，避免信息过载
                        5. 搜索结果应进行筛选和总结，提取最相关的内容
                        6. 若搜索结果不理想，可尝试调整关键词重新搜索
                        """)
                // 使用示例
                .examples(List.of(
                        PluginDescriptor.UsageExample.builder()
                                .scenario("查询最新信息")
                                .userInput("2024年AI领域有哪些重要进展？")
                                .toolArguments(Map.of(
                                        "keywords", "2024 AI 人工智能 进展",
                                        "limit", 5
                                ))
                                .expectedOutput("返回2024年AI领域的重要新闻和发展")
                                .notes("包含时间限定词，确保搜索结果时效性")
                                .build(),
                        PluginDescriptor.UsageExample.builder()
                                .scenario("补充知识库信息")
                                .userInput("RAG技术最近有什么新论文？")
                                .toolArguments(Map.of(
                                        "keywords", "RAG 检索增强生成 论文 2024",
                                        "limit", 3
                                ))
                                .expectedOutput("返回RAG相关的最新学术研究")
                                .notes("当知识库信息可能过时时，使用网络搜索补充")
                                .build()
                ))
                // 工具协作
                .preRequisiteTools(List.of()) // 无前置工具
                .followUpTools(List.of("rag_retriever")) // 可能需要结合知识库进行对比
                // 错误处理
                .errorHandling("""
                        常见错误及处理：
                        1. 关键词为空：提示用户需要提供搜索内容
                        2. 无搜索结果：建议：
                           - 尝试简化关键词
                           - 使用更通用的搜索词
                           - 检查关键词拼写
                        3. 网络超时：建议稍后重试或使用其他信息源
                        4. 结果质量差：建议调整关键词重新搜索
                        """)
                // 执行特性
                .idempotent(true)
                .priority(15)
                .estimatedDuration(2000L)
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
