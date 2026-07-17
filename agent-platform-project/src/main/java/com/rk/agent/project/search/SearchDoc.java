package com.rk.agent.project.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 搜索引擎中的文档
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDoc implements Serializable {

    /** 索引（document/task/project_page...） */
    private String index;
    /** 文档 ID */
    private String id;
    /** 文档类型（REQUIREMENT/PRODUCT/...） */
    private String type;
    /** 标题 */
    private String title;
    /** 内容 */
    private String content;
    /** 标签 */
    private String tags;
    /** 项目 ID */
    private Long projectId;
    /** 任务 ID */
    private Long taskId;
    /** 时间戳 */
    private Long timestamp;
    /** 向量（可选） */
    private float[] vector;
    /** 额外元数据 */
    private Map<String, Object> meta;
}
