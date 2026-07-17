package com.rk.agent.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "任务创建/更新请求")
public class TaskDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;
    private String type;
    private String priority;
    private String status;
    private Long assigneeId;
    private String assigneeName;
    private Long pipelineId;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
}
