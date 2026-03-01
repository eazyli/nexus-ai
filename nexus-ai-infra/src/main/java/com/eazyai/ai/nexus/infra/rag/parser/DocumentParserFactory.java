package com.eazyai.ai.nexus.infra.rag.parser;

import com.eazyai.ai.nexus.infra.rag.Document;
import com.eazyai.ai.nexus.infra.rag.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档解析器工厂
 */
@Slf4j
@Component
public class DocumentParserFactory {

    private final Map<String, DocumentParser> parserMap = new HashMap<>();
    private final DocumentParser defaultParser;

    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.defaultParser = new TxtDocumentParser();
        
        // 注册所有解析器
        for (DocumentParser parser : parsers) {
            for (String type : parser.supportedTypes()) {
                parserMap.put(type.toLowerCase(), parser);
                log.info("注册文档解析器: {} -> {}", type, parser.getClass().getSimpleName());
            }
        }
    }

    /**
     * 获取指定类型的解析器
     */
    public Optional<DocumentParser> getParser(String fileType) {
        if (fileType == null) {
            return Optional.of(defaultParser);
        }
        return Optional.ofNullable(parserMap.get(fileType.toLowerCase()));
    }

    /**
     * 解析文档
     */
    public Document parse(byte[] content, String fileName) {
        String fileType = getFileType(fileName);
        DocumentParser parser = getParser(fileType).orElse(defaultParser);
        log.info("使用解析器 {} 解析文件: {}", parser.getClass().getSimpleName(), fileName);
        return parser.parse(content, fileName);
    }

    /**
     * 解析文档
     */
    public Document parse(String filePath, String fileName) {
        String fileType = getFileType(fileName);
        DocumentParser parser = getParser(fileType).orElse(defaultParser);
        log.info("使用解析器 {} 解析文件: {}", parser.getClass().getSimpleName(), fileName);
        return parser.parse(filePath);
    }

    /**
     * 检查是否支持该文件类型
     */
    public boolean supports(String fileType) {
        return parserMap.containsKey(fileType.toLowerCase());
    }

    private String getFileType(String fileName) {
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
