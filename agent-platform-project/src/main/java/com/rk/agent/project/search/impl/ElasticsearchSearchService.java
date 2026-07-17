package com.rk.agent.project.search.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.project.search.SearchDoc;
import com.rk.agent.project.search.SearchHit;
import com.rk.agent.project.search.SearchQuery;
import com.rk.agent.project.search.SearchService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Elasticsearch 全文搜索实现
 * <p>
 * 走 HTTP REST API（_bulk / _search）
 * <p>
 * 启用：agent.search.elasticsearch.enabled=true
 * 端口默认 9200
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "agent.search.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchSearchService implements SearchService {

    @Value("${agent.search.elasticsearch.base-url:http://localhost:9200}")
    private String baseUrl;

    @Value("${agent.search.elasticsearch.default-index:agent_documents}")
    private String defaultIndex;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void indexDocument(SearchDoc doc) {
        try {
            if (!indexExists(doc.getIndex())) {
                createIndex(doc.getIndex());
            }
            Map<String, Object> body = new HashMap<>();
            body.put("title", doc.getTitle());
            body.put("content", doc.getContent());
            body.put("type", doc.getType());
            body.put("tags", doc.getTags());
            body.put("projectId", doc.getProjectId());
            body.put("taskId", doc.getTaskId());
            body.put("timestamp", doc.getTimestamp() != null ? doc.getTimestamp() : System.currentTimeMillis());
            if (doc.getMeta() != null) body.put("meta", doc.getMeta());
            String json = objectMapper.writeValueAsString(body);
            Request req = new Request.Builder()
                    .url(baseUrl + "/" + doc.getIndex() + "/_doc/" + doc.getId() + "?refresh=wait_for")
                    .header("Content-Type", "application/json")
                    .put(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("ES 索引失败: {} - {}", resp.code(), resp.body() != null ? resp.body().string() : "");
                }
            }
        } catch (Exception e) {
            log.warn("ES 索引异常: {}", e.getMessage());
        }
    }

    @Override
    public void indexDocuments(List<SearchDoc> docs) {
        try {
            if (docs == null || docs.isEmpty()) return;
            String index = docs.get(0).getIndex();
            if (!indexExists(index)) {
                createIndex(index);
            }
            StringBuilder ndjson = new StringBuilder();
            for (SearchDoc d : docs) {
                Map<String, Object> action = Map.of("index", Map.of("_index", d.getIndex(), "_id", d.getId()));
                Map<String, Object> doc = new HashMap<>();
                doc.put("title", d.getTitle());
                doc.put("content", d.getContent());
                doc.put("type", d.getType());
                doc.put("tags", d.getTags());
                doc.put("projectId", d.getProjectId());
                doc.put("taskId", d.getTaskId());
                doc.put("timestamp", d.getTimestamp() != null ? d.getTimestamp() : System.currentTimeMillis());
                if (d.getMeta() != null) doc.put("meta", d.getMeta());
                ndjson.append(objectMapper.writeValueAsString(action)).append("\n");
                ndjson.append(objectMapper.writeValueAsString(doc)).append("\n");
            }
            Request req = new Request.Builder()
                    .url(baseUrl + "/_bulk?refresh=wait_for")
                    .header("Content-Type", "application/x-ndjson")
                    .post(RequestBody.create(ndjson.toString(), MediaType.parse("application/x-ndjson")))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("ES 批量索引失败: {}", resp.code());
                }
            }
        } catch (Exception e) {
            log.warn("ES 批量索引异常: {}", e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String index, String id) {
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/" + index + "/_doc/" + id + "?refresh=wait_for")
                    .delete()
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("ES 删除失败: {} - {}", resp.code(), resp.message());
                }
            }
        } catch (Exception e) {
            log.warn("ES 删除异常: {}", e.getMessage());
        }
    }

    @Override
    public List<SearchHit> fullTextSearch(String keyword, SearchQuery query) {
        try {
            String index = query.getIndex() != null ? query.getIndex() : defaultIndex;
            if (!indexExists(index)) {
                return List.of();
            }
            Map<String, Object> body = new HashMap<>();
            body.put("size", query.getSize());
            body.put("from", query.getFrom());

            // match 查询
            Map<String, Object> match = new HashMap<>();
            match.put("query", keyword != null ? keyword : "");
            match.put("fields", List.of("title^3", "content", "tags^2"));
            match.put("type", "best_fields");
            Map<String, Object> queryBody = Map.of(
                    "multi_match", match
            );

            // 项目/类型/任务过滤
            List<Map<String, Object>> filters = new ArrayList<>();
            if (query.getProjectId() != null) {
                filters.add(Map.of("term", Map.of("projectId", query.getProjectId())));
            }
            if (query.getTaskId() != null) {
                filters.add(Map.of("term", Map.of("taskId", query.getTaskId())));
            }
            if (query.getType() != null) {
                filters.add(Map.of("term", Map.of("type", query.getType())));
            }
            if (!filters.isEmpty()) {
                Map<String, Object> bool = new HashMap<>();
                bool.put("must", List.of(queryBody));
                bool.put("filter", filters);
                body.put("query", Map.of("bool", bool));
            } else {
                body.put("query", queryBody);
            }

            // 高亮
            body.put("highlight", Map.of(
                    "fields", Map.of("content", Map.of("fragment_size", 200, "number_of_fragments", 1))
            ));

            String json = objectMapper.writeValueAsString(body);
            Request req = new Request.Builder()
                    .url(baseUrl + "/" + index + "/_search")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    log.warn("ES 搜索失败: {} - {}", resp.code(), respBody);
                    return List.of();
                }
                return parseResponse(respBody);
            }
        } catch (Exception e) {
            log.warn("ES 搜索异常: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<SearchHit> vectorSearch(float[] queryVector, SearchQuery query) {
        // ES 全文模式不支持原生向量（如要支持需安装 k-NN 插件）
        return List.of();
    }

    @Override
    public List<SearchHit> hybridSearch(String keyword, float[] queryVector, SearchQuery query) {
        // 第一批：只走全文
        return fullTextSearch(keyword, query);
    }

    @Override
    public boolean indexExists(String index) {
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/" + index)
                    .head()
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                return resp.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean enabled() {
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/_cluster/health")
                    .get()
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                return resp.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ---- private ----

    private void createIndex(String name) {
        try {
            Map<String, Object> settings = Map.of(
                    "number_of_shards", 1,
                    "number_of_replicas", 0
            );
            Map<String, Object> mappings = Map.of(
                    "properties", Map.of(
                            "title", Map.of("type", "text", "analyzer", "standard"),
                            "content", Map.of("type", "text", "analyzer", "standard"),
                            "type", Map.of("type", "keyword"),
                            "tags", Map.of("type", "keyword"),
                            "projectId", Map.of("type", "long"),
                            "taskId", Map.of("type", "long"),
                            "timestamp", Map.of("type", "date")
                    )
            );
            Map<String, Object> body = new HashMap<>();
            body.put("settings", settings);
            body.put("mappings", mappings);
            String json = objectMapper.writeValueAsString(body);
            Request req = new Request.Builder()
                    .url(baseUrl + "/" + name)
                    .header("Content-Type", "application/json")
                    .put(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("ES 创建索引失败: {} - {}", resp.code(), resp.body() != null ? resp.body().string() : "");
                }
            }
        } catch (Exception e) {
            log.warn("ES 创建索引异常: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<SearchHit> parseResponse(String body) throws IOException {
        Map<String, Object> root = objectMapper.readValue(body, Map.class);
        Map<String, Object> hitsRoot = (Map<String, Object>) root.getOrDefault("hits", Map.of());
        List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsRoot.getOrDefault("hits", List.of());
        List<SearchHit> result = new ArrayList<>();
        for (Map<String, Object> h : hits) {
            Map<String, Object> source = (Map<String, Object>) h.getOrDefault("_source", Map.of());
            Map<String, Object> highlight = (Map<String, Object>) h.getOrDefault("highlight", Map.of());
            String snippet = null;
            if (highlight.get("content") instanceof List<?> list && !list.isEmpty()) {
                snippet = list.get(0).toString();
            }
            result.add(SearchHit.builder()
                    .id((String) h.get("_id"))
                    .index((String) h.get("_index"))
                    .type((String) source.get("type"))
                    .title((String) source.get("title"))
                    .snippet(snippet != null ? snippet : truncate((String) source.get("content"), 200))
                    .score(((Number) h.get("_score")).doubleValue())
                    .source(source)
                    .build());
        }
        return result;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
