package com.eazyai.ai.nexus.web.dto;

import com.eazyai.ai.nexus.core.mcp.McpToolDescriptor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 数据库工具注册请求DTO
 */
@Data
@Schema(description = "数据库工具注册请求")
public class DbToolRegisterRequest {

    @Schema(description = "所属应用ID", example = "wenhuagong")
    private String appId;

    @Schema(description = "工具ID（可选，不填则自动生成）")
    private String toolId;

    @NotBlank(message = "工具名称不能为空")
    @Schema(description = "工具名称", required = true, example = "queryOrderStatistics")
    private String name;

    @NotBlank(message = "工具描述不能为空")
    @Schema(description = "工具描述", required = true, example = "查询订单统计报表数据")
    private String description;

    @Schema(description = "工具能力标签", example = "[\"report\", \"db\", \"query\"]")
    private List<String> capabilities;

    @Schema(description = "参数定义")
    private List<McpToolDescriptor.ParamDefinition> parameters;

    @NotBlank(message = "数据源ID不能为空")
    @Schema(description = "数据源ID", required = true, example = "mysql-datasource-001")
    private String datasourceId;

    @NotBlank(message = "SQL模板不能为空")
    @Schema(description = "SQL模板（支持参数占位符）", required = true, 
            example = "SELECT * FROM orders WHERE status = #{status} AND create_time >= #{startDate}")
    private String sqlTemplate;

    @Schema(description = "查询类型", example = "SELECT", allowableValues = {"SELECT", "INSERT", "UPDATE", "DELETE"})
    private String queryType;

    @Schema(description = "重试次数", example = "2")
    private Integer retryTimes;

    @Schema(description = "超时时间（毫秒）", example = "10000")
    private Long timeout;
}
