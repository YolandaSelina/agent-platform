package com.rk.agent.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rk.agent.auth.entity.SysUser;

import java.util.Set;

public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询
     */
    SysUser findByUsername(String username);

    /**
     * 登录
     */
    String login(String username, String password);

    /**
     * 注册
     */
    Long register(String username, String password, String nickname, String email);

    /**
     * 拿当前用户的角色
     */
    Set<String> getUserRoles(Long userId);
}
