package com.eazyai.ai.nexus.infra.rag.store;

import com.eazyai.ai.nexus.infra.rag.TextChunk;
import com.eazyai.ai.nexus.infra.rag.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储（用于测试和无ES环境）
 */
@Slf4j
@Component
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, TextChunk> store = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> knowledgeIndex = new ConcurrentHashMap<>();

    @Override
    public void add(TextChunk chunk) {
        if (chunk.getId() == null) {
            chunk.setId(UUID.randomUUID().toString());
        }
        store.put(chunk.getId(), chunk);
        
        // 建立知识库索引
        if (chunk.getKnowledgeId() != null) {
            knowledgeIndex.computeIfAbsent(chunk.getKnowledgeId(), k -> ConcurrentHashMap.newKeySet())
                    .add(chunk.getId());
        }
        
        log.debug("添加切片: {} (知识库: {})", chunk.getId(), chunk.getKnowledgeId());
    }

    @Override
    public void addBatch(List<TextChunk> chunks) {
        for (TextChunk chunk : chunks) {
            add(chunk);
        }
        log.info("批量添加 {} 个切片", chunks.size());
    }

    @Override
    public List<TextChunk> similaritySearch(float[] query, int topK) {
        return similaritySearch(query, topK, null);
    }

    @Override
    public List<TextChunk> similaritySearch(float[] query, int topK, String knowledgeId) {
        List<TextChunk> candidates;
        
        if (knowledgeId != null) {
            Set<String> chunkIds = knowledgeIndex.get(knowledgeId);
            if (chunkIds == null || chunkIds.isEmpty()) {
                return List.of();
            }
            candidates = chunkIds.stream()
                    .map(store::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            candidates = new ArrayList<>(store.values());
        }

        // 计算余弦相似度并排序
        return candidates.stream()
                .filter(chunk -> chunk.getEmbedding() != null)
                .map(chunk -> {
                    chunk.setScore(cosineSimilarity(query, chunk.getEmbedding()));
                    return chunk;
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public List<TextChunk> keywordSearch(String query, int topK) {
        String[] keywords = query.toLowerCase().split("\\s+");
        
        return store.values().stream()
                .filter(chunk -> chunk.getContent() != null)
                .map(chunk -> {
                    String content = chunk.getContent().toLowerCase();
                    int matchCount = 0;
                    for (String keyword : keywords) {
                        if (content.contains(keyword)) {
                            matchCount++;
                        }
                    }
                    chunk.setScore((double) matchCount / keywords.length);
                    return chunk;
                })
                .filter(chunk -> chunk.getScore() > 0)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public List<TextChunk> hybridSearch(String query, float[] queryVector, int topK) {
        // 向量检索结果
        List<TextChunk> vectorResults = similaritySearch(queryVector, topK * 2);
        
        // 关键词检索结果
        List<TextChunk> keywordResults = keywordSearch(query, topK * 2);
        
        // 合并结果（RRF融合）
        Map<String, Double> scores = new HashMap<>();
        int rank = 1;
        
        for (TextChunk chunk : vectorResults) {
            scores.merge(chunk.getId(), 1.0 / (rank + 60), Double::sum);
            rank++;
        }
        
        rank = 1;
        for (TextChunk chunk : keywordResults) {
            scores.merge(chunk.getId(), 1.0 / (rank + 60), Double::sum);
            rank++;
        }
        
        // 按融合分数排序返回
        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> store.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        Set<String> chunkIds = knowledgeIndex.remove(knowledgeId);
        if (chunkIds != null) {
            for (String id : chunkIds) {
                store.remove(id);
            }
            log.info("删除知识库 {} 的 {} 个切片", knowledgeId, chunkIds.size());
        }
    }

    @Override
    public void delete(String chunkId) {
        TextChunk chunk = store.remove(chunkId);
        if (chunk != null && chunk.getKnowledgeId() != null) {
            Set<String> ids = knowledgeIndex.get(chunk.getKnowledgeId());
            if (ids != null) {
                ids.remove(chunkId);
            }
        }
    }

    @Override
    public boolean indexExists() {
        return true;
    }

    @Override
    public void createIndex() {
        log.info("内存向量存储初始化完成");
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0;
        }
        
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0 || normB == 0) {
            return 0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 获取存储的切片数量
     */
    public int size() {
        return store.size();
    }

    /**
     * 清空存储
     */
    public void clear() {
        store.clear();
        knowledgeIndex.clear();
    }
}
