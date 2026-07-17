package com.rk.agent.project.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.project.entity.Document;
import com.rk.agent.project.entity.DocumentVersion;
import com.rk.agent.project.mapper.DocumentMapper;
import com.rk.agent.project.mapper.DocumentVersionMapper;
import com.rk.agent.project.search.SearchDoc;
import com.rk.agent.project.search.SearchService;
import com.rk.agent.project.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    private final DocumentVersionMapper documentVersionMapper;
    /** 所有 SearchService 实现（ES / Qdrant / 内存） */
    private final List<SearchService> searchServices;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upsert(Document doc) {
        if (doc.getProjectId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "projectId 不能为空");
        }
        if (StrUtil.isBlank(doc.getDocType())) {
            throw new BizException(ResultCode.BAD_REQUEST, "docType 不能为空");
        }
        if (StrUtil.isBlank(doc.getFormat())) {
            doc.setFormat("MARKDOWN");
        }
        if (doc.getStatus() == null) {
            doc.setStatus("DRAFT");
        }
        // 查找是否已存在（按 projectId + docType + stage + runId 唯一）
        LambdaQueryWrapper<Document> qw = new LambdaQueryWrapper<Document>()
                .eq(Document::getProjectId, doc.getProjectId())
                .eq(Document::getDocType, doc.getDocType())
                .eq(StrUtil.isNotBlank(doc.getStage()), Document::getStage, doc.getStage())
                .eq(doc.getPipelineRunId() != null, Document::getPipelineRunId, doc.getPipelineRunId())
                .eq(doc.getNodeRunId() != null, Document::getNodeRunId, doc.getNodeRunId())
                .orderByDesc(Document::getUpdateTime)
                .last("LIMIT 1");
        Document existing = getOne(qw, false);
        int newVersion = 1;
        if (existing != null) {
            newVersion = (existing.getVersion() == null ? 1 : existing.getVersion()) + 1;
            existing.setTitle(doc.getTitle() != null ? doc.getTitle() : existing.getTitle());
            existing.setContent(doc.getContent() != null ? doc.getContent() : existing.getContent());
            existing.setStatus(doc.getStatus() != null ? doc.getStatus() : existing.getStatus());
            existing.setFormat(doc.getFormat() != null ? doc.getFormat() : existing.getFormat());
            existing.setTags(doc.getTags() != null ? doc.getTags() : existing.getTags());
            existing.setVersion(newVersion);
            updateById(existing);
            doc = existing;
        } else {
            doc.setVersion(newVersion);
            save(doc);
        }
        // 写版本历史
        DocumentVersion ver = new DocumentVersion();
        ver.setDocumentId(doc.getId());
        ver.setVersion(newVersion);
        ver.setContent(doc.getContent());
        ver.setCreatorId(doc.getCreatorId());
        ver.setCreatorName(doc.getCreatorName());
        ver.setDiffSummary("Version " + newVersion);
        documentVersionMapper.insert(ver);

        // 同步到搜索引擎（ES / Qdrant）
        syncToSearchEngines(doc);

        return doc.getId();
    }

    private void syncToSearchEngines(Document doc) {
        if (searchServices == null || searchServices.isEmpty()) return;
        SearchDoc sd = SearchDoc.builder()
                .index("documents")
                .id(String.valueOf(doc.getId()))
                .type(doc.getDocType())
                .title(doc.getTitle())
                .content(doc.getContent())
                .tags(doc.getTags())
                .projectId(doc.getProjectId())
                .taskId(doc.getTaskId())
                .timestamp(System.currentTimeMillis())
                .build();
        for (SearchService s : searchServices) {
            try {
                if (s.enabled()) {
                    s.indexDocument(sd);
                }
            } catch (Exception e) {
                log.warn("同步到搜索引擎 [{}] 失败: {}", s.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public List<Document> listByProject(Long projectId, String docType) {
        LambdaQueryWrapper<Document> qw = new LambdaQueryWrapper<Document>()
                .eq(Document::getProjectId, projectId)
                .eq(StrUtil.isNotBlank(docType), Document::getDocType, docType)
                .orderByDesc(Document::getUpdateTime);
        return list(qw);
    }

    @Override
    public IPage<Document> page(Long current, Long size, Long projectId, String docType, String stage) {
        LambdaQueryWrapper<Document> qw = new LambdaQueryWrapper<>();
        if (projectId != null) qw.eq(Document::getProjectId, projectId);
        if (StrUtil.isNotBlank(docType)) qw.eq(Document::getDocType, docType);
        if (StrUtil.isNotBlank(stage)) qw.eq(Document::getStage, stage);
        qw.orderByDesc(Document::getUpdateTime);
        return page(new Page<>(current, size), qw);
    }

    @Override
    public Document detail(Long id) {
        Document d = getById(id);
        if (d == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "文档不存在: " + id);
        }
        return d;
    }

    @Override
    public List<Map<String, Object>> listVersions(Long id) {
        LambdaQueryWrapper<DocumentVersion> qw = new LambdaQueryWrapper<DocumentVersion>()
                .eq(DocumentVersion::getDocumentId, id)
                .orderByDesc(DocumentVersion::getVersion);
        List<DocumentVersion> list = documentVersionMapper.selectList(qw);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        list.forEach(v -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", v.getId());
            m.put("documentId", v.getDocumentId());
            m.put("version", v.getVersion());
            m.put("creatorId", v.getCreatorId());
            m.put("creatorName", v.getCreatorName());
            m.put("createTime", v.getCreateTime());
            m.put("diffSummary", v.getDiffSummary());
            result.add(m);
        });
        return result;
    }

    @Override
    public Map<String, Object> statistics(Long projectId) {
        LambdaQueryWrapper<Document> qw = new LambdaQueryWrapper<Document>()
                .eq(Document::getProjectId, projectId);
        List<Document> all = list(qw);
        Map<String, Integer> byType = new HashMap<>();
        for (Document d : all) {
            byType.merge(d.getDocType(), 1, Integer::sum);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", all.size());
        result.put("byType", byType);
        return result;
    }
}
