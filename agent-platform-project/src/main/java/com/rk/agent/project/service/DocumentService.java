package com.rk.agent.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rk.agent.project.entity.Document;

import java.util.List;
import java.util.Map;

public interface DocumentService extends IService<Document> {

    /**
     * upsert：按 (projectId, docType, stage, runId/nodeRunId) 唯一性创建或更新文档
     * 并写入版本历史
     */
    Long upsert(Document doc);

    /**
     * 列出项目下所有文档
     */
    List<Document> listByProject(Long projectId, String docType);

    /**
     * 文档分页
     */
    IPage<Document> page(Long current, Long size, Long projectId, String docType, String stage);

    /**
     * 文档详情
     */
    Document detail(Long id);

    /**
     * 文档版本历史
     */
    List<Map<String, Object>> listVersions(Long id);

    /**
     * 文档统计（按类型）
     */
    Map<String, Object> statistics(Long projectId);
}
