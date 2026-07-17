package com.rk.agent.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rk.agent.project.dto.ProjectDTO;
import com.rk.agent.project.entity.Project;

public interface ProjectService extends IService<Project> {

    Project create(ProjectDTO dto, Long currentUserId);

    Project update(ProjectDTO dto);

    IPage<Project> page(Long current, Long size, String keyword, String status);

    Project detail(Long id);
}
