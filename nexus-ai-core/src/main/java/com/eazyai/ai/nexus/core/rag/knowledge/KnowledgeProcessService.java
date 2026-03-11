package com.eazyai.ai.nexus.core.rag.knowledge;

import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import com.eazyai.ai.nexus.core.rag.DocumentSplitter;
import com.eazyai.ai.nexus.core.rag.RerankService;
import com.eazyai.ai.nexus.core.rag.VectorStore;
import com.eazyai.ai.nexus.core.rag.model.TextChunk;
import com.eazyai.ai.nexus.core.rag.parser.DocumentParserFactory;
import com.eazyai.ai.nexus.core.rag.parser.ParsedSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库处理服务
 * 
 * <p>负责知识库的核心业务逻辑：
 * <ul>
 *   <li>文档解析与切片</li>
 *   <li>向量化流程编排</li>
 *   <li>混合检索与重排序</li>
 * </ul>
 * 
 * <p>数据访问通过 KnowledgeRepository 接口委托给 infra 层</p>
 */
@Slf4j
@RequiredArgsConstructor
public class KnowledgeProcessService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter documentSplitter;
    private final DocumentParserFactory parserFactory;
    private final RerankService rerankService;
    private final KnowledgeRepository knowledgeRepository;
    private final int embeddingBatchSize;

    // ==================== 文档处理 ====================

    /**
     * 处理文档：解析 -> 切片 -> 向量化 -> 存储
     * 
     * @param content 文档内容
     * @param fileName 文件名
     * @param knowledgeId 知识库ID
     * @param docId 文档ID（用于关联ai_knowledge_doc表）
     * @return 处理结果
     */
    public ProcessResult processDocument(byte[] content, String fileName, String knowledgeId, Long docId) {
        try {
            // 1. 解析文档
            List<ParsedSection> sections = parserFactory.parseSections(content, fileName);
            log.info("[KnowledgeProcessService] 解析文档: {} -> {} 个段落", fileName, sections.size());

            // 2. 切片
            List<TextChunk> allChunks = new ArrayList<>();
            int chunkIndex = 0;

            for (ParsedSection section : sections) {
                List<TextChunk> sectionChunks = documentSplitter.split(section.getContent());

                for (TextChunk chunk : sectionChunks) {
                    chunk.setId(UUID.randomUUID().toString());
                    chunk.setKnowledgeId(knowledgeId);
                    if (docId != null) {
                        chunk.setDocumentId(docId.toString());
                    }
                    chunk.setChunkIndex(chunkIndex++);
                    if (section.getMetadata() != null) {
                        chunk.getMetadata().putAll(section.getMetadata());
                    }
                }
                allChunks.addAll(sectionChunks);
            }

            // 3. 向量化并存储（保存到ES和数据库）
            indexChunks(allChunks, docId);

            log.info("[KnowledgeProcessService] 文档处理完成: {} -> {} 个切片", fileName, allChunks.size());
            return new ProcessResult(true, allChunks.size(), null);

        } catch (Exception e) {
            log.error("[KnowledgeProcessService] 文档处理失败: {}", fileName, e);
            return new ProcessResult(false, 0, e.getMessage());
        }
    }

    /**
     * 处理文本：切片 -> 向量化 -> 存储
     * 
     * @param text 文本内容
     * @param knowledgeId 知识库ID
     * @param metadata 元数据
     * @return 切片数量
     */
    public int processText(String text, String knowledgeId, Map<String, Object> metadata) {
        List<TextChunk> chunks = documentSplitter.split(text);

        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            chunk.setId(UUID.randomUUID().toString());
            chunk.setKnowledgeId(knowledgeId);
            chunk.setChunkIndex(i);
            if (metadata != null) {
                chunk.getMetadata().putAll(metadata);
            }
        }

        // 保存到ES和数据库（docId为null，因为是直接添加文本，不是文档上传）
        indexChunks(chunks, null);
        return chunks.size();
    }

    /**
     * 索引切片：生成向量 -> 存储到 ES 和数据库
     * 
     * <p>批量调用 Embedding API，提升性能</p>
     * <p>同时保存到向量数据库(Elasticsearch)和关系型数据库(ai_knowledge_chunk表)</p>
     */
    public void indexChunks(List<TextChunk> chunks) {
        indexChunks(chunks, null);
    }
    
    /**
     * 索引切片：生成向量 -> 存储到 ES 和数据库（带文档ID）
     * 
     * @param chunks 切片列表
     * @param docId 文档ID（可选，用于关联ai_knowledge_doc表）
     */
    public void indexChunks(List<TextChunk> chunks, Long docId) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("[KnowledgeProcessService] chunks为空，跳过索引");
            return;
        }

        // 过滤掉内容为空的chunk
        List<TextChunk> validChunks = chunks.stream()
                .filter(chunk -> chunk.getContent() != null && !chunk.getContent().trim().isEmpty())
                .collect(Collectors.toList());
        
        if (validChunks.isEmpty()) {
            log.warn("[KnowledgeProcessService] 没有有效的chunk需要索引（所有内容都为空）");
            return;
        }
        
        if (validChunks.size() < chunks.size()) {
            log.warn("[KnowledgeProcessService] 过滤掉 {} 个空内容chunk", chunks.size() - validChunks.size());
        }

        int batchSize = embeddingBatchSize > 0 ? embeddingBatchSize : 20; // 使用配置值，默认 20
        int totalChunks = validChunks.size();

        // 保存到数据库（记录chunkId，用于后续删除ES数据）
        if (knowledgeRepository != null) {
            try {
                knowledgeRepository.saveChunks(validChunks, docId);
                log.info("[KnowledgeProcessService] 保存 {} 个切片到数据库(ai_knowledge_chunk表)", validChunks.size());
            } catch (Exception e) {
                log.error("[KnowledgeProcessService] 保存切片到数据库失败", e);
                // 继续执行，不影响向量存储
            }
        }

        // 分批生成向量和保存到向量存储
        for (int i = 0; i < totalChunks; i += batchSize) {
            int end = Math.min(i + batchSize, totalChunks);
            List<TextChunk> batch = validChunks.subList(i, end);

            // 批量生成向量
            List<String> texts = batch.stream()
                    .map(TextChunk::getContent)
                    .toList();
            List<float[]> embeddings = embeddingModel.embedBatch(texts);

            for (int j = 0; j < batch.size(); j++) {
                batch.get(j).setEmbedding(embeddings.get(j));
            }

            // 批量存储向量到Elasticsearch
            vectorStore.addBatch(batch);

            log.debug("[KnowledgeProcessService] 索引进度: {}/{}", end, totalChunks);
        }
        
        log.info("[KnowledgeProcessService] 索引完成: {} 个切片, 批量大小: {}, 已保存到DB和ES", 
                totalChunks, batchSize);
    }

    // ==================== 检索 ====================

    /**
     * 检索知识库
     */
    public RetrievalResult retrieve(String query, List<String> knowledgeIds, int topK, 
                                     boolean useHybridSearch, boolean useRerank) {
        long startTime = System.currentTimeMillis();

        List<TextChunk> allChunks;
        int candidateSize = useRerank && rerankService != null ? Math.max(topK * 3, 20) : topK * 2;

        // 生成查询向量
        float[] queryVector = embeddingModel.embed(query);

        if (useHybridSearch) {
            // 混合检索
            allChunks = vectorStore.hybridSearch(query, queryVector, candidateSize);
        } else {
            // 向量检索
            allChunks = vectorStore.similaritySearch(queryVector, candidateSize);
        }

        // 过滤知识库
        List<TextChunk> filteredChunks;
        if (knowledgeIds != null && !knowledgeIds.isEmpty()) {
            filteredChunks = allChunks.stream()
                    .filter(chunk -> knowledgeIds.contains(chunk.getKnowledgeId()))
                    .collect(Collectors.toList());
        } else {
            filteredChunks = allChunks;
        }

        // 重排序
        List<TextChunk> rankedChunks;
        if (useRerank && rerankService != null && !filteredChunks.isEmpty()) {
            rankedChunks = rerankChunks(query, filteredChunks, topK);
        } else {
            rankedChunks = filteredChunks;
        }

        // 限制数量
        List<TextChunk> resultChunks = rankedChunks.stream()
                .limit(topK)
                .toList();

        return new RetrievalResult(
                query,
                resultChunks,
                System.currentTimeMillis() - startTime,
                useHybridSearch ? "hybrid" : "vector"
        );
    }

    /**
     * 使用 Rerank 模型重排序
     */
    private List<TextChunk> rerankChunks(String query, List<TextChunk> chunks, int topK) {
        long startTime = System.currentTimeMillis();

        List<RerankService.RerankDocument> documents = new ArrayList<>();
        for (TextChunk chunk : chunks) {
            documents.add(new RerankService.RerankDocument(
                    chunk.getId(),
                    chunk.getContent(),
                    (float) chunk.getScore()
            ));
        }

        List<RerankService.RerankResult> results = rerankService.rerank(query, documents, topK);

        List<TextChunk> rankedChunks = new ArrayList<>();
        for (RerankService.RerankResult result : results) {
            TextChunk original = chunks.get(result.originalIndex());
            TextChunk ranked = new TextChunk();
            ranked.setId(original.getId());
            ranked.setDocumentId(original.getDocumentId());
            ranked.setKnowledgeId(original.getKnowledgeId());
            ranked.setContent(original.getContent());
            ranked.setEmbedding(original.getEmbedding());
            ranked.setStartPosition(original.getStartPosition());
            ranked.setEndPosition(original.getEndPosition());
            ranked.setChunkIndex(original.getChunkIndex());
            ranked.setMetadata(original.getMetadata());
            ranked.setScore(result.rerankScore());
            rankedChunks.add(ranked);
        }

        log.debug("[KnowledgeProcessService] Rerank 完成, 耗时: {}ms, 输入: {} 条, 输出: {} 条",
                System.currentTimeMillis() - startTime, chunks.size(), rankedChunks.size());

        return rankedChunks;
    }

    // ==================== 数据模型 ====================

    public record ProcessResult(boolean success, int chunkCount, String error) {}

    public record RetrievalResult(
            String query,
            List<TextChunk> chunks,
            long totalTime,
            String searchType
    ) {
        public String getContext() {
            if (chunks == null || chunks.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                if (i > 0) {
                    sb.append("\n\n");
                }
                sb.append(chunks.get(i).getContent());
            }
            return sb.toString();
        }
    }
}
