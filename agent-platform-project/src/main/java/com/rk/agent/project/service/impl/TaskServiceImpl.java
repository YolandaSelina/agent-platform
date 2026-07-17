package com.rk.agent.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.project.dto.TaskDTO;
import com.rk.agent.project.entity.Task;
import com.rk.agent.project.mapper.TaskMapper;
import com.rk.agent.project.service.TaskService;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Override
    public Task create(TaskDTO dto, Long currentUserId) {
        if (dto.getProjectId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "项目 ID 不能为空");
        }
        Task t = new Task();
        BeanUtil.copyProperties(dto, t, "id");
        if (t.getStatus() == null) {
            t.setStatus("TODO");
        }
        if (t.getPriority() == null) {
            t.setPriority("P2");
        }
        t.setCreatorId(currentUserId);
        save(t);
        return t;
    }

    @Override
    public Task update(TaskDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "id 不能为空");
        }
        Task t = new Task();
        BeanUtil.copyProperties(dto, t);
        updateById(t);
        return getById(t.getId());
    }

    @Override
    public IPage<Task> page(Long current, Long size, Long projectId, String status, String priority) {
        LambdaQueryWrapper<Task> qw = new LambdaQueryWrapper<>();
        if (projectId != null) {
            qw.eq(Task::getProjectId, projectId);
        }
        if (StrUtil.isNotBlank(status)) {
            qw.eq(Task::getStatus, status);
        }
        if (StrUtil.isNotBlank(priority)) {
            qw.eq(Task::getPriority, priority);
        }
        qw.orderByDesc(Task::getUpdateTime);
        return page(new Page<>(current, size), qw);
    }

    @Override
    public Task detail(Long id) {
        Task t = getById(id);
        if (t == null) {
            throw new BizException(ResultCode.TASK_NOT_FOUND);
        }
        return t;
    }
}
