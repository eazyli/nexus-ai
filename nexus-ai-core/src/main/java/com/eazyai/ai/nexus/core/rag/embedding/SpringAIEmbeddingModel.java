package com.eazyai.ai.nexus.core.rag.embedding;

import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI EmbeddingModel 适配器
 * 
 * <p>将 Spring AI 的 EmbeddingModel 适配到项目的 EmbeddingModel 接口</p>
 * 
 * <p>优势：</p>
 * <ul>
 *   <li>复用 Spring AI 的统一接口</li>
 *   <li>支持 Spring AI 提供的多种 Embedding 实现（OpenAI、Azure、本地模型等）</li>
 *   <li>自动处理批量请求优化</li>
 * </ul>
 * 
 * <p>使用示例：</p>
 * <pre>
 * &#064;Bean
 * public EmbeddingModel embeddingModel(SpringAIEmbeddingModel adapter) {
 *     return adapter;
 * }
 * </pre>
 */
@Slf4j
public class SpringAIEmbeddingModel implements EmbeddingModel {

    private final org.springframework.ai.embedding.EmbeddingModel delegate;
    private final String modelName;
    private int dimension = -1; // 缓存维度值，-1 表示未初始化

    public SpringAIEmbeddingModel(org.springframework.ai.embedding.EmbeddingModel delegate, String modelName) {
        this.delegate = delegate;
        this.modelName = modelName;
        log.info("[SpringAIEmbeddingModel] 初始化完成, model={}", modelName);
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("[SpringAIEmbeddingModel] 输入文本为空，返回零向量");
            return new float[dimension()];
        }

        try {
            log.debug("[SpringAIEmbeddingModel] 开始生成向量，文本长度: {}", text.length());
            
            // Spring AI 1.0.0-M3 中 embed(String text) 返回 float[]
            float[] result = delegate.embed(text);
            
            if (result == null || result.length == 0) {
                log.error("[SpringAIEmbeddingModel] delegate.embed() 返回 null 或空数组");
                throw new RuntimeException("Embedding 生成返回 null 或空数组");
            }
            
            log.debug("[SpringAIEmbeddingModel] 向量生成完成，维度: {}", result.length);
            
            return result;
            
        } catch (Exception e) {
            log.error("[SpringAIEmbeddingModel] 生成向量失败，文本长度: {}, 错误: {}", 
                     text != null ? text.length() : 0, e.getMessage(), e);
            throw new RuntimeException("Embedding 生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.debug("[SpringAIEmbeddingModel] 输入文本列表为空，返回空列表");
            return List.of();
        }

        // 过滤空文本
        List<String> validTexts = texts.stream()
                .filter(text -> text != null && !text.trim().isEmpty())
                .toList();
                
        if (validTexts.isEmpty()) {
            log.warn("[SpringAIEmbeddingModel] 所有输入文本都为空，返回空列表");
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        log.debug("[SpringAIEmbeddingModel] 开始批量生成向量, 原始文本数: {}, 有效文本数: {}", 
                 texts.size(), validTexts.size());

        try {
            // Spring AI 的 embedForResponse 方法支持批量请求
            EmbeddingResponse response = delegate.embedForResponse(validTexts);
            
            if (response == null) {
                log.error("[SpringAIEmbeddingModel] delegate.embedForResponse() 返回 null");
                throw new RuntimeException("批量 Embedding 生成返回 null");
            }
            
            var results = response.getResults();
            if (results == null || results.isEmpty()) {
                log.error("[SpringAIEmbeddingModel] response.getResults() 返回 null 或空列表");
                throw new RuntimeException("批量 Embedding 结果为空");
            }
            
            List<float[]> embeddings = results.stream()
                    .map(result -> {
                        if (result == null) {
                            log.warn("[SpringAIEmbeddingModel] 单个 Embedding 结果为 null，使用零向量代替");
                            return new float[dimension()];
                        }
                        // Spring AI 1.0.0-M3 中 result.getOutput() 返回 float[]
                        float[] output = result.getOutput();
                        if (output == null || output.length == 0) {
                            log.warn("[SpringAIEmbeddingModel] 单个 Embedding 输出为空，使用零向量代替");
                            return new float[dimension()];
                        }
                        return output;
                    })
                    .collect(Collectors.toList());

            long costTime = System.currentTimeMillis() - startTime;
            log.debug("[SpringAIEmbeddingModel] 批量向量生成完成, 耗时: {}ms, 返回 {} 个向量", 
                    costTime, embeddings.size());

            return embeddings;

        } catch (Exception e) {
            log.error("[SpringAIEmbeddingModel] 批量生成向量失败, 文本数量: {}, 错误: {}", 
                     validTexts.size(), e.getMessage(), e);
            throw new RuntimeException("批量 Embedding 生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int dimension() {
        // 缓存维度值，避免重复计算
        if (dimension > 0) {
            return dimension;
        }
        
        // 从第一个向量推断维度
        try {
            log.debug("[SpringAIEmbeddingModel] 尝试推断向量维度...");
            float[] sample = embed("test");
            dimension = sample.length;
            log.info("[SpringAIEmbeddingModel] 向量维度推断完成: {}", dimension);
            return dimension;
        } catch (Exception e) {
            log.warn("[SpringAIEmbeddingModel] 无法推断维度，使用默认值 1536: {}", e.getMessage());
            dimension = 1536; // OpenAI text-embedding-ada-002 的维度
            return dimension;
        }
    }

    @Override
    public String modelName() {
        return modelName;
    }
}
