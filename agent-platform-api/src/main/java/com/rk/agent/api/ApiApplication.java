package com.rk.agent.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"com.rk.agent.api", "com.rk.agent.common", "com.rk.agent.auth", "com.rk.agent.project", "com.rk.agent.pipeline", "com.rk.agent.llm"},
                       exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ConfigurationPropertiesScan("com.rk.agent.common.config")
@EnableAsync
public class ApiApplication {

    public static void main(String[] args) {
        System.setProperty("spring.application.name", "agent-platform-api");
        SpringApplication.run(ApiApplication.class, args);
    }
}
