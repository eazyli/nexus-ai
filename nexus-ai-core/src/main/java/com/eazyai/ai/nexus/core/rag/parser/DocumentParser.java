package com.eazyai.ai.nexus.core.rag.parser;

import java.io.InputStream;
import java.util.List;

/**
 * 文档解析器接口
 * 
 * <p>核心层接口，定义文档解析的基本操作</p>
 * <p>由 infra 层实现具体的解析器（如 TxtParser, PdfParser, MarkdownParser）</p>
 */
public interface DocumentParser {

    /**
     * 支持的文件扩展名
     */
    String[] getSupportedExtensions();

    /**
     * 解析文档，返回文本段落列表
     * 
     * @param inputStream 文档输入流
     * @return 解析后的文本段落
     */
    List<String> parse(InputStream inputStream);

    /**
     * 解析文档，返回带元数据的段落
     * 
     * @param inputStream 文档输入流
     * @return 解析后的段落（带元数据）
     */
    default List<ParsedSection> parseWithMetadata(InputStream inputStream) {
        return parse(inputStream).stream()
                .map(content -> ParsedSection.builder().content(content).build())
                .toList();
    }

    /**
     * 检查是否支持该文件类型
     */
    default boolean supports(String extension) {
        String ext = extension.toLowerCase();
        for (String supported : getSupportedExtensions()) {
            if (supported.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }
}
