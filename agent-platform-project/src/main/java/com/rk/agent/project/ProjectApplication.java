package com.rk.agent.project;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 项目服务启动类 :9082
 */
@SpringBootApplication(scanBasePackages = {"com.rk.agent.project", "com.rk.agent.common"})
@ConfigurationPropertiesScan("com.rk.agent.common.config")
@MapperScan("com.rk.agent.project.mapper")
@EnableAsync
@EnableScheduling
public class ProjectApplication {

    public static void main(String[] args) {
        System.setProperty("spring.application.name", "agent-platform-project");
        SpringApplication.run(ProjectApplication.class, args);
    }
}
