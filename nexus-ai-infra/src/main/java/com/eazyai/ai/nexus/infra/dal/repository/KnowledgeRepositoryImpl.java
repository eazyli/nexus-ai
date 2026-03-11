package com.eazyai.ai.nexus.infra.dal.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeRepository;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeRepository.KnowledgeInfo;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeRepository.DocInfo;
import com.eazyai.ai.nexus.core.rag.model.TextChunk;
import com.eazyai.ai.nexus.infra.dal.entity.AiKnowledgeDoc;
import com.eazyai.ai.nexus.infra.dal.entity.KnowledgeChunkEntity;
import com.eazyai.ai.nexus.infra.dal.entity.KnowledgeEntity;
import com.eazyai.ai.nexus.infra.dal.mapper.AiKnowledgeDocMapper;
import com.eazyai.ai.nexus.infra.dal.mapper.KnowledgeChunkMapper;
import com.eazyai.ai.nexus.infra.dal.mapper.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库数据访问实现
 * 
 * <p>负责知识库相关的数据库操作</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KnowledgeRepositoryImpl implements KnowledgeRepository {

    private final KnowledgeMapper knowledgeMapper;
    private final AiKnowledgeDocMapper docMapper;
    private final KnowledgeChunkMapper chunkMapper;

    // ==================== 知识库管理 ====================

    @Override
    public KnowledgeInfo saveKnowledge(KnowledgeInfo info) {
        KnowledgeEntity dbEntity = toDbEntity(info);
        knowledgeMapper.insert(dbEntity);
        return info;
    }

    @Override
    public KnowledgeInfo findKnowledgeById(String knowledgeId) {
        KnowledgeEntity entity = knowledgeMapper.selectById(knowledgeId);
        return entity != null ? toKnowledgeInfo(entity) : null;
    }

    @Override
    public List<KnowledgeInfo> findKnowledgeByAppId(String appId) {
        List<KnowledgeEntity> entities = knowledgeMapper.findByAppId(appId);
        return entities.stream()
                .map(this::toKnowledgeInfo)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteKnowledgeById(String knowledgeId) {
        knowledgeMapper.deleteById(knowledgeId);
        log.debug("[KnowledgeRepository] 删除知识库: {}", knowledgeId);
    }

    @Override
    public void updateStatus(String knowledgeId, int status, int progress) {
        knowledgeMapper.updateStatus(knowledgeId, status, progress);
    }

    @Override
    public void incrementChunkCount(String knowledgeId, int increment) {
        knowledgeMapper.incrementChunkCount(knowledgeId, increment);
    }

    @Override
    public void incrementDocCount(String knowledgeId, int increment) {
        knowledgeMapper.incrementChunkCount(knowledgeId, increment);
    }

    // ==================== 文档管理 ====================

    @Override
    public DocInfo saveDocument(DocInfo info) {
        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setKnowledgeId(info.knowledgeId());
        doc.setFileName(info.fileName());
        doc.setFileSize(info.fileSize());
        doc.setFileType(info.fileType());
        doc.setStatus(info.status());
        doc.setCreateTime(LocalDateTime.now());
        docMapper.insert(doc);
        return new DocInfo(doc.getId(), doc.getKnowledgeId(), doc.getFileName(),
                doc.getFileSize(), doc.getFileType(), doc.getStatus(), null, null);
    }

    @Override
    public void updateDocumentStatus(Long docId, int status, Integer chunkCount, String errorMsg) {
        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setId(docId);
        doc.setStatus(status);
        if (chunkCount != null) {
            doc.setChunkCount(chunkCount);
        }
        if (errorMsg != null) {
            doc.setErrorMsg(errorMsg);
        }
        doc.setUpdateTime(LocalDateTime.now());
        docMapper.updateById(doc);
    }

    @Override
    public void deleteDocumentById(Long docId) {
        docMapper.deleteById(docId);
        log.debug("[KnowledgeRepository] 删除文档: {}", docId);
    }

    @Override
    public List<DocInfo> findDocumentsByKnowledgeId(String knowledgeId) {
        LambdaQueryWrapper<AiKnowledgeDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeDoc::getKnowledgeId, knowledgeId);
        List<AiKnowledgeDoc> docs = docMapper.selectList(wrapper);
        return docs.stream()
                .map(this::toDocInfo)
                .collect(Collectors.toList());
    }

    // ==================== 切片管理 ====================

    @Override
    public void saveChunks(List<TextChunk> chunks, Long docId) {
        for (TextChunk chunk : chunks) {
            KnowledgeChunkEntity entity = KnowledgeChunkEntity.builder()
                    .chunkId(chunk.getId())
                    .knowledgeId(chunk.getKnowledgeId())
                    .docId(docId)
                    .content(chunk.getContent())
                    .chunkIndex(chunk.getChunkIndex())
                    .startOffset(chunk.getStartPosition())
                    .endOffset(chunk.getEndPosition())
                    .metadata(chunk.getMetadata())
                    .vectorId(chunk.getId())
                    .createTime(LocalDateTime.now())
                    .build();
            chunkMapper.insert(entity);
        }
    }

    @Override
    public List<TextChunk> findChunksByKnowledgeId(String knowledgeId) {
        List<KnowledgeChunkEntity> entities = chunkMapper.findByKnowledgeId(knowledgeId);
        return entities.stream()
                .map(this::toTextChunk)
                .collect(Collectors.toList());
    }

    @Override
    public List<TextChunk> findChunksByDocId(Long docId) {
        List<KnowledgeChunkEntity> entities = chunkMapper.findByDocId(docId);
        return entities.stream()
                .map(this::toTextChunk)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteChunksByKnowledgeId(String knowledgeId) {
        int count = chunkMapper.deleteByKnowledgeId(knowledgeId);
        log.debug("[KnowledgeRepository] 删除知识库切片: {} -> {} 条", knowledgeId, count);
    }

    @Override
    public int deleteChunksByDocId(Long docId) {
        return chunkMapper.deleteByDocId(docId);
    }

    @Override
    public List<String> findChunkIdsByDocId(Long docId) {
        return chunkMapper.findByDocId(docId).stream()
                .map(KnowledgeChunkEntity::getChunkId)
                .collect(Collectors.toList());
    }

    // ==================== 转换方法 ====================

    private KnowledgeInfo toKnowledgeInfo(KnowledgeEntity entity) {
        Object vectorConfig = entity.getVectorDbConfig();
        Map<String, Object> vectorDbConfigMap = null;
        if (vectorConfig instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) vectorConfig;
            vectorDbConfigMap = map;
        }
        
        return new KnowledgeInfo(
                entity.getKnowledgeId(),
                entity.getKnowledgeName(),
                entity.getKnowledgeType(),
                entity.getAppId(),
                entity.getDescription(),
                entity.getEmbeddingModel(),
                entity.getChunkSize(),
                entity.getChunkOverlap(),
                entity.getStatus(),
                entity.getProcessProgress(),
                entity.getDocCount(),
                entity.getChunkCount(),
                vectorDbConfigMap != null ? vectorDbConfigMap : Map.of()
        );
    }

    private KnowledgeEntity toDbEntity(KnowledgeInfo info) {
        KnowledgeEntity dbEntity = new KnowledgeEntity();
        dbEntity.setKnowledgeId(info.knowledgeId());
        dbEntity.setKnowledgeName(info.knowledgeName());
        dbEntity.setKnowledgeType(info.knowledgeType());
        dbEntity.setAppId(info.appId());
        dbEntity.setDescription(info.description());
        dbEntity.setEmbeddingModel(info.embeddingModel());
        dbEntity.setChunkSize(info.chunkSize());
        dbEntity.setChunkOverlap(info.chunkOverlap());
        dbEntity.setStatus(info.status());
        dbEntity.setProcessProgress(info.processProgress());
        dbEntity.setDocCount(info.docCount());
        dbEntity.setChunkCount(info.chunkCount());
        dbEntity.setVectorDbConfig(info.vectorDbConfig());
        dbEntity.setCreateTime(LocalDateTime.now());
        return dbEntity;
    }

    private DocInfo toDocInfo(AiKnowledgeDoc doc) {
        return new DocInfo(
                doc.getId(),
                doc.getKnowledgeId(),
                doc.getFileName(),
                doc.getFileSize(),
                doc.getFileType(),
                doc.getStatus(),
                doc.getChunkCount(),
                doc.getErrorMsg()
        );
    }

    private TextChunk toTextChunk(KnowledgeChunkEntity entity) {
        TextChunk chunk = new TextChunk();
        chunk.setId(entity.getChunkId());
        chunk.setKnowledgeId(entity.getKnowledgeId());
        chunk.setDocumentId(String.valueOf(entity.getDocId()));
        chunk.setContent(entity.getContent());
        chunk.setChunkIndex(entity.getChunkIndex());
        chunk.setStartPosition(entity.getStartOffset());
        chunk.setEndPosition(entity.getEndOffset());
        chunk.setMetadata(entity.getMetadata() != null ? entity.getMetadata() : Map.of());
        return chunk;
    }
}
