package com.rk.agent.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rk.agent.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_task")
@Schema(description = "项目任务")
public class Task extends BaseEntity {

    @Schema(description = "项目 ID")
    @TableField("project_id")
    private Long projectId;

    @Schema(description = "任务标题")
    private String title;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "任务类型：FEATURE/BUG/IMPROVEMENT/REQUIREMENT")
    private String type;

    @Schema(description = "优先级：P0/P1/P2/P3")
    private String priority;

    @Schema(description = "状态：TODO/DOING/REVIEW/DONE/CANCELED")
    private String status;

    @Schema(description = "创建人 ID")
    @TableField("creator_id")
    private Long creatorId;

    @Schema(description = "创建人姓名")
    @TableField("creator_name")
    private String creatorName;

    @Schema(description = "负责人 ID")
    @TableField("assignee_id")
    private Long assigneeId;

    @Schema(description = "负责人姓名")
    @TableField("assignee_name")
    private String assigneeName;

    @Schema(description = "关联的 Pipeline ID")
    @TableField("pipeline_id")
    private Long pipelineId;

    @Schema(description = "开始时间")
    @TableField("start_time")
    private LocalDateTime startTime;

    @Schema(description = "截止时间")
    @TableField("deadline")
    private LocalDateTime deadline;
}
