package com.rk.agent.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rk.agent.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project")
@Schema(description = "项目")
public class Project extends BaseEntity {

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "项目编码（唯一）")
    private String code;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "项目类型：WEB/APP/API/SERVICE")
    private String type;

    @Schema(description = "技术栈，逗号分隔")
    private String techStack;

    @Schema(description = "项目仓库地址")
    @TableField("repo_url")
    private String repoUrl;

    @Schema(description = "项目状态：DRAFT/ARCHIVED/ACTIVE")
    private String status;

    @Schema(description = "项目负责人 ID")
    @TableField("owner_id")
    private Long ownerId;

    @Schema(description = "项目负责人姓名")
    @TableField("owner_name")
    private String ownerName;

    @Schema(description = "项目封面图")
    private String cover;
}
