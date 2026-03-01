package com.eazyai.ai.nexus.infra.rag.splitter;

import com.eazyai.ai.nexus.infra.rag.Document;
import com.eazyai.ai.nexus.infra.rag.TextChunk;
import com.eazyai.ai.nexus.infra.rag.TextSplitter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归字符文本切片器
 * 按段落、句子、单词优先级切分
 */
@Slf4j
public class RecursiveCharacterTextSplitter implements TextSplitter {

    private final int chunkSize;
    private final int chunkOverlap;
    private final List<String> separators;

    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        // 中文优先按段落和句号分割，英文按段落和换行分割
        this.separators = List.of("\n\n", "\n", "。", "！", "？", ". ", "! ", "? ", " ", "");
    }

    public RecursiveCharacterTextSplitter() {
        this(500, 50);
    }

    @Override
    public List<TextChunk> split(String text) {
        return splitText(text, 0);
    }

    @Override
    public List<TextChunk> split(Document document) {
        List<TextChunk> chunks = split(document.getContent());
        // 设置文档ID和知识库ID
        for (TextChunk chunk : chunks) {
            chunk.setDocumentId(document.getId());
            chunk.setKnowledgeId(document.getKnowledgeId());
            if (document.getMetadata() != null) {
                chunk.setMetadata(new java.util.HashMap<>(document.getMetadata()));
            }
        }
        return chunks;
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public int getChunkOverlap() {
        return chunkOverlap;
    }

    private List<TextChunk> splitText(String text, int globalOffset) {
        List<TextChunk> result = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return result;
        }

        // 如果文本小于切片大小，直接返回
        if (text.length() <= chunkSize) {
            TextChunk chunk = TextChunk.of(text, globalOffset, globalOffset + text.length(), 0);
            result.add(chunk);
            return result;
        }

        // 尝试用不同的分隔符切分
        List<String> splits = new ArrayList<>();
        String separator = null;
        
        for (String sep : separators) {
            if (text.contains(sep)) {
                separator = sep;
                splits = splitBySeparator(text, sep);
                break;
            }
        }

        // 如果没有找到合适的分隔符，强制按字符数切分
        if (separator == null) {
            splits = forceSplit(text);
        }

        // 合并小块到大块
        List<String> mergedChunks = mergeChunks(splits, separator);

        // 创建TextChunk对象
        int currentOffset = globalOffset;
        int chunkIndex = 0;
        
        for (String chunk : mergedChunks) {
            TextChunk textChunk = TextChunk.of(chunk, currentOffset, currentOffset + chunk.length(), chunkIndex++);
            result.add(textChunk);
            currentOffset += chunk.length() - chunkOverlap;
        }

        return result;
    }

    private List<String> splitBySeparator(String text, String separator) {
        List<String> splits = new ArrayList<>();
        int start = 0;
        int index;
        
        while ((index = text.indexOf(separator, start)) != -1) {
            String part = text.substring(start, index + separator.length());
            splits.add(part);
            start = index + separator.length();
        }
        
        // 添加最后剩余部分
        if (start < text.length()) {
            splits.add(text.substring(start));
        }
        
        return splits;
    }

    private List<String> forceSplit(String text) {
        List<String> splits = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            splits.add(text.substring(i, end));
        }
        return splits;
    }

    private List<String> mergeChunks(List<String> splits, String separator) {
        List<String> merged = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;

        for (String split : splits) {
            int splitLength = split.length();
            
            // 如果单个分块就超过了chunkSize，需要进一步切分
            if (splitLength > chunkSize) {
                // 先保存当前的chunk
                if (currentLength > 0) {
                    merged.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentLength = 0;
                }
                
                // 递归处理大块
                List<TextChunk> subChunks = splitText(split, 0);
                for (TextChunk subChunk : subChunks) {
                    merged.add(subChunk.getContent());
                }
                continue;
            }

            // 检查是否需要开启新的chunk
            if (currentLength + splitLength > chunkSize && currentLength > 0) {
                // 保存当前chunk
                merged.add(currentChunk.toString().trim());
                
                // 保留overlap部分
                String overlapStr = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlapStr);
                currentLength = overlapStr.length();
            }

            currentChunk.append(split);
            currentLength += splitLength;
        }

        // 保存最后一个chunk
        if (currentLength > 0) {
            merged.add(currentChunk.toString().trim());
        }

        return merged;
    }

    private String getOverlapText(String text) {
        if (chunkOverlap <= 0 || text.length() <= chunkOverlap) {
            return "";
        }
        
        // 从后往前找合适的分割点
        String overlapPart = text.substring(text.length() - chunkOverlap);
        
        // 尝试在overlap区域内找到句子边界
        for (String sep : List.of("。", "！", "？", ".", "!", "?", "\n", " ")) {
            int idx = overlapPart.indexOf(sep);
            if (idx >= 0 && idx < overlapPart.length() - 1) {
                return overlapPart.substring(idx + sep.length());
            }
        }
        
        return overlapPart;
    }
}
