package com.rk.agent.common.config;

import com.rk.agent.common.security.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Web 配置：ThreadLocal 工具
 */
@Configuration
public class WebConfig {

    @Bean
    public RequestContextProvider requestContextProvider() {
        return new RequestContextProvider();
    }

    public static class RequestContextProvider {

        public Optional<HttpServletRequest> currentRequest() {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return Optional.ofNullable(attrs).map(ServletRequestAttributes::getRequest);
        }

        public Optional<String> currentTraceId() {
            return currentRequest().map(r -> r.getHeader("X-Trace-Id"));
        }

        public Long currentUserId() {
            try {
                return SecurityContextHolder.requireUserId();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
