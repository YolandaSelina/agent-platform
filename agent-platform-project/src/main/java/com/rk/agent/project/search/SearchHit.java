package com.rk.agent.project.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 搜索命中结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHit implements Serializable {

    /** 文档 ID */
    private String id;
    /** 索引 */
    private String index;
    /** 类型 */
    private String type;
    /** 标题 */
    private String title;
    /** 内容片段（高亮） */
    private String snippet;
    /** 得分 */
    private Double score;
    /** 原始数据 */
    private Map<String, Object> source;
}
