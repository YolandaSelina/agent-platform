package com.rk.agent.llm.service;

import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.llm.client.LlmClient;
import com.rk.agent.llm.client.LlmClientFactory;
import com.rk.agent.llm.dto.LlmChatRequest;
import com.rk.agent.llm.dto.LlmChatResponse;
import com.rk.agent.llm.entity.ProviderConfigEntity;
import com.rk.agent.llm.resolver.ProviderConfigResolver;
import com.rk.agent.llm.util.KeyEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 服务封装
 * - chat(req)：按 request.platform 走内置客户端
 * - chat(req, projectId)：解析项目级配置，覆盖 API Key / baseUrl / model
 * - chatWithConfig(configId, ..., req)：用数据库中指定配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmClientFactory clientFactory;
    private final ProviderConfigResolver configResolver;
    private final ProviderConfigService providerConfigService;
    private final WebSearchService webSearchService;

    public LlmChatResponse chat(LlmChatRequest request) {
        return chat(request, null);
    }

    public LlmChatResponse chat(LlmChatRequest request, Long projectId) {
        if (request == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "request 不能为空");
        }
        // 解析配置（只把 model 写到 request，不在这里覆盖 client 的 override 字段 —
        // 内置 client 是 Spring singleton，覆盖会污染所有线程；下面的 clientFactory.get
        // 已经会按 projectId 解析并返回/创建带正确 apiKey/baseUrl 的 client）
        applyResolvedConfig(request, projectId);
        // 联网搜索（如果 request 或 DB 配置启用）
        applyWebSearch(request, projectId);
        // 走带 projectId 的工厂方法，自动支持自定义 OpenAI 兼容平台
        LlmClient client = clientFactory.get(request.getPlatform(), request.getModel(), projectId);
        return invoke(client, request);
    }

    /**
     * 用数据库中指定 config 的配置（用于"测试配置"等场景）
     */
    public LlmChatResponse chatWithConfig(Long configId, String overrideModel, Long projectId, LlmChatRequest request) {
        if (request == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "request 不能为空");
        }
        if (configId == null || configId <= 0) {
            return chat(request, projectId);
        }
        ProviderConfigEntity c = providerConfigService.getById(configId);
        if (c == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "配置不存在: " + configId);
        }
        // 用数据库配置覆盖 request
        request.setPlatform(c.getPlatform());
        if (overrideModel != null) request.setModel(overrideModel);
        else if (request.getModel() == null) request.setModel(c.getModel());
        if (c.getTemperature() != null && request.getTemperature() == null) request.setTemperature(c.getTemperature());
        if (c.getMaxTokens() != null && request.getMaxTokens() == null) request.setMaxTokens(c.getMaxTokens());
        // 走带 projectId 的工厂方法（POOL 按 projectId 隔离；不直接 override 字段，避免污染）
        LlmClient client = clientFactory.get(c.getPlatform(), request.getModel(), c.getProjectId());
        return invoke(client, request);
    }

    private void applyResolvedConfig(LlmChatRequest request, Long projectId) {
        // 只做一件事：把 Resolver 解析出的 model 写到 request（如果 request 没设）
        // 不再覆盖 client.overrideApiKey/overrideBaseUrl — 那会污染 Spring singleton 的内置 client
        // 正确的 client 获取/创建由 clientFactory.get(platform, model, projectId) 负责
        ProviderConfigResolver.ResolvedConfig rc = configResolver.resolve(request.getPlatform(), request.getModel(), projectId);
        if (request.getModel() == null) {
            request.setModel(rc.getModel());
        }
        if (request.getMaxTokens() == null && rc.getMaxTokens() != null) {
            request.setMaxTokens(rc.getMaxTokens());
        }
        if (request.getTemperature() == null && rc.getTemperature() != null) {
            request.setTemperature(rc.getTemperature());
        }
    }

    /**
     * 联网搜索：如果 request.webSearchEnabled=true 或 DB 配置 webSearchEnabled=1
     * 提取用户最后一条 user 消息，调用搜索 API，把结果 prepend 到该消息前面
     */
    private void applyWebSearch(LlmChatRequest request, Long projectId) {
        Boolean reqEnabled = request.getWebSearchEnabled();
        // DB 配的搜索配置（直接从 mapper 查）
        ProviderConfigEntity dbCfg = providerConfigService.findBest(request.getPlatform(), request.getModel(), projectId);
        Integer dbEnabled = dbCfg != null ? dbCfg.getWebSearchEnabled() : null;

        boolean enabled = (reqEnabled != null && reqEnabled)
                || (dbEnabled != null && dbEnabled == 1 && (reqEnabled == null || reqEnabled));
        if (!enabled) return;

        // 搜索参数：request 优先 > DB
        String provider = request.getWebSearchProvider() != null ? request.getWebSearchProvider() :
                (dbCfg != null ? dbCfg.getWebSearchProvider() : null);
        String apiKey = request.getWebSearchApiKey();
        if (apiKey == null && dbCfg != null && dbCfg.getWebSearchApiKey() != null) {
            // DB 里是加密的，解密
            try { apiKey = KeyEncryptor.decrypt(dbCfg.getWebSearchApiKey()); } catch (Exception ignored) {}
        }
        String baseUrl = request.getWebSearchBaseUrl() != null ? request.getWebSearchBaseUrl() :
                (dbCfg != null ? dbCfg.getWebSearchBaseUrl() : null);
        int maxResults = request.getWebSearchMaxResults() != null ? request.getWebSearchMaxResults() : 5;

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("联网搜索已启用但 API Key 未配置，跳过: provider={}", provider);
            return;
        }

        // 提取最后一条 user 消息
        List<LlmChatRequest.Message> msgs = request.getMessages();
        if (msgs == null || msgs.isEmpty()) return;
        int lastIdx = msgs.size() - 1;
        while (lastIdx >= 0 && !"user".equals(msgs.get(lastIdx).getRole())) lastIdx--;
        if (lastIdx < 0) return;
        LlmChatRequest.Message lastUser = msgs.get(lastIdx);
        String query = lastUser.getContent();
        if (query == null || query.isBlank()) return;

        log.info("联网搜索: provider={} query={}", provider, query);
        String searchResult = webSearchService.searchAndFormat(query, provider, apiKey, baseUrl, maxResults);
        // prepend 搜索结果到 user 消息
        lastUser.setContent(searchResult + "\n\n【用户问题】\n" + query);
    }

    private LlmChatResponse invoke(LlmClient client, LlmChatRequest request) {
        log.info("LLM 调用: platform={} model={}", request.getPlatform(), request.getModel());
        log.info("request:{}", request);
        LlmChatResponse resp = client.chat(request);
        log.info("resp:{}", resp);
        if (resp.isSuccess()) {
            log.info("LLM 调用成功: latency={}ms tokens={}", resp.getLatencyMs(),
                    resp.getUsage() != null ? resp.getUsage().getTotalTokens() : 0);
        } else {
            log.warn("LLM 调用失败: code={} message={}", resp.getErrorCode(), resp.getErrorMessage());
        }
        return resp;
    }
}
