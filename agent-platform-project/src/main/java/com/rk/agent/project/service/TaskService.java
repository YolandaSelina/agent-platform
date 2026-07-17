package com.rk.agent.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rk.agent.project.dto.TaskDTO;
import com.rk.agent.project.entity.Task;

public interface TaskService extends IService<Task> {

    Task create(TaskDTO dto, Long currentUserId);

    Task update(TaskDTO dto);

    IPage<Task> page(Long current, Long size, Long projectId, String status, String priority);

    Task detail(Long id);
}
