package com.rk.agent.llm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.config.AgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 通用 OpenAI 兼容协议客户端
 * <p>
 * 用于支持页面动态添加的任意 OpenAI 兼容平台（如 SiliconFlow、Moonshot、自建网关等）。
 * <p>
 * <b>每次调用 new 一个新实例</b>：不缓存。原因——DB 配置可能在运行时被改（用户编辑、删除、新增），
 * 缓存会让旧 client 持有旧 apiKey/baseUrl，导致新配置不生效。HttpClient 本身是 OkHttp 单例复用，
 * 这里的 client 实例只持有 config 引用，new 一个开销极小。
 */
@Slf4j
public class GenericOpenAiClient extends AbstractOpenAiCompatibleClient {

    private final String platform;

    private GenericOpenAiClient(String platform, AgentProperties.Llm.ProviderConfig cfg, ObjectMapper mapper) {
        super(cfg, mapper);
        this.platform = platform;
    }

    @Override
    public String platform() {
        return platform;
    }

    @Override
    public String toString() {
        return "GenericOpenAiClient{platform=" + platform + ", baseUrl=" + currentBaseUrl() + "}";
    }

    /**
     * 直接 new 一个 client（不做缓存）
     */
    public static GenericOpenAiClient create(String platform, String baseUrl, String apiKey,
                                              Long projectId, ObjectMapper objectMapper) {
        AgentProperties.Llm.ProviderConfig cfg = new AgentProperties.Llm.ProviderConfig(baseUrl, null);
        cfg.setApiKey(apiKey);
        cfg.setTimeoutSeconds(120);
        return new GenericOpenAiClient(platform.toUpperCase(), cfg, objectMapper);
    }

    @Component
    public static class Factory {
        private final ObjectMapper objectMapper;
        public Factory(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }
        public GenericOpenAiClient create(String platform, String baseUrl, String apiKey, Long projectId) {
            return GenericOpenAiClient.create(platform, baseUrl, apiKey, projectId, objectMapper);
        }
    }
}
