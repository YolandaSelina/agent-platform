package com.rk.agent.project.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 搜索查询条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchQuery implements Serializable {

    /** 索引（为空则搜所有） */
    private String index;
    /** 项目 ID 过滤 */
    private Long projectId;
    /** 任务 ID 过滤 */
    private Long taskId;
    /** 文档类型过滤 */
    private String type;
    /** 标签过滤 */
    private List<String> tags;
    /** 返回数量 */
    @Builder.Default
    private Integer size = 20;
    /** 起始偏移（分页） */
    @Builder.Default
    private Integer from = 0;
}
