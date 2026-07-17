package com.rk.agent.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "注册请求")
public class RegisterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64)
    private String password;

    private String nickname;

    @Email(message = "邮箱格式错误")
    private String email;
}
