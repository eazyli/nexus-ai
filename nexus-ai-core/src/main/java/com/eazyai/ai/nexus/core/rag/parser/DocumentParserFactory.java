package com.eazyai.ai.nexus.core.rag.parser;

import com.eazyai.ai.nexus.core.rag.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * 文档解析器工厂
 */
@Slf4j
@Component
public class DocumentParserFactory {

    @Autowired(required = false)
    private List<DocumentParser> parsers;

    private final Map<String, DocumentParser> parserMap = new HashMap<>();

    @PostConstruct
    public void init() {
        if (parsers != null) {
            for (DocumentParser parser : parsers) {
                for (String ext : parser.getSupportedExtensions()) {
                    parserMap.put(ext.toLowerCase(), parser);
                    log.info("[DocumentParserFactory] 注册解析器: {} -> {}", ext, parser.getClass().getSimpleName());
                }
            }
        }
        log.info("[DocumentParserFactory] 初始化完成，支持 {} 种文件格式", parserMap.size());
    }

    /**
     * 解析文档
     * 
     * @param content 文档内容
     * @param fileName 文件名
     * @return 文档对象
     */
    public Document parse(byte[] content, String fileName) {
        String extension = getExtension(fileName);
        DocumentParser parser = getParser(extension);
        
        if (parser == null) {
            log.warn("[DocumentParserFactory] 不支持的文件格式: {}，使用默认解析器", extension);
            return parseAsText(content, fileName);
        }

        try {
            List<ParsedSection> sections = parser.parseWithMetadata(new ByteArrayInputStream(content));
            
            // 合并所有段落内容
            StringBuilder fullContent = new StringBuilder();
            for (int i = 0; i < sections.size(); i++) {
                if (i > 0) {
                    fullContent.append("\n\n");
                }
                fullContent.append(sections.get(i).getContent());
            }

            Document document = Document.of(fullContent.toString());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileName", fileName);
            metadata.put("extension", extension);
            metadata.put("sectionCount", sections.size());
            document.setMetadata(metadata);
            
            return document;

        } catch (Exception e) {
            log.error("[DocumentParserFactory] 解析文档失败: {}", fileName, e);
            throw new RuntimeException("解析文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析文档（返回带元数据的段落）
     */
    public List<ParsedSection> parseSections(byte[] content, String fileName) {
        String extension = getExtension(fileName);
        DocumentParser parser = getParser(extension);
        
        if (parser == null) {
            // 默认按段落分割
            String text = new String(content);
            return Arrays.stream(text.split("\n\n"))
                    .filter(s -> !s.isBlank())
                    .map(ParsedSection::of)
                    .toList();
        }

        try {
            return parser.parseWithMetadata(new ByteArrayInputStream(content));
        } catch (Exception e) {
            log.error("[DocumentParserFactory] 解析文档失败: {}", fileName, e);
            throw new RuntimeException("解析文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取解析器
     */
    public DocumentParser getParser(String extension) {
        return parserMap.get(extension.toLowerCase());
    }

    /**
     * 检查是否支持该格式
     */
    public boolean isSupported(String extension) {
        return parserMap.containsKey(extension.toLowerCase());
    }

    /**
     * 获取支持的格式列表
     */
    public Set<String> getSupportedExtensions() {
        return Collections.unmodifiableSet(parserMap.keySet());
    }

    /**
     * 默认文本解析
     */
    private Document parseAsText(byte[] content, String fileName) {
        String text = new String(content);
        Document document = Document.of(text);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", fileName);
        document.setMetadata(metadata);
        return document;
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String fileName) {
        if (fileName == null) {
            return "txt";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "txt";
    }
}
