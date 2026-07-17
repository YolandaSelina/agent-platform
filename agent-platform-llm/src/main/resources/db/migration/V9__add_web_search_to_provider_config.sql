-- ============================================================
-- V9 LLM 配置表加联网搜索字段
-- ============================================================
ALTER TABLE llm_provider_config
    ADD COLUMN web_search_enabled   TINYINT      DEFAULT 0                       COMMENT '是否启用联网搜索',
    ADD COLUMN web_search_provider  VARCHAR(50)  DEFAULT 'TAVILY'                COMMENT '搜索 provider: TAVILY / SERPAPI / BING / CUSTOM',
    ADD COLUMN web_search_api_key   VARCHAR(500) DEFAULT NULL                    COMMENT '搜索 API Key（加密）',
    ADD COLUMN web_search_base_url  VARCHAR(500) DEFAULT 'https://api.tavily.com' COMMENT '搜索 API base URL';
