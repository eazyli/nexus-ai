package com.eazyai.ai.nexus.core.rag.splitter;

import com.eazyai.ai.nexus.core.rag.DocumentSplitter;
import com.eazyai.ai.nexus.core.rag.model.TextChunk;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用 LangChain4j 官方文档分割器的实现
 * 
 * <p>基于 LangChain4j 0.36.2+ 版本，提供四种分割策略：</p>
 * <ul>
 *   <li>段落分割（Paragraph）：按 \n\n 分割</li>
 *   <li>句子分割（Sentence）：按句子边界分割</li>
 *   <li>单词分割（Word）：按空格分割</li>
 *   <li>字符分割（Character）：按字符分割（兜底策略）</li>
 * </ul>
 * 
 * <p>特点：</p>
 * <ul>
 *   <li>使用 LangChain4j 官方实现，经过充分测试</li>
 *   <li>支持递归分割策略（段落 -> 句子 -> 单词 -> 字符）</li>
 *   <li>自动处理中英文混合文本</li>
 *   <li>支持 chunkSize 和 chunkOverlap 配置</li>
 * </ul>
 * 
 * <p>使用示例：</p>
 * <pre>
 * DocumentSplitter splitter = new Langchain4jOfficialSplitter(500, 50);
 * List&lt;TextChunk&gt; chunks = splitter.split(text);
 * </pre>
 * 
 * <p>推荐使用此实现替代自定义的分割器</p>
 */
@Slf4j
public class Langchain4jOfficialSplitter implements DocumentSplitter {

    private final int chunkSize;
    private final int chunkOverlap;
    private final dev.langchain4j.data.document.DocumentSplitter langchainSplitter;

    /**
     * 创建 LangChain4j 官方分割器
     * 
     * <p>使用递归分割策略：</p>
     * <ol>
     *   <li>首先尝试按段落分割</li>
     *   <li>如果段落太长，按句子分割</li>
     *   <li>如果句子太长，按单词分割</li>
     *   <li>最后按字符分割</li>
     * </ol>
     * 
     * @param chunkSize 每个切片的最大字符数（按 tokenizer 估算）
     * @param chunkOverlap 切片之间的重叠字符数
     */
    public Langchain4jOfficialSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        
        // 使用 LangChain4j 的递归分割器
        // 优先按段落，然后句子，然后单词，最后字符
        this.langchainSplitter = new DocumentByParagraphSplitter(
                chunkSize,
                chunkOverlap,
                new DocumentBySentenceSplitter(
                        chunkSize,
                        chunkOverlap,
                        new DocumentByWordSplitter(
                                chunkSize,
                                chunkOverlap,
                                new DocumentByCharacterSplitter(
                                        chunkSize,
                                        chunkOverlap
                                )
                        )
                )
        );
        
        log.info("[Langchain4jOfficialSplitter] 初始化完成, chunkSize={}, chunkOverlap={}", chunkSize, chunkOverlap);
    }

    @Override
    public List<TextChunk> split(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        try {
            // 将文本转换为 LangChain4j Document
            Document document = Document.from(text);
            
            // 使用 LangChain4j 分割器进行分割（返回的是 TextSegment，不是 Document）
            List<TextSegment> segments = langchainSplitter.split(document);
            
            // 转换为 TextChunk
            List<TextChunk> chunks = new ArrayList<>();
            int currentPos = 0;
            int chunkIndex = 0;
            
            for (TextSegment segment : segments) {
                String chunkText = segment.text();
                
                TextChunk chunk = TextChunk.of(
                        chunkText,
                        currentPos,
                        currentPos + chunkText.length(),
                        chunkIndex++
                );
                chunks.add(chunk);
                
                // 更新位置（考虑重叠）
                currentPos += chunkText.length() - chunkOverlap;
            }
            
            log.debug("[Langchain4jOfficialSplitter] 成功分割文本，共 {} 个切片", chunks.size());
            return chunks;
            
        } catch (Exception e) {
            log.error("[Langchain4jOfficialSplitter] 分割文本失败，将使用备用分割器", e);
            // 如果 LangChain4j 分割失败，使用之前修复过的自定义分割器作为备用
            return new Langchain4jDocumentSplitter(chunkSize, chunkOverlap).split(text);
        }
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