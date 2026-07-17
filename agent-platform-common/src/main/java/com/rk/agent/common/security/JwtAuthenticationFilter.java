package com.rk.agent.common.security;

import com.rk.agent.common.exception.BizException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT 认证过滤器
 * <p>
 * 本类中用到的两个 SecurityContextHolder：
 * <ul>
 *   <li>Spring Security 的 {@link SecurityContextHolder}：存放 Authentication</li>
 *   <li>项目内的 {@link com.rk.agent.common.security.SecurityContextHolder}：存放 LoginUser（业务取用）</li>
 * </ul>
 * 为避免同名冲突，业务侧的 ThreadLocal 全部用全限定名 com.rk.agent.common.security.SecurityContextHolder 访问。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = jwtUtil.resolveToken(request.getHeader(jwtUtil.getHeader()));
        SecurityContext springContext = null;
        if (token != null) {
            try {
                Claims claims = jwtUtil.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                String username = claims.get("username", String.class);

                // 从 JWT claims 读取 roles/permissions（登录时已写入）
                Set<String> roles = new HashSet<>();
                Object rolesClaim = claims.get("roles");
                if (rolesClaim instanceof java.util.List<?> list) {
                    for (Object o : list) {
                        if (o != null) roles.add(o.toString());
                    }
                }
                Set<String> perms = new HashSet<>();
                Object permsClaim = claims.get("perms");
                if (permsClaim instanceof java.util.List<?> list) {
                    for (Object o : list) {
                        if (o != null) perms.add(o.toString());
                    }
                }

                LoginUser loginUser = LoginUser.builder()
                        .userId(userId)
                        .username(username)
                        .roles(roles)
                        .permissions(perms)
                        .build();

                Set<SimpleGrantedAuthority> authorities = new HashSet<>();
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                perms.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        loginUser, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Spring Security 上下文
                springContext = SecurityContextHolder.createEmptyContext();
                springContext.setAuthentication(auth);
                SecurityContextHolder.setContext(springContext);

                // 项目内 ThreadLocal（业务代码从这里取 LoginUser）
                com.rk.agent.common.security.SecurityContextHolder.set(loginUser);
            } catch (BizException e) {
                log.debug("JWT 认证失败: {}", e.getMessage());
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // 清理两个上下文
            com.rk.agent.common.security.SecurityContextHolder.clear();
            if (springContext != null) {
                springContext.setAuthentication(null);
            }
            SecurityContextHolder.clearContext();
        }
    }
}
