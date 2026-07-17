package com.rk.agent.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rk.agent.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档版本历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_document_version")
@Schema(description = "文档版本历史")
public class DocumentVersion extends BaseEntity {

    @Schema(description = "文档 ID")
    @TableField("document_id")
    private Long documentId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "变更摘要")
    @TableField("diff_summary")
    private String diffSummary;

    @Schema(description = "创建人 ID")
    @TableField("creator_id")
    private Long creatorId;

    @Schema(description = "创建人姓名")
    @TableField("creator_name")
    private String creatorName;
}
