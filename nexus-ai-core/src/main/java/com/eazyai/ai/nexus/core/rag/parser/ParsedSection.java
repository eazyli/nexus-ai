package com.eazyai.ai.nexus.core.rag.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 解析后的段落（带元数据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedSection {

    /**
     * 段落内容
     */
    private String content;

    /**
     * 页码（适用于PDF等）
     */
    private Integer pageNumber;

    /**
     * 起始偏移量
     */
    private Integer startOffset;

    /**
     * 结束偏移量
     */
    private Integer endOffset;

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 创建简单段落
     */
    public static ParsedSection of(String content) {
        return ParsedSection.builder().content(content).build();
    }

    /**
     * 创建带页码的段落
     */
    public static ParsedSection of(String content, int pageNumber) {
        return ParsedSection.builder()
                .content(content)
                .pageNumber(pageNumber)
                .build();
    }
}
