package com.rk.agent.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rk.agent.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目成员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project_member")
@Schema(description = "项目成员")
public class ProjectMember extends BaseEntity {

    @Schema(description = "项目 ID")
    @TableField("project_id")
    private Long projectId;

    @Schema(description = "用户 ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "项目角色：OWNER/MAIN/DEV/REVIEWER/OBSERVER")
    private String role;
}
