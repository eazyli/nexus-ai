package com.eazyai.ai.nexus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能体请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户查询内容
     */
    private String query;

    /**
     * 应用ID（用于关联工具和场景）
     */
    private String appId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 任务类型（可选，用于路由）
     */
    private String taskType;

    /**
     * 会话ID（用于多轮对话上下文）
     */
    private String sessionId;

    /**
     * 最大迭代次数（防止无限循环）
     */
    @Builder.Default
    private int maxIterations = 10;

    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeout = 60000;

    /**
     * 业务参数
     */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();

    /**
     * 期望输出格式
     */
    private String outputFormat;

    /**
     * 创建构建器
     */
    public static AgentRequestBuilder builder() {
        return new AgentRequestBuilder();
    }
}
