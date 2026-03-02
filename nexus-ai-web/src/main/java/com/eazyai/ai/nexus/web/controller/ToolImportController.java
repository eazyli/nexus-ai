package com.eazyai.ai.nexus.web.controller;

import com.eazyai.ai.nexus.web.dto.ApifoxParseRequest;
import com.eazyai.ai.nexus.web.dto.ApifoxParseResponse;
import com.eazyai.ai.nexus.web.dto.BatchToolImportRequest;
import com.eazyai.ai.nexus.web.dto.BatchToolImportResponse;
import com.eazyai.ai.nexus.web.service.ApifoxParserService;
import com.eazyai.ai.nexus.web.service.ToolImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 工具导入控制器
 * 提供Apifox文档解析和批量工具导入功能
 * 
 * 使用案例：创建一个"工具管理应用"，通过AI对话方式实现工具的批量导入
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
@Tag(name = "工具导入接口", description = "Apifox文档解析和批量工具导入")
public class ToolImportController {

    private final ToolImportService toolImportService;
    private final ApifoxParserService apifoxParserService;

    /**
     * 解析Apifox导出的OpenAPI文档
     * 
     * 支持解析OpenAPI 3.0格式的文档，将API端点转换为工具定义
     * 可用于AI理解接口文档并协助批量导入
     */
    @PostMapping("/parse-apifox")
    @Operation(summary = "解析Apifox文档", description = "解析Apifox导出的OpenAPI格式文档，转换为工具定义列表")
    public ResponseEntity<ApifoxParseResponse> parseApifox(@Valid @RequestBody ApifoxParseRequest request) {
        log.info("解析Apifox文档, baseUrl: {}", request.getBaseUrl());
        ApifoxParseResponse response = apifoxParserService.parse(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 批量导入工具
     * 
     * 将多个工具定义批量导入到指定应用
     * 支持覆盖已存在的工具
     */
    @PostMapping("/batch-import")
    @Operation(summary = "批量导入工具", description = "批量导入多个工具到指定应用，支持覆盖已存在的工具")
    public ResponseEntity<BatchToolImportResponse> batchImport(@Valid @RequestBody BatchToolImportRequest request) {
        log.info("批量导入工具到应用: {}", request.getTargetAppId());
        BatchToolImportResponse response = toolImportService.batchImport(request);
        return ResponseEntity.ok(response);
    }
}
