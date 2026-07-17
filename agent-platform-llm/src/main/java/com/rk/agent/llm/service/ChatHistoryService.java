package com.rk.agent.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.llm.entity.ChatMessageEntity;
import com.rk.agent.llm.entity.ChatSessionEntity;
import com.rk.agent.llm.mapper.ChatMessageMapper;
import com.rk.agent.llm.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatHistoryService extends ServiceImpl<ChatSessionMapper, ChatSessionEntity> {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    /**
     * 创建新会话（默认 title = "新对话"）
     */
    public ChatSessionEntity createSession(Long userId, String title, String platform, String model) {
        ChatSessionEntity s = new ChatSessionEntity();
        s.setUserId(userId);
        s.setTitle(title != null && !title.isBlank() ? title : "新对话");
        s.setPlatform(platform);
        s.setModel(model);
        s.setMessageCount(0);
        sessionMapper.insert(s);
        return s;
    }

    /**
     * 追加一条消息（同时更新 session 计数 + last_message_at）
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageEntity appendMessage(Long sessionId, Long userId, String role, String content,
                                            String platform, String model, Integer tokens, Long latencyMs) {
        ChatSessionEntity s = sessionMapper.selectById(sessionId);
        if (s == null || s.getDeleted() == 1) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "会话不存在: " + sessionId);
        }
        if (!s.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        ChatMessageEntity m = new ChatMessageEntity();
        m.setSessionId(sessionId);
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content);
        m.setPlatform(platform);
        m.setModel(model);
        m.setTokens(tokens);
        m.setLatencyMs(latencyMs);
        messageMapper.insert(m);

        // 更新 session
        s.setMessageCount(s.getMessageCount() == null ? 1 : s.getMessageCount() + 1);
        s.setLastMessageAt(LocalDateTime.now());
        // 自动用首条 user 消息前 30 字更新 title
        if ("新对话".equals(s.getTitle()) && "user".equals(role) && content != null && !content.isBlank()) {
            String snippet = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            s.setTitle(snippet.replaceAll("\\s+", " "));
        }
        sessionMapper.updateById(s);
        return m;
    }

    /**
     * 列出用户最近 N 条会话（按 last_message_at DESC）
     */
    public IPage<ChatSessionEntity> listRecentSessions(Long userId, long current, long size) {
        return sessionMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ChatSessionEntity>()
                        .eq(ChatSessionEntity::getUserId, userId)
                        .eq(ChatSessionEntity::getDeleted, 0)
                        .orderByDesc(ChatSessionEntity::getLastMessageAt)
                        .orderByDesc(ChatSessionEntity::getId));
    }

    /**
     * 拉取会话的全部消息（按 create_time ASC）
     */
    public List<ChatMessageEntity> listMessages(Long sessionId, Long userId) {
        ChatSessionEntity s = sessionMapper.selectById(sessionId);
        if (s == null || s.getDeleted() == 1) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "会话不存在: " + sessionId);
        }
        if (!s.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        return messageMapper.selectList(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .eq(ChatMessageEntity::getDeleted, 0)
                .orderByAsc(ChatMessageEntity::getCreateTime));
    }

    /**
     * 软删除会话
     * 注意：MyBatis-Plus 的 updateById 默认不更新 deleted 字段（认为是系统字段）
     * 所以用 LambdaUpdateWrapper 显式 set deleted
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        log.info("[deleteSession] enter sessionId={}, userId={}", sessionId, userId);
        ChatSessionEntity s = sessionMapper.selectById(sessionId);
        log.info("[deleteSession] loaded session: id={}, userId={}, deleted={}", s == null ? "null" : s.getId(), s == null ? "null" : s.getUserId(), s == null ? "null" : s.getDeleted());
        if (s == null || s.getDeleted() == 1) {
            log.info("[deleteSession] early return: session is null or already deleted");
            return;
        }
        if (!s.getUserId().equals(userId)) {
            log.warn("[deleteSession] forbidden: session.userId={} != caller.userId={}", s.getUserId(), userId);
            throw new BizException(ResultCode.FORBIDDEN, "无权删除该会话");
        }
        // 用 setSql 写原生 SQL 片段绕过 mybatis-plus 逻辑删除字段过滤
        // （LambdaUpdateWrapper.set(Entity::getDeleted, 1) 会被自动识别为逻辑删除字段并跳过）
        int rows = sessionMapper.update(null, new LambdaUpdateWrapper<ChatSessionEntity>()
                .eq(ChatSessionEntity::getId, sessionId)
                .setSql("deleted = 1"));
        log.info("[deleteSession] session update rows={}", rows);
        // 消息也软删除
        int msgRows = messageMapper.update(null, new LambdaUpdateWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .setSql("deleted = 1"));
        log.info("[deleteSession] message update rows={}", msgRows);
        log.info("[deleteSession] done");
    }
}
