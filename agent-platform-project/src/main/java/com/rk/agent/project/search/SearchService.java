package com.rk.agent.project.search;

import java.util.List;
import java.util.Map;

/**
 * 搜索引擎抽象
 * - 索引文档
 * - 删除文档
 * - 全文搜索（中文友好）
 * - 向量搜索（语义）
 * - 混合搜索
 */
public interface SearchService {

    /**
     * 索引一个文档到全文搜索引擎
     */
    void indexDocument(SearchDoc doc);

    /**
     * 批量索引
     */
    void indexDocuments(List<SearchDoc> docs);

    /**
     * 删除文档
     */
    void deleteDocument(String index, String id);

    /**
     * 全文搜索
     */
    List<SearchHit> fullTextSearch(String keyword, SearchQuery query);

    /**
     * 向量搜索（基于查询向量）
     */
    List<SearchHit> vectorSearch(float[] queryVector, SearchQuery query);

    /**
     * 混合搜索（全文 + 向量）
     */
    List<SearchHit> hybridSearch(String keyword, float[] queryVector, SearchQuery query);

    /**
     * 索引是否存在
     */
    boolean indexExists(String index);

    /**
     * 文档是否启用
     */
    boolean enabled();
}
