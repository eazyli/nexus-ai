package com.eazyai.ai.nexus.core.rag.splitter;

import com.eazyai.ai.nexus.core.rag.DocumentSplitter;
import com.eazyai.ai.nexus.core.rag.model.TextChunk;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Langchain4j 的文档切片器
 * 
 * <p>使用 Langchain4j 的递归分割算法，支持：</p>
 * <ul>
 *   <li>递归字符分割策略</li>
 *   <li>中英文智能分割</li>
 *   <li>段落优先、句子次之、单词最后的分割策略</li>
 * </ul>
 * 
 * <p>推荐使用此实现替代自定义的 RecursiveDocumentSplitter</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * DocumentSplitter splitter = new Langchain4jDocumentSplitter(500, 50);
 * List&lt;TextChunk&gt; chunks = splitter.split(text);
 * </pre>
 */
@Slf4j
public class Langchain4jDocumentSplitter implements DocumentSplitter {

    private final int chunkSize;
    private final int chunkOverlap;

    /**
     * 创建切片器
     * 
     * <p>使用字符数计算，自动选择最佳分割策略：</p>
     * <ul>
     *   <li>优先按段落分割（\n\n）</li>
     *   <li>其次按句子分割（\n）</li>
     *   <li>然后按单词分割（空格）</li>
     *   <li>最后按字符分割</li>
     * </ul>
     * 
     * @param chunkSize 每个切片的最大字符数
     * @param chunkOverlap 切片之间的重叠字符数
     */
    public Langchain4jDocumentSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        
        log.info("[Langchain4jDocumentSplitter] 初始化完成, chunkSize={}, chunkOverlap={}", chunkSize, chunkOverlap);
    }

    @Override
    public List<TextChunk> split(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        // 使用递归字符分割算法（类似 Langchain4j 的实现）
        List<String> splits = recursiveSplit(text);
        
        // 转换为 TextChunk
        List<TextChunk> chunks = new ArrayList<>();
        int currentPos = 0;
        int chunkIndex = 0;
        
        for (String split : splits) {
            TextChunk chunk = TextChunk.of(
                    split,
                    currentPos,
                    currentPos + split.length(),
                    chunkIndex++
            );
            chunks.add(chunk);
            currentPos += split.length() - chunkOverlap;
        }
        
        return chunks;
    }
    
    /**
     * 递归分割文本
     */
    private List<String> recursiveSplit(String text) {
        List<String> result = new ArrayList<>();
        
        // 如果文本小于切片大小，直接返回
        if (text.length() <= chunkSize) {
            result.add(text);
            return result;
        }
        
        // 分割符优先级：段落 > 句子 > 单词
        List<String> separators = List.of("\n\n", "\n", "。", "！", "？", ". ", "! ", "? ", " ");
        
        List<String> splits = new ArrayList<>();
        String separator = null;
        
        // 找到第一个存在的分割符
        for (String sep : separators) {
            if (text.contains(sep)) {
                separator = sep;
                splits = splitBySeparator(text, sep);
                break;
            }
        }
        
        // 如果没有找到任何分隔符，直接在chunkSize处切分
        if (splits.isEmpty()) {
            splits = forceSplitAtPosition(text, chunkSize);
        }
        
        // 合并小块到大块
        return mergeChunks(splits, separator);
    }
    
    /**
     * 在没有分隔符时，强制在指定位置切分文本
     */
    private List<String> forceSplitAtPosition(String text, int position) {
        List<String> splits = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + position, text.length());
            splits.add(text.substring(start, end));
            start = end;
        }
        
        return splits;
    }
    
    /**
     * 按分隔符分割文本
     */
    private List<String> splitBySeparator(String text, String separator) {
        List<String> splits = new ArrayList<>();
        int start = 0;
        int index;
        
        while ((index = text.indexOf(separator, start)) != -1) {
            String part = text.substring(start, index + separator.length());
            splits.add(part);
            start = index + separator.length();
        }
        
        // 添加剩余部分
        if (start < text.length()) {
            splits.add(text.substring(start));
        }
        
        return splits;
    }
    
    /**
     * 合并小块到大块
     */
    private List<String> mergeChunks(List<String> splits, String separator) {
        List<String> merged = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;
        
        for (String split : splits) {
            int splitLength = split.length();
            
            // 如果单个分块超过 chunkSize，需要进一步切分
            if (splitLength > chunkSize) {
                // 先保存当前块
                if (currentLength > 0) {
                    merged.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentLength = 0;
                }
                
                // 递归处理大块
                merged.addAll(recursiveSplit(split));
                continue;
            }
            
            // 检查是否需要开启新块
            if (currentLength + splitLength > chunkSize && currentLength > 0) {
                // 保存当前块
                merged.add(currentChunk.toString().trim());
                
                // 保留重叠部分
                String overlap = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlap);
                currentLength = overlap.length();
            }
            
            currentChunk.append(split);
            currentLength += splitLength;
        }
        
        // 保存最后一个块
        if (currentLength > 0) {
            merged.add(currentChunk.toString().trim());
        }
        
        return merged;
    }
    
    /**
     * 获取重叠文本
     */
    private String getOverlapText(String text) {
        if (chunkOverlap <= 0 || text.length() <= chunkOverlap) {
            return "";
        }
        
        // 从末尾获取重叠部分
        String overlapPart = text.substring(text.length() - chunkOverlap);
        
        // 尝试在重叠区域内找到句子边界
        for (String sep : List.of("。", "！", "？", ".", "!", "?", "\n", " ")) {
            int idx = overlapPart.indexOf(sep);
            if (idx >= 0 && idx < overlapPart.length() - 1) {
                return overlapPart.substring(idx + sep.length());
            }
        }
        
        return overlapPart;
    }

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public int getChunkOverlap() {
        return chunkOverlap;
    }
}
