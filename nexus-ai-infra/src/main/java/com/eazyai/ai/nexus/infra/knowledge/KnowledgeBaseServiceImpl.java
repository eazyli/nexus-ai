package com.eazyai.ai.nexus.infra.knowledge;

import com.eazyai.ai.nexus.core.rag.EmbeddingModel;
import com.eazyai.ai.nexus.core.rag.VectorStore;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeBaseService;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeProcessService;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 * 
 * <p>实现 api 层定义的 KnowledgeBaseService 接口</p>
 * <p>负责知识库管理、文档处理、检索的业务逻辑</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeProcessService knowledgeProcessService;
    private final VectorStore vectorStore;


    // ==================== 知识库管理 ====================

    @Override
    public String createKnowledge(String knowledgeName, String appId, Map<String, Object> config) {
        String knowledgeId = UUID.randomUUID().toString().replace("-", "");
        
        String embeddingModelId = config != null ? (String) config.get("embeddingModel") : null;
        Integer chunkSize = config != null ? (Integer) config.get("chunkSize") : 500;
        Integer chunkOverlap = config != null ? (Integer) config.get("chunkOverlap") : 50;
        
        KnowledgeRepository.KnowledgeInfo info = new KnowledgeRepository.KnowledgeInfo(
                knowledgeId,
                knowledgeName,
                "document",
                appId,
                null,
                embeddingModelId,
                chunkSize,
                chunkOverlap,
                0,  // status: 处理中
                0,  // progress
                0,  // docCount
                0,  // chunkCount
                config != null ? config : new HashMap<>()
        );
        
        knowledgeRepository.saveKnowledge(info);
        log.info("[KnowledgeBaseService] 创建知识库: {} -> {}", knowledgeName, knowledgeId);
        
        return knowledgeId;
    }

    @Override
    public KnowledgeBaseInfo getKnowledge(String knowledgeId) {
        KnowledgeRepository.KnowledgeInfo info = knowledgeRepository.findKnowledgeById(knowledgeId);
        return info != null ? toKnowledgeBaseInfo(info) : null;
    }

    @Override
    public List<KnowledgeBaseInfo> listByAppId(String appId) {
        List<KnowledgeRepository.KnowledgeInfo> infos = knowledgeRepository.findKnowledgeByAppId(appId);
        return infos.stream()
                .map(this::toKnowledgeBaseInfo)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteKnowledge(String knowledgeId) {
        // 删除向量存储中的数据
        vectorStore.deleteByKnowledgeId(knowledgeId);
        
        // 删除数据库记录
        knowledgeRepository.deleteChunksByKnowledgeId(knowledgeId);
        knowledgeRepository.deleteKnowledgeById(knowledgeId);
        
        log.info("[KnowledgeBaseService] 删除知识库: {}", knowledgeId);
    }

    @Override
    public void updateStatus(String knowledgeId, int status, int progress) {
        knowledgeRepository.updateStatus(knowledgeId, status, progress);
    }

    // ==================== 文档管理 ====================

    @Override
    public Long addDocument(String knowledgeId, byte[] content, String fileName) {
        // 创建文档记录
        KnowledgeRepository.DocInfo docInfo = new KnowledgeRepository.DocInfo(
                null, knowledgeId, fileName, (long) content.length,
                getFileType(fileName), 0, null, null
        );
        docInfo = knowledgeRepository.saveDocument(docInfo);
        
        // 异步处理文档
        try {
            KnowledgeProcessService.ProcessResult result = 
                    knowledgeProcessService.processDocument(content, fileName, knowledgeId, docInfo.id());
            
            if (result.success()) {
                knowledgeRepository.updateDocumentStatus(docInfo.id(), 1, result.chunkCount(), null);
                knowledgeRepository.incrementDocCount(knowledgeId, 1);
                knowledgeRepository.incrementChunkCount(knowledgeId, result.chunkCount());
            } else {
                knowledgeRepository.updateDocumentStatus(docInfo.id(), 2, null, result.error());
            }
        } catch (Exception e) {
            log.error("[KnowledgeBaseService] 处理文档失败: {}", fileName, e);
            knowledgeRepository.updateDocumentStatus(docInfo.id(), 2, null, e.getMessage());
        }
        
        return docInfo.id();
    }

    @Override
    public int addText(String knowledgeId, String text, Map<String, Object> metadata) {
        int chunkCount = knowledgeProcessService.processText(text, knowledgeId, metadata);
        knowledgeRepository.incrementChunkCount(knowledgeId, chunkCount);
        return chunkCount;
    }

    @Override
    public void deleteDocument(String knowledgeId, Long docId) {
        // 1. 查询该文档的所有chunkId（用于删除ES数据）
        List<String> chunkIds = knowledgeRepository.findChunkIdsByDocId(docId);
        log.debug("[KnowledgeBaseService] 查询到文档 {} 的 {} 个切片ID", docId, chunkIds.size());
        
        // 2. 删除ES中的向量数据
        if (chunkIds != null && !chunkIds.isEmpty()) {
            for (String chunkId : chunkIds) {
                try {
                    vectorStore.delete(chunkId);
                    log.debug("[KnowledgeBaseService] 删除ES切片: {}", chunkId);
                } catch (Exception e) {
                    log.warn("[KnowledgeBaseService] 删除ES切片失败: {}, 错误: {}", chunkId, e.getMessage());
                }
            }
        }
        
        // 3. 删除数据库中的记录
        int chunkCount = knowledgeRepository.deleteChunksByDocId(docId);
        knowledgeRepository.deleteDocumentById(docId);
        knowledgeRepository.incrementChunkCount(knowledgeId, -chunkCount);
        knowledgeRepository.incrementDocCount(knowledgeId, -1);
        
        log.info("[KnowledgeBaseService] 删除文档: {} -> 切片数: {}, ES删除: {}", 
                docId, chunkCount, chunkIds.size());
    }

    // ==================== 检索 ====================

    @Override
    public RetrievalResult retrieve(String query, List<String> knowledgeIds, int topK) {
        KnowledgeProcessService.RetrievalResult result = 
                knowledgeProcessService.retrieve(query, knowledgeIds, topK, true, true);
        
        List<ChunkInfo> chunks = result.chunks().stream()
                .map(chunk -> new ChunkInfo(
                        chunk.getId(),
                        chunk.getContent(),
                        chunk.getScore(),
                        chunk.getKnowledgeId(),
                        chunk.getMetadata()
                ))
                .collect(Collectors.toList());
        
        return new RetrievalResult(query, chunks, result.totalTime(), result.searchType());
    }

    @Override
    public RetrievalResult retrieveByApp(String query, String appId, int topK) {
        List<KnowledgeBaseInfo> knowledgeBases = listByAppId(appId);
        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            return new RetrievalResult(query, List.of(), 0, "none");
        }
        
        List<String> knowledgeIds = knowledgeBases.stream()
                .map(KnowledgeBaseInfo::knowledgeId)
                .collect(Collectors.toList());
        
        return retrieve(query, knowledgeIds, topK);
    }

    // ==================== 私有方法 ====================

    private KnowledgeBaseInfo toKnowledgeBaseInfo(KnowledgeRepository.KnowledgeInfo info) {
        return new KnowledgeBaseInfo(
                info.knowledgeId(),
                info.knowledgeName(),
                info.knowledgeType(),
                info.appId(),
                info.status(),
                info.docCount(),
                info.chunkCount()
        );
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "unknown";
    }
}
