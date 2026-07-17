package com.rk.agent.llm.resolver;

import com.rk.agent.common.config.AgentProperties;
import com.rk.agent.common.util.JsonUtils;
import com.rk.agent.llm.entity.ProviderConfigEntity;
import com.rk.agent.llm.mapper.ProviderConfigMapper;
import com.rk.agent.llm.util.KeyEncryptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 解析 LLM 配置（优先级：页面配置 > yml 默认）
 * <p>
 * 查找顺序：
 *   1. 项目级 + 平台 + 精确模型
 *   2. 项目级 + 平台 + 模型=NULL
 *   3. 系统级 + 平台 + 精确模型
 *   4. 系统级 + 平台 + 模型=NULL
 *   5. 都没有 → 用 yml 默认（AgentProperties）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderConfigResolver {

    private final AgentProperties agentProperties;
    private final ProviderConfigMapper configMapper;

    @Value("${agent.jwt.secret:agent-platform-default-secret-please-change-in-production-32bytes}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        KeyEncryptor.init(jwtSecret);
        log.info("ProviderConfigResolver 初始化完成");
    }

    /**
     * 解析生效的配置
     * @param platform OPENAI/ANTHROPIC/QWEN/DEEPSEEK/SILICONFLOW/自定义
     * @param model 可选，指定模型
     * @param projectId 可选，项目 ID（NULL 表示系统级）
     * @return ResolvedConfig（已合并的最终配置）
     */
    public ResolvedConfig resolve(String platform, String model, Long projectId) {
        AgentProperties.Llm.ProviderConfig defaultCfg = getDefaultFromYml(platform);

        // 数据库查找
        try {
            ProviderConfigEntity best = configMapper.findBestMatch(platform, model, projectId);
            if (best != null) {
                String apiKey = KeyEncryptor.decrypt(best.getApiKey());
                return ResolvedConfig.builder()
                        .platform(platform)
                        .model(model != null ? model : best.getModel())
                        .apiKey(apiKey)
                        .baseUrl(best.getBaseUrl() != null ? best.getBaseUrl() :
                                (defaultCfg != null ? defaultCfg.getBaseUrl() : null))
                        .temperature(best.getTemperature() != null ? best.getTemperature() :
                                (defaultCfg != null ? defaultCfg.getTimeoutSeconds() != null ? 0.7 : 0.7 : 0.7))
                        .maxTokens(best.getMaxTokens())
                        .timeoutSeconds(best.getTimeoutSeconds() != null ? best.getTimeoutSeconds() :
                                (defaultCfg != null ? defaultCfg.getTimeoutSeconds() : 120))
                        .source("DB")
                        .configId(best.getId())
                        .build();
            }
        } catch (Exception e) {
            log.warn("查询 LLM 配置失败: {}", e.getMessage());
        }

        // 都没有，用 yml
        if (defaultCfg == null) {
            return ResolvedConfig.builder()
                    .platform(platform).model(model)
                    .apiKey(null).baseUrl(null)
                    .temperature(0.7).maxTokens(null).timeoutSeconds(120)
                    .source("MISSING")
                    .build();
        }
        return ResolvedConfig.builder()
                .platform(platform)
                .model(model != null ? model : defaultCfg.getDefaultModel())
                .apiKey(defaultCfg.getApiKey())
                .baseUrl(defaultCfg.getBaseUrl())
                .temperature(0.7)
                .maxTokens(null)
                .timeoutSeconds(defaultCfg.getTimeoutSeconds() != null ? defaultCfg.getTimeoutSeconds() : 120)
                .source("YML")
                .build();
    }

    private AgentProperties.Llm.ProviderConfig getDefaultFromYml(String platform) {
        if (platform == null) return null;
        return switch (platform.toUpperCase()) {
            case "OPENAI" -> agentProperties.getLlm().getOpenai();
            case "ANTHROPIC" -> agentProperties.getLlm().getAnthropic();
            case "QWEN" -> agentProperties.getLlm().getQwen();
            case "DEEPSEEK" -> agentProperties.getLlm().getDeepseek();
            case "SILICONFLOW" -> agentProperties.getLlm().getSiliconflow();
            default -> null;  // 自定义平台没有 yml 默认
        };
    }

    @lombok.Data
    @lombok.Builder
    public static class ResolvedConfig {
        private String platform;
        private String model;
        private String apiKey;
        private String baseUrl;
        private Double temperature;
        private Integer maxTokens;
        private Integer timeoutSeconds;
        /** DB / YML / MISSING */
        private String source;
        private Long configId;
    }
}
