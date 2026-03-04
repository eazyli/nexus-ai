package com.eazyai.ai.nexus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 应用工具绑定请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppToolBindRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工具ID列表
     */
    private List<String> toolIds;

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;
}
