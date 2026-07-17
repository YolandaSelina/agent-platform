package com.rk.agent.llm.controller;

import com.rk.agent.common.result.Result;
import com.rk.agent.common.security.LoginUser;
import com.rk.agent.common.security.SecurityContextHolder;
import com.rk.agent.llm.dto.LlmChatRequest;
import com.rk.agent.llm.dto.LlmChatResponse;
import com.rk.agent.llm.entity.ChatMessageEntity;
import com.rk.agent.llm.service.ChatHistoryService;
import com.rk.agent.llm.service.LlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "LLM 适配")
@RestController
@RequestMapping("/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;
    private final ChatHistoryService historyService;

    @Operation(summary = "LLM 聊天（同步，自动写历史）")
    @PostMapping("/chat")
    public Result<LlmChatResponse> chat(@Valid @RequestBody LlmChatRequest request,
                                        @RequestParam(required = false) Long projectId,
                                        @RequestParam(required = false) Long sessionId) {
        LoginUser u = SecurityContextHolder.required();
        // 自动创建/复用 session
        Long sid = sessionId;
        if (sid == null) {
            var s = historyService.createSession(u.getUserId(), null, request.getPlatform(), request.getModel());
            sid = s.getId();
        }
        // 记录 user 消息
        String userContent = "";
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            var last = request.getMessages().get(request.getMessages().size() - 1);
            if ("user".equals(last.getRole())) userContent = last.getContent();
        }
        if (!userContent.isEmpty()) {
            historyService.appendMessage(sid, u.getUserId(), "user", userContent, null, null, null, null);
        }
        // 调 LLM
        LlmChatResponse resp = llmService.chat(request, projectId);
        // 记录 assistant 消息
        if (resp != null && resp.isSuccess()) {
            Integer tokens = resp.getUsage() != null ? resp.getUsage().getTotalTokens() : null;
            historyService.appendMessage(sid, u.getUserId(), "assistant", resp.getContent(),
                    resp.getPlatform(), resp.getModel(), tokens, resp.getLatencyMs());
        }
        // 把 sessionId 塞到 data 里返回，前端用来后续关联
        resp.setSessionId(sid);
        return Result.success(resp);
    }
}

