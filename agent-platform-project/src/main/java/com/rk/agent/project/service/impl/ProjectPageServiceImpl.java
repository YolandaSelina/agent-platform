package com.rk.agent.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.project.entity.ProjectPage;
import com.rk.agent.project.mapper.ProjectPageMapper;
import com.rk.agent.project.service.ProjectPageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectPageServiceImpl extends ServiceImpl<ProjectPageMapper, ProjectPage> implements ProjectPageService {

    @Override
    public List<ProjectPage> listByProject(Long projectId) {
        return list(new LambdaQueryWrapper<ProjectPage>()
                .eq(ProjectPage::getProjectId, projectId)
                .orderByAsc(ProjectPage::getSortOrder));
    }

    @Override
    public ProjectPage savePage(ProjectPage page) {
        if (page.getId() == null) {
            save(page);
        } else {
            updateById(page);
        }
        return page;
    }

    @Override
    public List<ProjectPage> listByType(Long projectId, String type) {
        return list(new LambdaQueryWrapper<ProjectPage>()
                .eq(ProjectPage::getProjectId, projectId)
                .eq(ProjectPage::getType, type)
                .orderByAsc(ProjectPage::getSortOrder));
    }

    @Override
    public boolean deletePage(Long id) {
        ProjectPage p = getById(id);
        if (p == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return removeById(id);
    }
}
