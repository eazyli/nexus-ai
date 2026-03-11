package com.eazyai.ai.nexus.core.rag.parser.impl;

import com.eazyai.ai.nexus.core.rag.parser.DocumentParser;
import com.eazyai.ai.nexus.core.rag.parser.ParsedSection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF 解析器
 * 
 * <p>需要依赖 org.apache.pdfbox:pdfbox</p>
 */
@Slf4j
@Component
@ConditionalOnClass(name = "org.apache.pdfbox.pdmodel.PDDocument")
public class PdfParser implements DocumentParser {

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"pdf"};
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
        
        try {
            // 使用反射加载PDFBox，避免编译期依赖
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdfTextStripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            
            Object document = pdDocumentClass.getMethod("load", InputStream.class)
                    .invoke(null, inputStream);
            
            try {
                int numberOfPages = (int) pdDocumentClass.getMethod("getNumberOfPages")
                        .invoke(document);
                
                for (int pageNum = 1; pageNum <= numberOfPages; pageNum++) {
                    Object stripper = pdfTextStripperClass.getDeclaredConstructor().newInstance();
                    
                    // 设置提取页面范围
                    pdfTextStripperClass.getMethod("setStartPage", int.class)
                            .invoke(stripper, pageNum);
                    pdfTextStripperClass.getMethod("setEndPage", int.class)
                            .invoke(stripper, pageNum);
                    
                    // 提取文本
                    String text = (String) pdfTextStripperClass.getMethod("getText", pdDocumentClass)
                            .invoke(stripper, document);
                    
                    if (text != null && !text.isBlank()) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("pageNumber", pageNum);
                        metadata.put("totalPages", numberOfPages);
                        
                        sections.add(ParsedSection.builder()
                                .content(text.trim())
                                .pageNumber(pageNum)
                                .metadata(metadata)
                                .build());
                    }
                }
            } finally {
                pdDocumentClass.getMethod("close").invoke(document);
            }
            
        } catch (ClassNotFoundException e) {
            log.warn("[PdfParser] PDFBox未安装，无法解析PDF文件");
            throw new RuntimeException("PDFBox未安装，请添加依赖: org.apache.pdfbox:pdfbox");
        } catch (Exception e) {
            log.error("[PdfParser] 解析PDF文件失败", e);
            throw new RuntimeException("解析PDF文件失败: " + e.getMessage(), e);
        }
        
        return sections;
    }
}
