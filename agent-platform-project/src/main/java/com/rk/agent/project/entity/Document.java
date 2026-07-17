package com.rk.agent.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rk.agent.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档（统一管理需求/产品/设计/测试/发布等各阶段文档）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_document")
@Schema(description = "文档")
public class Document extends BaseEntity {

    @Schema(description = "项目 ID")
    @TableField("project_id")
    private Long projectId;

    @Schema(description = "任务 ID")
    @TableField("task_id")
    private Long taskId;

    @Schema(description = "Pipeline Run ID")
    @TableField("pipeline_run_id")
    private Long pipelineRunId;

    @Schema(description = "Node Run ID")
    @TableField("node_run_id")
    private Long nodeRunId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "文档类型：REQUIREMENT/PRODUCT/DESIGN/CODE/TEST/RELEASE/REVIEW/NOTE")
    @TableField("doc_type")
    private String docType;

    @Schema(description = "阶段标识")
    private String stage;

    @Schema(description = "格式：MARKDOWN/HTML/JSON")
    private String format;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "状态：DRAFT/REVIEWING/APPROVED/DEPRECATED")
    private String status;

    @Schema(description = "创建人 ID")
    @TableField("creator_id")
    private Long creatorId;

    @Schema(description = "创建人姓名")
    @TableField("creator_name")
    private String creatorName;

    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "附件路径")
    @TableField("file_path")
    private String filePath;
}
