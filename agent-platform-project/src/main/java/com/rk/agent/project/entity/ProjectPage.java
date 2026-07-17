package com.rk.agent.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rk.agent.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目页面：用于项目内的页面化管理（页面、文档、产物）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project_page")
@Schema(description = "项目页面/标签页")
public class ProjectPage extends BaseEntity {

    @Schema(description = "项目 ID")
    @TableField("project_id")
    private Long projectId;

    @Schema(description = "父页面 ID（用于树形）")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "页面类型：PAGE/DOC/TASK/ARTIFACT/PIPELINE/REVIEW")
    private String type;

    @Schema(description = "页面标题")
    private String title;

    @Schema(description = "页面内容（Markdown/JSON）")
    private String content;

    @Schema(description = "关联任务 ID")
    @TableField("task_id")
    private Long taskId;

    @Schema(description = "关联 Pipeline ID")
    @TableField("pipeline_id")
    private Long pipelineId;

    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;

    @Schema(description = "图标")
    private String icon;
}
