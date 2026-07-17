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
 * LLM 聊天请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmChatRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 平台：OPENAI / ANTHROPIC / QWEN / DEEPSEEK */
    private String platform;
    /** 模型 */
    private String model;
    /** 消息列表 */
    private List<Message> messages;
    /** 系统提示 */
    private String systemPrompt;
    /** 温度 */
    private Double temperature;
    /** 最大 token */
    private Integer maxTokens;
    /** Top-P */
    private Double topP;
    /** 工具定义 */
    private List<Map<String, Object>> tools;
    /** 工具选择策略 */
    private String toolChoice;
    /** 额外参数 */
    private Map<String, Object> extra;

    // ===== 联网搜索 =====
    /** 是否启用联网搜索（覆盖 ProviderConfig.webSearchEnabled） */
    private Boolean webSearchEnabled;
    /** 搜索 provider：TAVILY / SERPAPI / BING / CUSTOM */
    private String webSearchProvider;
    /** 搜索 API Key（覆盖 ProviderConfig.webSearchApiKey） */
    private String webSearchApiKey;
    /** 搜索 base URL */
    private String webSearchBaseUrl;
    /** 搜索结果最大条数（默认 5） */
    private Integer webSearchMaxResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 角色：system / user / assistant / tool */
        private String role;
        /** 内容 */
        private String content;
        /** 名称（tool 消息时） */
        private String name;
        /** 工具调用 ID（tool 消息时） */
        private String toolCallId;
        /** 工具调用（assistant 消息时） */
        private List<ToolCall> toolCalls;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String id;
        private String type;
        private ToolFunction function;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolFunction implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String name;
        /** 参数（JSON 字符串） */
        private String arguments;
    }
}
