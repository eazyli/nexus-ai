package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeBaseService;
import com.eazyai.ai.nexus.core.rag.knowledge.KnowledgeBaseService.RetrievalResult;
import com.eazyai.ai.nexus.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    // ==================== 知识库管理 ====================

    /**
     * 创建知识库
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createKnowledge(
            @Valid @RequestBody KnowledgeCreateRequest request) {
        
        Map<String, Object> config = request.getConfig() != null ? request.getConfig() : new HashMap<>();
        config.put("type", request.getKnowledgeType());
        config.put("description", request.getDescription());
        
        String knowledgeId = knowledgeBaseService.createKnowledge(
                request.getKnowledgeName(), 
                request.getAppId(), 
                config
        );
        
        log.info("[KnowledgeBaseController] 创建知识库: {} -> {}", knowledgeId, request.getKnowledgeName());
        
        Map<String, Object> data = Map.of(
                "knowledgeId", knowledgeId,
                "knowledgeName", request.getKnowledgeName(),
                "status", "SUCCESS"
        );
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{knowledgeId}")
    public ResponseEntity<ApiResponse<KnowledgeBaseService.KnowledgeBaseInfo>> getKnowledge(
            @PathVariable String knowledgeId) {
        
        KnowledgeBaseService.KnowledgeBaseInfo info = knowledgeBaseService.getKnowledge(knowledgeId);
        
        if (info == null) {
            return ResponseEntity.ok(ApiResponse.notFound("知识库不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    /**
     * 获取应用关联的知识库列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeBaseService.KnowledgeBaseInfo>>> listByAppId(
            @RequestParam(required = false) String appId) {
        
        List<KnowledgeBaseService.KnowledgeBaseInfo> knowledgeBases;
        
        if (appId != null) {
            knowledgeBases = knowledgeBaseService.listByAppId(appId);
        } else {
            knowledgeBases = List.of();
        }
        
        return ResponseEntity.ok(ApiResponse.success(knowledgeBases));
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{knowledgeId}")
    public ResponseEntity<ApiResponse<Void>> deleteKnowledge(@PathVariable String knowledgeId) {
        knowledgeBaseService.deleteKnowledge(knowledgeId);
        
        log.info("[KnowledgeBaseController] 删除知识库: {}", knowledgeId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    // ==================== 文档管理 ====================

    /**
     * 上传文档
     */
    @PostMapping("/{knowledgeId}/documents")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
            @PathVariable String knowledgeId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            String fileName = file.getOriginalFilename();
            byte[] content = file.getBytes();
            
            Long docId = knowledgeBaseService.addDocument(knowledgeId, content, fileName);
            
            log.info("[KnowledgeBaseController] 上传文档: {} -> {}", knowledgeId, fileName);
            
            Map<String, Object> data = Map.of(
                    "docId", docId,
                    "fileName", fileName,
                    "fileSize", file.getSize(),
                    "status", "PROCESSING"
            );
            
            return ResponseEntity.ok(ApiResponse.success(data));
            
        } catch (Exception e) {
            log.error("[KnowledgeBaseController] 上传文档失败", e);
            return ResponseEntity.ok(ApiResponse.error("上传失败: " + e.getMessage()));
        }
    }

    /**
     * 批量上传文档
     */
    @PostMapping("/{knowledgeId}/documents/batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocuments(
            @PathVariable String knowledgeId,
            @RequestParam("files") MultipartFile[] files) {
        
        int successCount = 0;
        int failCount = 0;
        
        for (MultipartFile file : files) {
            try {
                String fileName = file.getOriginalFilename();
                byte[] content = file.getBytes();
                knowledgeBaseService.addDocument(knowledgeId, content, fileName);
                successCount++;
            } catch (Exception e) {
                log.error("[KnowledgeBaseController] 上传文档失败: {}", file.getOriginalFilename(), e);
                failCount++;
            }
        }
        
        Map<String, Object> data = Map.of(
                "total", files.length,
                "success", successCount,
                "failed", failCount
        );
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{knowledgeId}/documents/{docId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable String knowledgeId,
            @PathVariable Long docId) {
        
        knowledgeBaseService.deleteDocument(knowledgeId, docId);
        
        log.info("[KnowledgeBaseController] 删除文档: {} -> {}", knowledgeId, docId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    // ==================== 检索 ====================

    /**
     * 知识库检索
     */
    @PostMapping("/retrieve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retrieve(
            @Valid @RequestBody KnowledgeRetrieveRequest request) {
        
        RetrievalResult result = knowledgeBaseService.retrieve(
                request.getQuery(), 
                request.getKnowledgeIds(), 
                request.getTopK()
        );
        
        Map<String, Object> data = Map.of(
                "query", result.query(),
                "chunks", result.chunks(),
                "total", result.chunks().size(),
                "executionTime", result.totalTime(),
                "context", result.getContext()
        );
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 按应用检索
     */
    @PostMapping("/retrieve-by-app")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retrieveByApp(
            @Valid @RequestBody KnowledgeRetrieveByAppRequest request) {
        
        RetrievalResult result = knowledgeBaseService.retrieveByApp(
                request.getQuery(), 
                request.getAppId(), 
                request.getTopK()
        );
        
        Map<String, Object> data = Map.of(
                "query", result.query(),
                "chunks", result.chunks(),
                "total", result.chunks().size(),
                "executionTime", result.totalTime(),
                "context", result.getContext()
        );
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 添加文本到知识库
     */
    @PostMapping("/{knowledgeId}/text")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addText(
            @PathVariable String knowledgeId,
            @Valid @RequestBody KnowledgeAddTextRequest request) {
        
        Map<String, Object> metadata = request.getMetadata() != null 
                ? request.getMetadata() 
                : new HashMap<>();
        
        int chunkCount = knowledgeBaseService.addText(knowledgeId, request.getText(), metadata);
        
        log.info("[KnowledgeBaseController] 添加文本: {} -> {} 个切片", knowledgeId, chunkCount);
        
        Map<String, Object> data = Map.of(
                "chunkCount", chunkCount,
                "status", "SUCCESS"
        );
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
