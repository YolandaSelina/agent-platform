package com.rk.agent.project.controller;

import com.rk.agent.common.result.Result;
import com.rk.agent.project.dto.ProjectPageSaveDTO;
import com.rk.agent.project.entity.ProjectPage;
import com.rk.agent.project.service.ProjectPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "项目页面管理")
@RestController
@RequestMapping("/page")
@RequiredArgsConstructor
public class ProjectPageController {

    private final ProjectPageService pageService;

    @Operation(summary = "列出项目下所有页面")
    @GetMapping("/list")
    public Result<List<ProjectPage>> list(@RequestParam Long projectId) {
        return Result.success(pageService.listByProject(projectId));
    }

    @Operation(summary = "按类型列出")
    @GetMapping("/list-by-type")
    public Result<List<ProjectPage>> listByType(@RequestParam Long projectId, @RequestParam String type) {
        return Result.success(pageService.listByType(projectId, type));
    }

    @Operation(summary = "保存页面（创建或更新）")
    @PostMapping("/save")
    public Result<ProjectPage> save(@Valid @RequestBody ProjectPageSaveDTO dto) {
        ProjectPage page = dto.toEntity();
        if (dto.getId() == null) {
            pageService.save(page);
        } else {
            page.setId(dto.getId());
            pageService.updateById(page);
        }
        return Result.success(page);
    }

    @Operation(summary = "删除页面")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pageService.deletePage(id);
        return Result.success();
    }
}
