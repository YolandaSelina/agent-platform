package com.rk.agent.common.security;

import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具
 */
@Slf4j
@Data
@Component
public class JwtUtil {

    @Value("${agent.jwt.secret:agent-platform-default-secret-please-change-in-production-32bytes}")
    private String secret;

    @Value("${agent.jwt.expire:86400}")
    private Long expireSeconds;

    @Value("${agent.jwt.header:Authorization}")
    private String header;

    @Value("${agent.jwt.prefix:Bearer }")
    private String prefix;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret 长度必须 >= 32 字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, java.util.Collections.emptySet(), java.util.Collections.emptySet());
    }

    public String generateToken(Long userId, String username, java.util.Set<String> roles, java.util.Set<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        if (roles != null && !roles.isEmpty()) {
            claims.put("roles", new java.util.ArrayList<>(roles));
        }
        if (permissions != null && !permissions.isEmpty()) {
            claims.put("perms", new java.util.ArrayList<>(permissions));
        }
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            throw new BizException(ResultCode.INVALID_TOKEN, e.getMessage());
        }
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId == null) {
            throw new BizException(ResultCode.INVALID_TOKEN, "token 中缺少 userId");
        }
        return Long.valueOf(userId.toString());
    }

    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    public String resolveToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith(prefix)) {
            return authorization.substring(prefix.length());
        }
        return authorization;
    }
}
