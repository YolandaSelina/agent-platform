package com.rk.agent.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rk.agent.llm.entity.ChatSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionEntity> {
}
