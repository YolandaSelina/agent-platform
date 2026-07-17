package com.rk.agent.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 认证服务启动类 :9081
 */
@SpringBootApplication(scanBasePackages = {"com.rk.agent.auth", "com.rk.agent.common"})
@ConfigurationPropertiesScan("com.rk.agent.common.config")
@MapperScan("com.rk.agent.auth.mapper")
@EnableAsync
@EnableScheduling
public class AuthApplication {

    public static void main(String[] args) {
        System.setProperty("spring.application.name", "agent-platform-auth");
        SpringApplication.run(AuthApplication.class, args);
    }
}
