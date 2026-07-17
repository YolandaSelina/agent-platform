package com.rk.agent.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rk.agent.project.entity.ProjectPage;

import java.util.List;

public interface ProjectPageService extends IService<ProjectPage> {

    /**
     * 列出项目下所有页面（树形）
     */
    List<ProjectPage> listByProject(Long projectId);

    /**
     * 创建或更新页面
     */
    ProjectPage savePage(ProjectPage page);

    /**
     * 按类型列出
     */
    List<ProjectPage> listByType(Long projectId, String type);

    /**
     * 删除
     */
    boolean deletePage(Long id);
}
