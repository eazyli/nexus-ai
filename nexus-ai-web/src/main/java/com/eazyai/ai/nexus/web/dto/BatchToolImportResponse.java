package com.eazyai.ai.nexus.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入工具响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量导入工具响应")
public class BatchToolImportResponse {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "成功导入数量")
    private Integer importedCount;

    @Schema(description = "失败数量")
    private Integer failedCount;

    @Schema(description = "导入的工具ID列表")
    private List<String> importedToolIds;

    @Schema(description = "失败的工具列表")
    private List<FailedTool> failedTools;

    @Schema(description = "消息")
    private String message;

    /**
     * 失败的工具
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "导入失败的工具")
    public static class FailedTool {
        @Schema(description = "工具名称")
        private String name;

        @Schema(description = "失败原因")
        private String reason;
    }
}
