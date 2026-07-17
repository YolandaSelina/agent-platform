package com.rk.agent.project.dto;

import com.rk.agent.project.entity.ProjectPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 项目页面保存 DTO
 */
@Data
@Schema(description = "项目页面保存 DTO")
public class ProjectPageSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（编辑时必传，新建时为空）")
    private Long id;

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    private Long parentId;

    @NotBlank(message = "页面类型不能为空")
    private String type; // PAGE/DOC/TASK/ARTIFACT/PIPELINE/REVIEW

    @NotBlank(message = "页面标题不能为空")
    private String title;

    private String content;
    private Long taskId;
    private Long pipelineId;
    private Integer sortOrder;
    private String icon;

    public ProjectPage toEntity() {
        ProjectPage e = new ProjectPage();
        e.setProjectId(this.projectId);
        e.setParentId(this.parentId);
        e.setType(this.type);
        e.setTitle(this.title);
        e.setContent(this.content);
        e.setTaskId(this.taskId);
        e.setPipelineId(this.pipelineId);
        e.setSortOrder(this.sortOrder);
        e.setIcon(this.icon);
        return e;
    }
}
