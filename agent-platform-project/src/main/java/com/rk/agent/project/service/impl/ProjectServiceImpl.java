package com.rk.agent.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.project.dto.ProjectDTO;
import com.rk.agent.project.entity.Project;
import com.rk.agent.project.entity.ProjectMember;
import com.rk.agent.project.mapper.ProjectMapper;
import com.rk.agent.project.mapper.ProjectMemberMapper;
import com.rk.agent.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMemberMapper projectMemberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Project create(ProjectDTO dto, Long currentUserId) {
        Project exist = baseMapper.selectOne(new LambdaQueryWrapper<Project>().eq(Project::getCode, dto.getCode()));
        if (exist != null) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "项目编码已存在: " + dto.getCode());
        }
        Project p = new Project();
        BeanUtil.copyProperties(dto, p, "id");
        if (p.getStatus() == null) {
            p.setStatus("ACTIVE");
        }
        p.setOwnerId(currentUserId);
        save(p);
        // 自动添加创建者为 OWNER
        ProjectMember m = new ProjectMember();
        m.setProjectId(p.getId());
        m.setUserId(currentUserId);
        m.setRole("OWNER");
        projectMemberMapper.insert(m);
        return p;
    }

    @Override
    public Project update(ProjectDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "id 不能为空");
        }
        Project p = new Project();
        BeanUtil.copyProperties(dto, p);
        updateById(p);
        return getById(p.getId());
    }

    @Override
    public IPage<Project> page(Long current, Long size, String keyword, String status) {
        LambdaQueryWrapper<Project> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(Project::getName, keyword).or().like(Project::getCode, keyword));
        }
        if (StrUtil.isNotBlank(status)) {
            qw.eq(Project::getStatus, status);
        }
        qw.orderByDesc(Project::getUpdateTime);
        return page(new Page<>(current, size), qw);
    }

    @Override
    public Project detail(Long id) {
        Project p = getById(id);
        if (p == null) {
            throw new BizException(ResultCode.PROJECT_NOT_FOUND);
        }
        return p;
    }
}
