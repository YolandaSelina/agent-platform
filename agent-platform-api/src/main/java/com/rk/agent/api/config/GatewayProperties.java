package com.rk.agent.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务路由配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.gateway")
public class GatewayProperties {
    private Map<String, String> routes = new HashMap<>();
}
