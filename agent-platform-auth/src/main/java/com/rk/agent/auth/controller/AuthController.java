package com.rk.agent.auth.controller;

import com.rk.agent.auth.dto.LoginDTO;
import com.rk.agent.auth.dto.RegisterDTO;
import com.rk.agent.auth.entity.SysUser;
import com.rk.agent.auth.service.SysUserService;
import com.rk.agent.common.result.Result;
import com.rk.agent.common.security.JwtUtil;
import com.rk.agent.common.security.LoginUser;
import com.rk.agent.common.security.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        String token = sysUserService.login(dto.getUsername(), dto.getPassword());
        SysUser user = sysUserService.findByUsername(dto.getUsername());
        Set<String> roles = sysUserService.getUserRoles(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("roles", roles);
        return Result.success(data);
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO dto) {
        Long id = sysUserService.register(dto.getUsername(), dto.getPassword(), dto.getNickname(), dto.getEmail());
        return Result.success(id);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        LoginUser lu = SecurityContextHolder.required();
        SysUser user = sysUserService.getById(lu.getUserId());
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        Set<String> roles = sysUserService.getUserRoles(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("email", user.getEmail());
        data.put("roles", roles);
        return Result.success(data);
    }

    @Operation(summary = "登出（前端清空 token 即可）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
