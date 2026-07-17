package com.rk.agent.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "项目创建/更新请求")
public class ProjectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "项目 ID（更新时必填）")
    private Long id;

    @NotBlank(message = "项目名称不能为空")
    private String name;

    @NotBlank(message = "项目编码不能为空")
    private String code;

    private String description;
    private String type;
    private String techStack;
    private String repoUrl;
    private String cover;
    private String status;
    private Long ownerId;
    private String ownerName;
}
