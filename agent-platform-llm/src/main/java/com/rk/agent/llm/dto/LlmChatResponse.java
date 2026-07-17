package com.rk.agent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * LLM 聊天响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmChatResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String errorCode;
    private String errorMessage;
    private String content;
    private String finishReason;
    private Usage usage;
    private List<LlmChatRequest.ToolCall> toolCalls;
    private String platform;
    private String model;
    private Long latencyMs;
    private String requestId;
    private Long sessionId;
    private Map<String, Object> raw;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
