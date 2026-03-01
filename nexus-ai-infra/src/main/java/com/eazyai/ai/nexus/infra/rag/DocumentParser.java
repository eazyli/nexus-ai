package com.eazyai.ai.nexus.infra.rag;

import java.util.List;

/**
 * 文档解析器接口
 */
public interface DocumentParser {
    
    /**
     * 支持的文件类型
     */
    List<String> supportedTypes();
    
    /**
     * 解析文档
     * @param filePath 文件路径
     * @return 解析后的文档
     */
    Document parse(String filePath);
    
    /**
     * 解析文档内容
     * @param content 文件内容字节
     * @param fileName 文件名
     * @return 解析后的文档
     */
    Document parse(byte[] content, String fileName);
    
    /**
     * 是否支持该文件类型
     */
    default boolean supports(String fileType) {
        return supportedTypes().stream()
                .anyMatch(type -> type.equalsIgnoreCase(fileType));
    }
}
