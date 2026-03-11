package com.eazyai.ai.nexus.core.rag;

import com.eazyai.ai.nexus.core.rag.model.TextChunk;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * RAG 检索结果
 */
@Data
@Builder
public class RetrievalResult {
    
    /**
     * 查询文本
     */
    private String query;
    
    /**
     * 检索到的文档切片
     */
    private List<TextChunk> chunks;
    
    /**
     * 总耗时（ms）
     */
    private long totalTime;
    
    /**
     * 检索类型
     */
    private String searchType;
    
    /**
     * 额外信息
     */
    private Map<String, Object> metadata;
    
    /**
     * 获取拼接后的上下文文本
     */
    public String getContextText() {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            sb.append("【文档").append(i + 1).append("】\n");
            sb.append(chunk.getContent()).append("\n\n");
        }
        return sb.toString();
    }
    
    /**
     * 获取拼接后的上下文（别名方法）
     */
    public String getContext() {
        return getContextText();
    }
    
    /**
     * 获取用于 Prompt 的上下文
     */
    public String getPromptContext() {
        if (chunks == null || chunks.isEmpty()) {
            return "未找到相关信息。";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("以下是从知识库检索到的相关信息：\n\n");
        
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            sb.append("[").append(i + 1).append("] ");
            sb.append(chunk.getContent()).append("\n\n");
        }
        
        sb.append("请基于以上信息回答用户问题。");
        return sb.toString();
    }
}
