package com.rk.agent.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rk.agent.common.entity.PageResult;
import com.rk.agent.common.result.Result;
import com.rk.agent.common.security.SecurityContextHolder;
import com.rk.agent.project.dto.ProjectDTO;
import com.rk.agent.project.entity.Project;
import com.rk.agent.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "项目管理")
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "创建项目")
    @PostMapping
    public Result<Project> create(@Valid @RequestBody ProjectDTO dto) {
        return Result.success(projectService.create(dto, SecurityContextHolder.requireUserId()));
    }

    @Operation(summary = "更新项目")
    @PutMapping
    public Result<Project> update(@Valid @RequestBody ProjectDTO dto) {
        return Result.success(projectService.update(dto));
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "项目详情")
    @GetMapping("/{id}")
    public Result<Project> detail(@PathVariable Long id) {
        return Result.success(projectService.detail(id));
    }

    @Operation(summary = "项目分页")
    @GetMapping("/page")
    public Result<PageResult<Project>> page(@RequestParam(defaultValue = "1") Long current,
                                             @RequestParam(defaultValue = "20") Long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status) {
        IPage<Project> page = projectService.page(current, size, keyword, status);
        return Result.success(PageResult.of(page));
    }
}
