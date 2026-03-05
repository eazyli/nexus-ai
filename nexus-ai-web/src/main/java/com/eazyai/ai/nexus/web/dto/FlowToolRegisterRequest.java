package com.eazyai.ai.nexus.web.dto;

import com.eazyai.ai.nexus.api.tool.ToolDescriptor;
import com.eazyai.ai.nexus.api.tool.ToolVisibility;
import com.eazyai.ai.nexus.api.tool.flow.FlowDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 流程工具注册请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "流程工具注册请求")
public class FlowToolRegisterRequest {
    
    @Schema(description = "工具ID（可选，不填则自动生成）")
    private String toolId;
    
    @Schema(description = "所属应用ID")
    private String appId;
    
    @Schema(description = "流程名称")
    private String name;
    
    @Schema(description = "流程描述")
    private String description;
    
    @Schema(description = "流程定义")
    private FlowDefinition flowDefinition;
    
    @Schema(description = "输入参数定义")
    private List<ToolDescriptor.ParamDefinition> parameters;
    
    @Schema(description = "可见性: PRIVATE/PUBLIC/SHARED")
    private String visibility;
    
    @Schema(description = "授权应用列表（visibility为SHARED时生效）")
    private List<String> authorizedApps;
    
    @Schema(description = "能力标签")
    private List<String> capabilities;
    
    @Schema(description = "重试次数")
    private Integer retryTimes;
    
    @Schema(description = "超时时间（毫秒）")
    private Long timeout;
    
    @Schema(description = "是否启用")
    @Builder.Default
    private Boolean enabled = true;
}
