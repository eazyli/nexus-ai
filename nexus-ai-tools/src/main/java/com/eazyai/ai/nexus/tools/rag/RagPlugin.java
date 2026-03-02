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
                .description("RAG检索插件，基于向量检索提供相关知识，用于回答需要特定领域知识或文档内容的问题")
                .author("AI Agent Team")
                .capabilities(List.of("rag", "retrieval", "knowledge", "vector_search"))
                // 增强的参数定义
                .parameters(List.of(
                        PluginDescriptor.ParameterDef.builder()
                                .name("query")
                                .type("string")
                                .description("查询语句，描述需要检索的知识内容")
                                .required(true)
                                .example("如何配置Spring AI的向量存储？")
                                .build(),
                        PluginDescriptor.ParameterDef.builder()
                                .name("topK")
                                .type("integer")
                                .description("返回Top K个最相关的结果")
                                .required(false)
                                .defaultValue(5)
                                .validation(Map.of("min", 1, "max", 20))
                                .example(3)
                                .build()
                ))
                // 触发条件
                .triggerConditions("""
                        当用户问题满足以下条件之一时，应调用此工具：
                        1. 问题涉及特定领域知识、专业术语或技术细节
                        2. 问题需要查询文档、手册或知识库中的内容
                        3. 问题包含"如何"、"什么是"、"配置方法"等求知意图
                        4. 用户明确要求检索或搜索相关信息
                        5. 当前的对话上下文中缺少必要的背景知识
                        """)
                // 使用指导
                .guidance("""
                        最佳实践：
                        1. 查询语句应清晰、具体，避免过于宽泛的描述
                        2. 建议先用用户的原始问题进行检索，若无结果再尝试简化或改写
                        3. topK建议设置为3-5个，太多可能引入噪声，太少可能遗漏信息
                        4. 检索结果应与用户问题进行对比，选择最相关的段落作为回答依据
                        5. 如果检索结果相关性较低（score < 0.7），应告知用户可能存在更准确的信息来源
                        """)
                // 使用示例
                .examples(List.of(
                        PluginDescriptor.UsageExample.builder()
                                .scenario("查询技术配置方法")
                                .userInput("Spring Boot如何配置多个数据源？")
                                .toolArguments(Map.of(
                                        "query", "Spring Boot多数据源配置方法",
                                        "topK", 3
                                ))
                                .expectedOutput("返回包含Spring Boot多数据源配置教程的文档片段")
                                .notes("查询语句对原始问题进行了关键词提取")
                                .build(),
                        PluginDescriptor.UsageExample.builder()
                                .scenario("查找概念解释")
                                .userInput("什么是RAG技术？")
                                .toolArguments(Map.of(
                                        "query", "RAG检索增强生成技术原理"
                                ))
                                .expectedOutput("返回RAG技术的定义和原理说明")
                                .notes("当用户询问概念定义时，优先检索知识库")
                                .build()
                ))
                // 工具协作
                .preRequisiteTools(List.of()) // 无前置工具
                .followUpTools(List.of("web_search", "code_executor")) // 后续可能需要联网搜索或代码执行
                // 错误处理
                .errorHandling("""
                        常见错误及处理：
                        1. 查询为空：提示用户需要提供查询内容
                        2. 无检索结果：告知用户知识库中暂无相关信息，建议：
                           - 尝试不同的关键词
                           - 使用更通用的描述
                           - 使用web_search工具联网查询
                        3. 服务超时：建议稍后重试，或降级使用其他检索方式
                        """)
                // 执行特性
                .idempotent(true)
                .priority(10)
                .estimatedDuration(500L)
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
