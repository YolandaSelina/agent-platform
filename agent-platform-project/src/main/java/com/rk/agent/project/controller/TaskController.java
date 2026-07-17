package com.rk.agent.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rk.agent.common.entity.PageResult;
import com.rk.agent.common.result.Result;
import com.rk.agent.common.security.SecurityContextHolder;
import com.rk.agent.project.dto.TaskDTO;
import com.rk.agent.project.entity.Task;
import com.rk.agent.project.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "任务管理")
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "创建任务")
    @PostMapping
    public Result<Task> create(@Valid @RequestBody TaskDTO dto) {
        return Result.success(taskService.create(dto, SecurityContextHolder.requireUserId()));
    }

    @Operation(summary = "更新任务")
    @PutMapping
    public Result<Task> update(@Valid @RequestBody TaskDTO dto) {
        return Result.success(taskService.update(dto));
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{id}")
    public Result<Task> detail(@PathVariable Long id) {
        return Result.success(taskService.detail(id));
    }

    @Operation(summary = "任务分页")
    @GetMapping("/page")
    public Result<PageResult<Task>> page(@RequestParam(defaultValue = "1") Long current,
                                          @RequestParam(defaultValue = "20") Long size,
                                          @RequestParam(required = false) Long projectId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String priority) {
        IPage<Task> page = taskService.page(current, size, projectId, status, priority);
        return Result.success(PageResult.of(page));
    }
}
