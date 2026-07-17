package com.rk.agent.llm.dto;

import com.rk.agent.llm.entity.ProviderConfigEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * LLM 提供商配置保存 DTO
 * <p>注意：apiKey 允许明文传入，service 层负责加密存储</p>
 */
@Data
@Schema(description = "LLM 提供商配置保存 DTO")
public class ProviderConfigSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（编辑时必传，新建时为空）")
    private Long id;

    private String name;

    @NotBlank(message = "平台不能为空")
    private String platform; // OPENAI/QWEN/DEEPSEEK/ANTHROPIC/SILICONFLOW/CUSTOM

    private String model; // 留空表示该平台下所有模型共用

    @Schema(description = "项目 ID（NULL = 系统级默认）")
    private Long projectId;

    @Schema(description = "API Key（明文，service 层会加密）")
    private String apiKey;

    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Integer enabled;
    private Integer priority;
    private String description;

    // ===== 联网搜索配置 =====
    @Schema(description = "是否启用联网搜索")
    private Integer webSearchEnabled;

    @Schema(description = "搜索 provider: TAVILY / SERPAPI / BING / CUSTOM")
    private String webSearchProvider;

    @Schema(description = "搜索 API Key（明文，service 层加密）")
    private String webSearchApiKey;

    @Schema(description = "搜索 base URL")
    private String webSearchBaseUrl;

    public ProviderConfigEntity toEntity() {
        ProviderConfigEntity e = new ProviderConfigEntity();
        e.setName(this.name);
        e.setPlatform(this.platform);
        e.setModel(this.model);
        e.setProjectId(this.projectId);
        e.setApiKey(this.apiKey);
        e.setBaseUrl(this.baseUrl);
        e.setTemperature(this.temperature);
        e.setMaxTokens(this.maxTokens);
        e.setTimeoutSeconds(this.timeoutSeconds);
        e.setEnabled(this.enabled);
        e.setPriority(this.priority);
        e.setDescription(this.description);
        e.setWebSearchEnabled(this.webSearchEnabled);
        e.setWebSearchProvider(this.webSearchProvider);
        e.setWebSearchApiKey(this.webSearchApiKey);
        e.setWebSearchBaseUrl(this.webSearchBaseUrl);
        return e;
    }
}
