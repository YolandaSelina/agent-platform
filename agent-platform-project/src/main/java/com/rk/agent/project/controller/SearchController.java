package com.rk.agent.project.controller;

import com.rk.agent.common.result.Result;
import com.rk.agent.project.search.SearchHit;
import com.rk.agent.project.search.SearchQuery;
import com.rk.agent.project.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 全局搜索 API
 * - 跨 ES + Qdrant 聚合
 * - 全文 / 向量 / 混合搜索
 */
@Tag(name = "全局搜索")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final List<SearchService> searchServices;

    @Operation(summary = "全文搜索（聚合所有引擎）")
    @GetMapping("/fulltext")
    public Result<Map<String, Object>> fulltext(
            @RequestParam String q,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String index,
            @RequestParam(defaultValue = "20") Integer size) {
        SearchQuery query = SearchQuery.builder()
                .index(index != null ? index : "documents")
                .projectId(projectId)
                .size(size)
                .build();
        Map<String, Object> result = new HashMap<>();
        List<SearchHit> all = new ArrayList<>();
        Map<String, Integer> byEngine = new HashMap<>();
        for (SearchService s : searchServices) {
            if (!s.enabled()) continue;
            try {
                List<SearchHit> hits = s.fullTextSearch(q, query);
                all.addAll(hits);
                byEngine.merge(s.getClass().getSimpleName(), hits.size(), Integer::sum);
            } catch (Exception ignored) {
            }
        }
        // 排序：按 score 降序去重
        Map<String, SearchHit> dedup = new HashMap<>();
        for (SearchHit h : all) {
            if (!dedup.containsKey(h.getId()) || (dedup.get(h.getId()).getScore() == null || h.getScore() == null
                    || h.getScore() > dedup.get(h.getId()).getScore())) {
                dedup.put(h.getId(), h);
            }
        }
        List<SearchHit> sorted = new ArrayList<>(dedup.values());
        sorted.sort((a, b) -> Double.compare(
                b.getScore() == null ? 0 : b.getScore(),
                a.getScore() == null ? 0 : a.getScore()));
        if (sorted.size() > size) sorted = sorted.subList(0, size);
        result.put("hits", sorted);
        result.put("total", sorted.size());
        result.put("byEngine", byEngine);
        return Result.success(result);
    }

    @Operation(summary = "向量搜索（Qdrant）")
    @PostMapping("/vector")
    public Result<List<SearchHit>> vector(
            @RequestBody Map<String, Object> body) {
        // body: { "vector": [0.1, 0.2, ...], "projectId": 1, "index": "documents", "size": 20 }
        @SuppressWarnings("unchecked")
        List<Number> raw = (List<Number>) body.get("vector");
        if (raw == null) return Result.error(400, "vector 不能为空");
        float[] vec = new float[raw.size()];
        for (int i = 0; i < raw.size(); i++) vec[i] = raw.get(i).floatValue();
        SearchQuery query = SearchQuery.builder()
                .index((String) body.getOrDefault("index", "documents"))
                .projectId(((Number) body.get("projectId")).longValue())
                .size(((Number) body.getOrDefault("size", 20)).intValue())
                .build();
        for (SearchService s : searchServices) {
            if (!s.enabled()) continue;
            try {
                List<SearchHit> hits = s.vectorSearch(vec, query);
                if (!hits.isEmpty()) {
                    return Result.success(hits);
                }
            } catch (Exception ignored) {
            }
        }
        return Result.success(List.of());
    }

    @Operation(summary = "搜索服务状态")
    @GetMapping("/status")
    public Result<Map<String, Boolean>> status() {
        Map<String, Boolean> map = new HashMap<>();
        for (SearchService s : searchServices) {
            map.put(s.getClass().getSimpleName(), s.enabled());
        }
        return Result.success(map);
    }

    @Operation(summary = "已注册的搜索引擎列表")
    @GetMapping("/engines")
    public Result<List<String>> engines() {
        List<String> list = new ArrayList<>();
        for (SearchService s : searchServices) {
            list.add(s.getClass().getSimpleName());
        }
        return Result.success(list);
    }
}
