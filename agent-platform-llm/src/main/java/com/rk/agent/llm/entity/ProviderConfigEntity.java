package com.rk.agent.llm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * LLM 提供商配置（页面配置）
 * 唯一键：(platform, model, project_id)
 * project_id = NULL 表示系统级默认
 * model = NULL 表示平台下所有模型共用
 */
@Data
@TableName("llm_provider_config")
public class ProviderConfigEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String platform;
    private String model;

    @TableField("project_id")
    private Long projectId;

    /** 加密存储的 API Key */
    @TableField("api_key")
    private String apiKey;

    @TableField("base_url")
    private String baseUrl;

    private Double temperature;

    @TableField("max_tokens")
    private Integer maxTokens;

    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    private Integer enabled;

    private Integer priority;

    private String description;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("creator_name")
    private String creatorName;

    /** 联网搜索：是否启用 */
    @TableField("web_search_enabled")
    private Integer webSearchEnabled;

    /** 联网搜索 provider: TAVILY / SERPAPI / BING / CUSTOM */
    @TableField("web_search_provider")
    private String webSearchProvider;

    /** 联网搜索 API Key（加密） */
    @TableField("web_search_api_key")
    private String webSearchApiKey;

    /** 联网搜索 base URL */
    @TableField("web_search_base_url")
    private String webSearchBaseUrl;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    private Integer deleted;
}
