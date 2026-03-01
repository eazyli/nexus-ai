package com.eazyai.ai.nexus.infra.rag.embedding;

import com.eazyai.ai.nexus.infra.rag.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 本地Embedding服务（简化实现）
 * 
 * 支持BGE、M3E等本地模型，推荐使用方式：
 * 
 * 1. HTTP方式（推荐）：使用TEI/Infinity/FastGPT等部署本地Embedding服务
 *    配置 ai.agent.embedding.type=http
 * 
 * 2. DJL方式：需要添加以下依赖
 *    <dependency>
 *        <groupId>ai.djl.huggingface</groupId>
 *        <artifactId>tokenizers</artifactId>
 *        <version>0.26.0</version>
 *    </dependency>
 *    <dependency>
 *        <groupId>ai.djl.pytorch</groupId>
 *        <artifactId>pytorch-engine</artifactId>
 *        <version>0.26.0</version>
 *    </dependency>
 * 
 * 3. ONNX方式：需要添加ONNX Runtime依赖
 *    <dependency>
 *        <groupId>com.microsoft.onnxruntime</groupId>
 *        <artifactId>onnxruntime</artifactId>
 *        <version>1.16.0</version>
 *    </dependency>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.agent.embedding.type", havingValue = "local")
public class LocalEmbeddingService implements EmbeddingService {

    @Value("${ai.agent.embedding.model:bge-large-zh-v1.5}")
    private String modelId;

    @Value("${ai.agent.embedding.dimension:1024}")
    private int dimension;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        log.info("本地Embedding服务初始化: {}", modelId);
        log.info("当前为模拟模式，实际使用请配置 type=http 连接本地Embedding服务");
        initialized = true;
    }

    @Override
    public float[] embed(String text) {
        if (!initialized) {
            throw new IllegalStateException("Embedding模型未初始化");
        }
        
        // 模拟向量生成（实际使用应调用本地模型）
        // 推荐使用HTTP方式连接本地部署的TEI/Infinity服务
        return generateMockVector(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return modelId;
    }

    /**
     * 生成模拟向量（基于文本hash，保证相同文本生成相同向量）
     */
    private float[] generateMockVector(String text) {
        float[] vector = new float[dimension];
        Random random = new Random(text.hashCode());
        float sum = 0;
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) random.nextGaussian();
            sum += vector[i] * vector[i];
        }
        // L2归一化
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < dimension; i++) {
            vector[i] = vector[i] / norm;
        }
        return vector;
    }
}
