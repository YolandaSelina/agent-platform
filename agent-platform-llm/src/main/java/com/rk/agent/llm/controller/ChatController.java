package com.rk.agent.llm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rk.agent.common.entity.PageResult;
import com.rk.agent.common.result.Result;
import com.rk.agent.common.security.LoginUser;
import com.rk.agent.common.security.SecurityContextHolder;
import com.rk.agent.llm.entity.ChatMessageEntity;
import com.rk.agent.llm.entity.ChatSessionEntity;
import com.rk.agent.llm.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "智能问答历史")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatHistoryService historyService;

    @Operation(summary = "创建新会话")
    @PostMapping("/session")
    public Result<ChatSessionEntity> createSession(@RequestBody(required = false) Map<String, String> body) {
        LoginUser u = SecurityContextHolder.required();
        String title = body != null ? body.get("title") : null;
        String platform = body != null ? body.get("platform") : null;
        String model = body != null ? body.get("model") : null;
        return Result.success(historyService.createSession(u.getUserId(), title, platform, model));
    }

    @Operation(summary = "历史会话分页（按 last_message_at DESC）")
    @GetMapping("/session/list")
    public Result<PageResult<ChatSessionEntity>> listSessions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        LoginUser u = SecurityContextHolder.required();
        IPage<ChatSessionEntity> page = historyService.listRecentSessions(u.getUserId(), current, size);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "加载会话的全部消息")
    @GetMapping("/session/{id}/messages")
    public Result<List<ChatMessageEntity>> listMessages(@PathVariable Long id) {
        LoginUser u = SecurityContextHolder.required();
        return Result.success(historyService.listMessages(id, u.getUserId()));
    }

    @Operation(summary = "软删除会话")
    @DeleteMapping("/session/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        log.info("[ChatController.deleteSession] enter id={}", id);
        LoginUser u = SecurityContextHolder.required();
        log.info("[ChatController.deleteSession] userId={}", u.getUserId());
        historyService.deleteSession(id, u.getUserId());
        log.info("[ChatController.deleteSession] service call returned");
        return Result.success();
    }
}
