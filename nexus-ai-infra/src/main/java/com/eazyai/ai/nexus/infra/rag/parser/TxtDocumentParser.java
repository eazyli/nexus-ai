package com.eazyai.ai.nexus.infra.rag.parser;

import com.eazyai.ai.nexus.infra.rag.Document;
import com.eazyai.ai.nexus.infra.rag.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TXT文档解析器
 */
@Slf4j
@Component
public class TxtDocumentParser implements DocumentParser {

    @Override
    public List<String> supportedTypes() {
        return List.of("txt", "text", "md", "markdown");
    }

    @Override
    public Document parse(String filePath) {
        try {
            String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            String fileName = Paths.get(filePath).getFileName().toString();
            Document doc = new Document();
            doc.setContent(content);
            doc.setFileName(fileName);
            doc.setFileType(getFileType(fileName));
            doc.setCreateTime(LocalDateTime.now());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", filePath);
            metadata.put("size", content.length());
            doc.setMetadata(metadata);
            
            return doc;
        } catch (Exception e) {
            log.error("解析TXT文档失败: {}", filePath, e);
            throw new RuntimeException("解析TXT文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Document parse(byte[] content, String fileName) {
        try {
            String text = new String(content, StandardCharsets.UTF_8);
            Document doc = new Document();
            doc.setContent(text);
            doc.setFileName(fileName);
            doc.setFileType(getFileType(fileName));
            doc.setCreateTime(LocalDateTime.now());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("size", text.length());
            doc.setMetadata(metadata);
            
            return doc;
        } catch (Exception e) {
            log.error("解析TXT文档失败: {}", fileName, e);
            throw new RuntimeException("解析TXT文档失败: " + e.getMessage(), e);
        }
    }

    private String getFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "txt";
    }
}
