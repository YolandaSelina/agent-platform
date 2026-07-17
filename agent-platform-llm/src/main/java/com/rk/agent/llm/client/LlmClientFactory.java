package com.rk.agent.llm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.config.AgentProperties;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.llm.resolver.ProviderConfigResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM 客户端工厂（动态生成/缓存客户端）
 * - 4 个内置平台：OpenAI / Anthropic / Qwen / DeepSeek / SiliconFlow
 * - 任意 OpenAI 兼容自定义平台：通过页面配置，GenericOpenAiClient
 * - Anthropic 自定义平台：暂不支持（独立协议）
 */
@Component
@RequiredArgsConstructor
public class LlmClientFactory {

    private final AgentProperties agentProperties;
    private final ProviderConfigResolver configResolver;
    private final List<LlmClient> clients;
    private final ObjectMapper objectMapper;
    private final Map<String, LlmClient> cache = new ConcurrentHashMap<>();

    /**
     * 用 Resolver 解析配置
     */
    public LlmChatConfig resolve(String platform, String model, Long projectId) {
        ProviderConfigResolver.ResolvedConfig rc = configResolver.resolve(platform, model, projectId);
        return new LlmChatConfig(rc.getApiKey(), rc.getBaseUrl(), rc.getModel(), rc.getTimeoutSeconds());
    }

    /**
     * 取客户端（内置平台）
     */
    public LlmClient get(String platform) {
        if (platform == null || platform.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "platform 不能为空");
        }
        return cache.computeIfAbsent(platform.toUpperCase(), p -> clients.stream()
                .filter(c -> p.equalsIgnoreCase(c.platform()))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST, "不支持的 LLM 平台: " + platform)));
    }

    /**
     * 取客户端（带项目级配置 + 自定义平台支持）
     * 流程：
     *  1. 用 Resolver 找最佳配置
     *  2. 如果配置来源是 DB 且 baseUrl 与默认不一致 → 动态创建 GenericOpenAiClient
     *  3. 否则用内置客户端
     */
    public LlmClient get(String platform, String model, Long projectId) {
        if (platform == null || platform.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "platform 不能为空");
        }
        ProviderConfigResolver.ResolvedConfig rc = configResolver.resolve(platform, model, projectId);

        // Anthropic 独立协议：只能用内置或页面配置为同一 baseUrl
        if ("ANTHROPIC".equalsIgnoreCase(platform)) {
            LlmClient client = get(platform);
            if (rc.getBaseUrl() != null) client.overrideBaseUrl(rc.getBaseUrl());
            return client;
        }

        // OpenAI 兼容（OpenAI/Qwen/DeepSeek/SiliconFlow/自定义）：
        // 如果 DB 配置有 baseUrl 且 != yml 默认，用 GenericOpenAiClient
        if (rc.getApiKey() != null && !rc.getApiKey().isBlank() && !rc.getApiKey().equalsIgnoreCase("REPLACE_ME")
                && rc.getBaseUrl() != null) {
            return GenericOpenAiClient.create(platform, rc.getBaseUrl(), rc.getApiKey(), projectId, objectMapper);
        }
        return get(platform);
    }

    /**
     * 列出已注册的客户端
     */
    public List<String> listPlatforms() {
        return clients.stream().map(LlmClient::platform).sorted().toList();
    }

    /**
     * LLM 请求配置
     */
    public record LlmChatConfig(String apiKey, String baseUrl, String model, Integer timeoutSeconds) {
        public boolean isValid() {
            return apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("REPLACE_ME");
        }
    }
}
