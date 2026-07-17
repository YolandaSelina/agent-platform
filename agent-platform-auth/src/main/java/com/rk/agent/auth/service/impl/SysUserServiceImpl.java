package com.rk.agent.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rk.agent.auth.entity.SysRole;
import com.rk.agent.auth.entity.SysUser;
import com.rk.agent.auth.entity.SysUserRole;
import com.rk.agent.auth.mapper.SysRoleMapper;
import com.rk.agent.auth.mapper.SysUserMapper;
import com.rk.agent.auth.mapper.SysUserRoleMapper;
import com.rk.agent.auth.service.SysUserService;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.common.security.JwtUtil;
import com.rk.agent.common.util.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public SysUser findByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public String login(String username, String password) {
        SysUser user = findByUsername(username);
        if (user == null) {
            throw new BizException(ResultCode.LOGIN_FAILED, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        // 更新最后登录信息
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginIp(WebUtils.clientIp());
        update.setLastLoginTime(java.time.LocalDateTime.now());
        updateById(update);

        // 角色 + 权限写入 token（供下游服务鉴权用）
        Set<String> roles = getUserRoles(user.getId());
        Set<String> perms = new HashSet<>();
        for (String role : roles) {
            perms.add("ROLE_" + role);
        }
        return jwtUtil.generateToken(user.getId(), user.getUsername(), roles, perms);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(String username, String password, String nickname, String email) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名或密码不能为空");
        }
        if (findByUsername(username) != null) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(StrUtil.blankToDefault(nickname, username));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(1);
        save(user);
        // 默认绑定 USER 角色
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "USER"));
        if (role != null) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(role.getId());
            sysUserRoleMapper.insert(ur);
        }
        return user.getId();
    }

    @Override
    public Set<String> getUserRoles(Long userId) {
        List<SysUserRole> list = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (list.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = list.stream().map(SysUserRole::getRoleId).toList();
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        Set<String> codes = new HashSet<>();
        roles.forEach(r -> codes.add(r.getCode()));
        return codes;
    }
}
