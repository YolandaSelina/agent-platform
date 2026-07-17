package com.rk.agent.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rk.agent.project.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
