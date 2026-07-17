package com.rk.agent.common.util;

import com.rk.agent.common.security.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Web 工具
 */
public final class WebUtils {

    private WebUtils() {
    }

    public static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    public static Optional<HttpServletRequest> currentRequestOptional() {
        return Optional.ofNullable(currentRequest());
    }

    public static String clientIp() {
        return currentRequestOptional()
                .map(r -> {
                    String ip = r.getHeader("X-Forwarded-For");
                    if (isUnknown(ip)) {
                        ip = r.getHeader("X-Real-IP");
                    }
                    if (isUnknown(ip)) {
                        ip = r.getHeader("Proxy-Client-IP");
                    }
                    if (isUnknown(ip)) {
                        ip = r.getRemoteAddr();
                    }
                    return ip;
                })
                .orElse("unknown");
    }

    public static Long currentUserIdOrNull() {
        try {
            return SecurityContextHolder.requireUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isUnknown(String s) {
        return s == null || s.isBlank() || "unknown".equalsIgnoreCase(s);
    }
}
