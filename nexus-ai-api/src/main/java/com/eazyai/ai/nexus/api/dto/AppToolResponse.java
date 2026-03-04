package com.eazyai.ai.nexus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用工具关联响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppToolResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
    private String toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具类型
     */
    private String toolType;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 绑定时间
     */
    private LocalDateTime bindTime;
}
