package com.rk.agent.llm.client;

import com.rk.agent.llm.dto.LlmChatRequest;
import com.rk.agent.llm.dto.LlmChatResponse;

/**
 * LLM 客户端统一接口
 */
public interface LlmClient {

    /**
     * 同步聊天
     */
    LlmChatResponse chat(LlmChatRequest request);

    /**
     * 平台标识
     */
    String platform();

    /**
     * 健康检查
     */
    default boolean healthy() {
        return true;
    }

    /**
     * 运行时覆盖 base URL（用于项目级配置）
     */
    default void overrideBaseUrl(String baseUrl) {
        // 默认无操作，由具体实现支持
    }

    /**
     * 运行时覆盖 API Key（用于页面配置）
     */
    default void overrideApiKey(String apiKey) {
        // 默认无操作，由具体实现支持
    }

    /**
     * 运行时覆盖 model
     */
    default void overrideModel(String model) {
        // 默认无操作
    }
}
