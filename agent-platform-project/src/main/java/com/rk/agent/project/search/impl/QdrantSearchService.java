package com.rk.agent.project.search.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.common.util.JsonUtils;
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
 * Qdrant 向量搜索实现
 * <p>
 * Qdrant HTTP API 文档：https://qdrant.tech/documentation/quick_start/
 * <p>
 * 启用方式：agent.search.qdrant.enabled=true
 * 端口默认 6333（HTTP），GRPC 6334
 * <p>
 * 本实现走 HTTP REST
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "agent.search.qdrant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QdrantSearchService implements SearchService {

    @Value("${agent.search.qdrant.base-url:http://localhost:6333}")
    private String baseUrl;

    @Value("${agent.search.qdrant.collection:agent_documents}")
    private String defaultCollection;

    @Value("${agent.search.qdrant.vector-size:1536}")
    private Integer vectorSize;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void indexDocument(SearchDoc doc) {
        if (!indexExists(doc.getIndex())) {
            createCollection(doc.getIndex());
        }
        try {
            Map<String, Object> point = new HashMap<>();
            point.put("id", stringToLongId(doc.getId()));
            point.put("payload", toPayload(doc));
            if (doc.getVector() != null) {
                point.put("vector", toFloatList(doc.getVector()));
            } else {
                // 没有向量就用零向量占位（生产应该走 embedding）
                point.put("vector", zeroVector());
            }
            Map<String, Object> body = Map.of("points", List.of(point));
            String json = objectMapper.writeValueAsString(body);
            Request req = new Request.Builder()
                    .url(baseUrl + "/collections/" + doc.getIndex() + "/points?wait=true")
                    .header("Content-Type", "application/json")
                    .put(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    log.warn("Qdrant 索引失败: {} - {}", resp.code(), respBody);
                }
            }
        } catch (Exception e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "Qdrant 索引失败: " + e.getMessage());
        }
    }

    @Override
    public void indexDocuments(List<SearchDoc> docs) {
        for (SearchDoc d : docs) {
            indexDocument(d);
        }
    }

    @Override
    public void deleteDocument(String index, String id) {
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/collections/" + index + "/points/" + stringToLongId(id))
                    .delete()
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("Qdrant 删除失败: {} - {}", resp.code(), resp.message());
                }
            }
        } catch (Exception e) {
            log.warn("Qdrant 删除异常: {}", e.getMessage());
        }
    }

    @Override
    public List<SearchHit> fullTextSearch(String keyword, SearchQuery query) {
        // Qdrant 没有原生全文搜索，使用 payload 过滤
        return vectorSearch(null, query);
    }

    @Override
    public List<SearchHit> vectorSearch(float[] queryVector, SearchQuery query) {
        if (queryVector == null) {
            // 没有查询向量时返回空
            return List.of();
        }
        try {
            String index = query.getIndex() != null ? query.getIndex() : defaultCollection;
            Map<String, Object> body = new HashMap<>();
            body.put("vector", toFloatList(queryVector));
            body.put("limit", query.getSize());
            body.put("offset", query.getFrom());
            body.put("with_payload", true);
            // 项目过滤
            if (query.getProjectId() != null) {
                body.put("filter", Map.of("must", List.of(
                        Map.of("key", "projectId", "match", Map.of("value", query.getProjectId()))
                )));
            }
            String json = objectMapper.writeValueAsString(body);
            Request req = new Request.Builder()
                    .url(baseUrl + "/collections/" + index + "/points/search")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    log.warn("Qdrant 搜索失败: {} - {}", resp.code(), respBody);
                    return List.of();
                }
                return parseSearchResponse(respBody);
            }
        } catch (Exception e) {
            log.warn("Qdrant 搜索异常: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<SearchHit> hybridSearch(String keyword, float[] queryVector, SearchQuery query) {
        // 第一批：直接走 vector（Qdrant 全文要 RRF 排序，第一批简化）
        return vectorSearch(queryVector, query);
    }

    @Override
    public boolean indexExists(String index) {
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/collections/" + index)
                    .get()
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
        // Qdrant 健康检查：尝试 /collections 列表（健康且可访问）
        // （Qdrant 1.x 没有 /health 端点，根路径是仪表板 UI；用 /collections 判断最稳）
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/collections")
                    .get()
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                return resp.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ---- private helpers ----

    private void createCollection(String name) {
        try {
            Map<String, Object> body = Map.of(
                    "vectors", Map.of("size", vectorSize, "distance", "Cosine")
            );
            String json = objectMapper.writeValueAsString(body);
            Request req = new Request.Builder()
                    .url(baseUrl + "/collections/" + name)
                    .header("Content-Type", "application/json")
                    .put(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("Qdrant 创建 collection 失败: {} - {}", resp.code(), resp.body() != null ? resp.body().string() : "");
                }
            }
        } catch (Exception e) {
            log.warn("Qdrant 创建 collection 异常: {}", e.getMessage());
        }
    }

    private Map<String, Object> toPayload(SearchDoc doc) {
        Map<String, Object> p = new HashMap<>();
        p.put("title", doc.getTitle());
        p.put("content", doc.getContent());
        p.put("type", doc.getType());
        p.put("tags", doc.getTags());
        p.put("projectId", doc.getProjectId());
        p.put("taskId", doc.getTaskId());
        p.put("timestamp", doc.getTimestamp());
        if (doc.getMeta() != null) p.put("meta", doc.getMeta());
        return p;
    }

    @SuppressWarnings("unchecked")
    private List<SearchHit> parseSearchResponse(String body) throws IOException {
        Map<String, Object> root = objectMapper.readValue(body, Map.class);
        List<Map<String, Object>> result = (List<Map<String, Object>>) root.getOrDefault("result", List.of());
        List<SearchHit> hits = new ArrayList<>();
        for (Map<String, Object> r : result) {
            Map<String, Object> payload = (Map<String, Object>) r.getOrDefault("payload", Map.of());
            hits.add(SearchHit.builder()
                    .id(String.valueOf(r.get("id")))
                    .index((String) payload.get("index"))
                    .type((String) payload.get("type"))
                    .title((String) payload.get("title"))
                    .snippet(truncate((String) payload.get("content"), 200))
                    .score(((Number) r.get("score")).doubleValue())
                    .source(payload)
                    .build());
        }
        return hits;
    }

    private long stringToLongId(String id) {
        if (id == null) return 0L;
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            // 简单 hash
            return Math.abs((long) id.hashCode());
        }
    }

    private List<Float> toFloatList(float[] v) {
        List<Float> list = new ArrayList<>(v.length);
        for (float f : v) list.add(f);
        return list;
    }

    private List<Float> zeroVector() {
        List<Float> v = new ArrayList<>(vectorSize);
        for (int i = 0; i < vectorSize; i++) v.add(0.0f);
        return v;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
