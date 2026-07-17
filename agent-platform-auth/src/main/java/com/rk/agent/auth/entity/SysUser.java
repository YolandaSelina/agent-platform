package com.rk.agent.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rk.agent.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser extends BaseEntity {

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "密码（BCrypt）")
    private String password;

    @Schema(description = "状态：0=禁用,1=启用")
    private Integer status;

    @Schema(description = "最后登录时间")
    @TableField("last_login_time")
    private java.time.LocalDateTime lastLoginTime;

    @Schema(description = "最后登录 IP")
    @TableField("last_login_ip")
    private String lastLoginIp;
}
