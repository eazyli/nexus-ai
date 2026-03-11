package com.eazyai.ai.nexus.core.rag.parser.impl;

import com.eazyai.ai.nexus.core.rag.parser.DocumentParser;
import com.eazyai.ai.nexus.core.rag.parser.ParsedSection;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 纯文本解析器
 */
@Component
public class TxtParser implements DocumentParser {

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"txt", "text", "log"};
    }

    @Override
    public List<String> parse(InputStream inputStream) {
        List<String> paragraphs = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            StringBuilder currentParagraph = new StringBuilder();
            String line;
            int emptyLineCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    emptyLineCount++;
                    // 连续空行表示段落分隔
                    if (emptyLineCount >= 1 && currentParagraph.length() > 0) {
                        paragraphs.add(currentParagraph.toString().trim());
                        currentParagraph = new StringBuilder();
                    }
                } else {
                    emptyLineCount = 0;
                    if (currentParagraph.length() > 0) {
                        currentParagraph.append(" ");
                    }
                    currentParagraph.append(line.trim());
                }
            }
            
            // 添加最后一个段落
            if (currentParagraph.length() > 0) {
                paragraphs.add(currentParagraph.toString().trim());
            }
            
        } catch (Exception e) {
            throw new RuntimeException("解析文本文件失败", e);
        }
        
        return paragraphs;
    }

    @Override
    public List<ParsedSection> parseWithMetadata(InputStream inputStream) {
        List<String> paragraphs = parse(inputStream);
        List<ParsedSection> sections = new ArrayList<>();
        
        int offset = 0;
        for (int i = 0; i < paragraphs.size(); i++) {
            String content = paragraphs.get(i);
            sections.add(ParsedSection.builder()
                    .content(content)
                    .startOffset(offset)
                    .endOffset(offset + content.length())
                    .metadata(Map.of("paragraphIndex", i))
                    .build());
            offset += content.length() + 2; // +2 for paragraph separator
        }
        
        return sections;
    }
}
