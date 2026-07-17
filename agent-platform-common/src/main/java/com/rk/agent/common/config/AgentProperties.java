package com.rk.agent.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 平台通用配置
 */
@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** JWT 配置 */
    private Jwt jwt = new Jwt();
    /** SSE 配置 */
    private Sse sse = new Sse();
    /** LLM 适配器配置 */
    private Llm llm = new Llm();
    /** 文件存储根目录 */
    private String storageRoot = "./data/storage";
    /** Pipeline 引擎配置 */
    private Pipeline pipeline = new Pipeline();

    @Data
    @NoArgsConstructor
    public static class Jwt {
        private String secret = "agent-platform-default-secret-please-change-in-production-32bytes";
        private Long expire = 86400L;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Data
    @NoArgsConstructor
    public static class Sse {
        private Long timeoutMs = 600_000L;
        private Integer bufferSize = 1024;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Llm {
        private ProviderConfig openai = new ProviderConfig("https://api.openai.com/v1", "gpt-4o");
        private ProviderConfig anthropic = new ProviderConfig("https://api.anthropic.com", "claude-3-5-sonnet-20241022");
        private ProviderConfig qwen = new ProviderConfig("https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus");
        private ProviderConfig deepseek = new ProviderConfig("https://api.deepseek.com/v1", "deepseek-chat");
        private ProviderConfig siliconflow = new ProviderConfig("https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-72B-Instruct");

        @Data
        @NoArgsConstructor
        public static class ProviderConfig {
            private String apiKey = "";
            private String baseUrl;
            private String defaultModel;
            private Integer timeoutSeconds = 120;

            public ProviderConfig(String baseUrl, String defaultModel) {
                this.baseUrl = baseUrl;
                this.defaultModel = defaultModel;
            }
        }
    }

    @Data
    @NoArgsConstructor
    public static class Pipeline {
        /** 节点默认超时（秒） */
        private Integer defaultNodeTimeoutSeconds = 600;
        /** 投票节点最少 Agent 数 */
        private Integer minVoteAgents = 2;
        /** 投票节点默认 Agent 数 */
        private Integer defaultVoteAgents = 3;
        /** Pipeline 单次最大节点数 */
        private Integer maxNodesPerPipeline = 50;
        /** 重试次数 */
        private Integer maxRetryCount = 3;
    }
}
