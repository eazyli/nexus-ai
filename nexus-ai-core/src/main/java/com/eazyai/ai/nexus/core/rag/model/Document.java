package com.eazyai.ai.nexus.core.rag.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档模型
 */
@Data
public class Document {
    
    private String id;
    
    private String knowledgeId;
    
    private String fileName;
    
    private String fileType;
    
    private String content;
    
    private Map<String, Object> metadata;
    
    private LocalDateTime createTime;
    
    public static Document of(String content) {
        Document doc = new Document();
        doc.setContent(content);
        doc.setCreateTime(LocalDateTime.now());
        return doc;
    }
    
    public static Document of(String content, Map<String, Object> metadata) {
        Document doc = of(content);
        doc.setMetadata(metadata != null ? metadata : new HashMap<>());
        return doc;
    }
}
