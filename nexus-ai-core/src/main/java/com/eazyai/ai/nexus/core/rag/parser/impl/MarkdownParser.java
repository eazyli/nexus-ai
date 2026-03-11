package com.eazyai.ai.nexus.core.rag.parser.impl;

import com.eazyai.ai.nexus.core.rag.parser.DocumentParser;
import com.eazyai.ai.nexus.core.rag.parser.ParsedSection;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Markdown 解析器
 * 
 * <p>按标题层级分割文档，每个标题下的内容作为一个段落</p>
 */
@Component
public class MarkdownParser implements DocumentParser {

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"md", "markdown"};
    }

    @Override
    public List<String> parse(InputStream inputStream) {
        return parseWithMetadata(inputStream).stream()
                .map(ParsedSection::getContent)
                .toList();
    }

    @Override
    public List<ParsedSection> parseWithMetadata(InputStream inputStream) {
        List<ParsedSection> sections = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            StringBuilder currentContent = new StringBuilder();
            String currentTitle = "Introduction";
            int currentLevel = 0;
            int sectionIndex = 0;
            String line;
            
            while ((line = reader.readLine()) != null) {
                // 检测标题
                int headingLevel = getHeadingLevel(line);
                
                if (headingLevel > 0) {
                    // 保存上一个段落
                    if (currentContent.length() > 0) {
                        sections.add(createSection(currentTitle, currentLevel, 
                                currentContent.toString(), sectionIndex++));
                    }
                    
                    // 开始新段落
                    currentTitle = line.substring(headingLevel).trim();
                    currentLevel = headingLevel;
                    currentContent = new StringBuilder();
                } else {
                    // 添加内容
                    if (currentContent.length() > 0) {
                        currentContent.append("\n");
                    }
                    currentContent.append(line);
                }
            }
            
            // 添加最后一个段落
            if (currentContent.length() > 0) {
                sections.add(createSection(currentTitle, currentLevel, 
                        currentContent.toString(), sectionIndex));
            }
            
        } catch (Exception e) {
            throw new RuntimeException("解析Markdown文件失败", e);
        }
        
        return sections;
    }

    /**
     * 检测标题级别
     * @return 标题级别（1-6），非标题返回0
     */
    private int getHeadingLevel(String line) {
        if (line == null || line.isEmpty()) {
            return 0;
        }
        
        int level = 0;
        while (level < line.length() && level < 6 && line.charAt(level) == '#') {
            level++;
        }
        
        // 必须有 # 后跟空格
        if (level > 0 && level < line.length() && line.charAt(level) == ' ') {
            return level;
        }
        
        return 0;
    }

    /**
     * 创建段落
     */
    private ParsedSection createSection(String title, int level, String content, int index) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", title);
        metadata.put("headingLevel", level);
        metadata.put("sectionIndex", index);
        
        // 构建完整内容（标题 + 内容）
        String fullContent = "#".repeat(level) + " " + title + "\n" + content.trim();
        
        return ParsedSection.builder()
                .content(fullContent)
                .metadata(metadata)
                .build();
    }
}
