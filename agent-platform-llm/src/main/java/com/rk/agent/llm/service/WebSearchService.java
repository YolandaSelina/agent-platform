package com.rk.agent.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.llm.entity.ProviderConfigEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 联网搜索服务
 * <p>支持：</p>
 * <ul>
 *   <li>TAVILY — 官方 API，POST /search，返回 JSON {results: [{title, url, content, ...}]}</li>
 *   <li>SERPAPI — POST /search?q=...&api_key=...，返回 {organic_results: [...]}</li>
 *   <li>BING — POST /bing/v7.0/search?q=...，返回 {webPages: {value: [...]}}</li>
 *   <li>CUSTOM — POST {baseUrl}/search，body={q,api_key}，response={results: [{title,url,content}]} 通用协议</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行搜索，返回 Markdown 格式的搜索结果（直接可拼到 LLM prompt）
     * @param query 搜索关键词（建议是用户的原始问题）
     * @param provider TAVILY / SERPAPI / BING / CUSTOM
     * @param apiKey 搜索 API Key
     * @param baseUrl 搜索 base URL
     * @param maxResults 最大结果数（1-10）
     * @return Markdown 字符串（标题 + URL + 摘要），空字符串表示无结果或失败
     */
    public String searchAndFormat(String query, String provider, String apiKey, String baseUrl, int maxResults) {
        if (provider == null || provider.isBlank()) provider = "TAVILY";
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = defaultBaseUrl(provider);
        if (apiKey == null || apiKey.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "联网搜索 API Key 未配置");
        }
        if (maxResults <= 0) maxResults = 5;
        if (maxResults > 10) maxResults = 10;

        try {
            String json = switch (provider.toUpperCase()) {
                case "TAVILY" -> callTavily(query, apiKey, baseUrl, maxResults);
                case "SERPAPI" -> callSerpApi(query, apiKey, baseUrl, maxResults);
                case "BING" -> callBing(query, apiKey, baseUrl, maxResults);
                case "CUSTOM" -> callCustom(query, apiKey, baseUrl, maxResults);
                default -> throw new BizException(ResultCode.BAD_REQUEST, "不支持的搜索 provider: " + provider);
            };
            return formatAsMarkdown(json, provider, maxResults);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("联网搜索失败: provider={} query={}", provider, query, e);
            throw new BizException(ResultCode.LLM_CALL_FAILED, "联网搜索失败: " + e.getMessage());
        }
    }

    /**
     * 从 ProviderConfigEntity 取联网配置
     */
    public boolean isEnabled(ProviderConfigEntity cfg) {
        return cfg != null && cfg.getWebSearchEnabled() != null && cfg.getWebSearchEnabled() == 1;
    }

    private String callTavily(String query, String apiKey, String baseUrl, int maxResults) throws Exception {
        // Tavily: POST {baseUrl}/search  body: {api_key, query, max_results, search_depth}
        String url = trimSlash(baseUrl) + "/search";
        Map<String, Object> body = new HashMap<>();
        body.put("api_key", apiKey);
        body.put("query", query);
        body.put("max_results", maxResults);
        body.put("search_depth", "basic");
        body.put("include_answer", false);
        return postJson(url, body);
    }

    private String callSerpApi(String query, String apiKey, String baseUrl, int maxResults) throws Exception {
        // SerpAPI: GET {baseUrl}/search?q=...&api_key=...&num=...
        String url = trimSlash(baseUrl) + "/search?q=" + java.net.URLEncoder.encode(query, "UTF-8")
                + "&api_key=" + java.net.URLEncoder.encode(apiKey, "UTF-8")
                + "&num=" + maxResults;
        return getJson(url);
    }

    private String callBing(String query, String apiKey, String baseUrl, int maxResults) throws Exception {
        // Bing Search API: GET {baseUrl}/bing/v7.0/search?q=...
        String url = trimSlash(baseUrl) + "/bing/v7.0/search?q=" + java.net.URLEncoder.encode(query, "UTF-8")
                + "&count=" + maxResults;
        Request req = new Request.Builder()
                .url(url)
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .get()
                .build();
        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                throw new BizException(ResultCode.LLM_CALL_FAILED, "Bing 搜索失败: HTTP " + resp.code() + " " + body);
            }
            return body;
        }
    }

    private String callCustom(String query, String apiKey, String baseUrl, int maxResults) throws Exception {
        // 通用协议: POST {baseUrl}/search  body: {q, api_key, max_results}
        String url = trimSlash(baseUrl) + "/search";
        Map<String, Object> body = new HashMap<>();
        body.put("q", query);
        body.put("api_key", apiKey);
        body.put("max_results", maxResults);
        return postJson(url, body);
    }

    private String postJson(String url, Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        Request req = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        try (Response resp = http.newCall(req).execute()) {
            String respBody = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                throw new BizException(ResultCode.LLM_CALL_FAILED, "搜索 API 失败: HTTP " + resp.code() + " " + respBody);
            }
            return respBody;
        }
    }

    private String getJson(String url) throws Exception {
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                throw new BizException(ResultCode.LLM_CALL_FAILED, "搜索 API 失败: HTTP " + resp.code() + " " + body);
            }
            return body;
        }
    }

    /**
     * 把搜索结果 JSON 解析成 Markdown
     */
    private String formatAsMarkdown(String json, String provider, int maxResults) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> items = new ArrayList<>();

            switch (provider.toUpperCase()) {
                case "TAVILY" -> {
                    JsonNode results = root.get("results");
                    if (results != null && results.isArray()) {
                        for (JsonNode r : results) {
                            String title = textOr(r, "title", "(无标题)");
                            String url = textOr(r, "url", "");
                            String content = textOr(r, "content", "");
                            if (content.length() > 300) content = content.substring(0, 300) + "...";
                            items.add(formatItem(title, url, content));
                        }
                    }
                }
                case "SERPAPI" -> {
                    JsonNode results = root.get("organic_results");
                    if (results != null && results.isArray()) {
                        for (JsonNode r : results) {
                            String title = textOr(r, "title", "(无标题)");
                            String url = textOr(r, "link", "");
                            String content = textOr(r, "snippet", "");
                            items.add(formatItem(title, url, content));
                        }
                    }
                }
                case "BING" -> {
                    JsonNode values = root.path("webPages").path("value");
                    if (values.isArray()) {
                        for (JsonNode r : values) {
                            String title = textOr(r, "name", "(无标题)");
                            String url = textOr(r, "url", "");
                            String content = textOr(r, "snippet", "");
                            items.add(formatItem(title, url, content));
                        }
                    }
                }
                case "CUSTOM" -> {
                    JsonNode results = root.get("results");
                    if (results != null && results.isArray()) {
                        for (JsonNode r : results) {
                            String title = textOr(r, "title", "(无标题)");
                            String url = textOr(r, "url", textOr(r, "link", ""));
                            String content = textOr(r, "content", textOr(r, "snippet", ""));
                            if (content.length() > 300) content = content.substring(0, 300) + "...";
                            items.add(formatItem(title, url, content));
                        }
                    } else {
                        // 兜底：把整个 JSON 当文本塞进 prompt
                        items.add("(自定义搜索结果)\n" + (json.length() > 2000 ? json.substring(0, 2000) + "..." : json));
                    }
                }
            }

            if (items.isEmpty()) return "(无搜索结果)";
            StringBuilder sb = new StringBuilder();
            sb.append("以下是从联网搜索得到的最新信息（请基于这些信息回答用户问题）：\n\n");
            for (int i = 0; i < Math.min(items.size(), maxResults); i++) {
                sb.append("【结果 ").append(i + 1).append("】\n").append(items.get(i)).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("解析搜索结果失败: {}", e.getMessage());
            return "(搜索结果解析失败，原文):\n" + (json.length() > 1000 ? json.substring(0, 1000) + "..." : json);
        }
    }

    private String formatItem(String title, String url, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("标题: ").append(title).append("\n");
        if (url != null && !url.isBlank()) sb.append("链接: ").append(url).append("\n");
        if (content != null && !content.isBlank()) sb.append("摘要: ").append(content);
        return sb.toString();
    }

    private String textOr(JsonNode node, String field, String def) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? def : v.asText(def);
    }

    private String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String defaultBaseUrl(String provider) {
        return switch (provider.toUpperCase()) {
            case "TAVILY" -> "https://api.tavily.com";
            case "SERPAPI" -> "https://serpapi.com";
            case "BING" -> "https://api.bing.microsoft.com";
            default -> "https://api.tavily.com";
        };
    }
}
