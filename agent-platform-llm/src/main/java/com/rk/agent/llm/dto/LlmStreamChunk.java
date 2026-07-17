package com.rk.agent.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流式 chunk
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmStreamChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 增量内容 */
    private String content;
    /** 工具调用增量 */
    private LlmChatRequest.ToolCall toolCall;
    /** 结束原因 */
    private String finishReason;
    /** 错误信息 */
    private String error;
    /** 平台 */
    private String platform;
    /** 模型 */
    private String model;
    /** token 使用 */
    private LlmChatResponse.Usage usage;
}
