package com.rk.agent.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rk.agent.common.entity.PageResult;
import com.rk.agent.common.result.Result;
import com.rk.agent.project.entity.Document;
import com.rk.agent.project.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "文档管理")
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "创建或更新文档（pipeline 等其他服务调用）")
    @PostMapping("/upsert")
    public Result<Long> upsert(@RequestBody Document doc) {
        return Result.success(documentService.upsert(doc));
    }

    @Operation(summary = "文档详情")
    @GetMapping("/{id}")
    public Result<Document> detail(@PathVariable Long id) {
        return Result.success(documentService.detail(id));
    }

    @Operation(summary = "文档分页")
    @GetMapping("/page")
    public Result<PageResult<Document>> page(@RequestParam(defaultValue = "1") Long current,
                                              @RequestParam(defaultValue = "20") Long size,
                                              @RequestParam(required = false) Long projectId,
                                              @RequestParam(required = false) String docType,
                                              @RequestParam(required = false) String stage) {
        IPage<Document> page = documentService.page(current, size, projectId, docType, stage);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "按项目+类型列出")
    @GetMapping("/list")
    public Result<List<Document>> list(@RequestParam Long projectId,
                                        @RequestParam(required = false) String docType) {
        return Result.success(documentService.listByProject(projectId, docType));
    }

    @Operation(summary = "文档版本历史")
    @GetMapping("/{id}/versions")
    public Result<List<Map<String, Object>>> versions(@PathVariable Long id) {
        return Result.success(documentService.listVersions(id));
    }

    @Operation(summary = "文档统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam Long projectId) {
        return Result.success(documentService.statistics(projectId));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.removeById(id);
        return Result.success();
    }
}
