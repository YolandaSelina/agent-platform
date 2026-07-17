package com.rk.agent.llm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * LLM 适配服务启动类 :9084
 */
@SpringBootApplication(scanBasePackages = {"com.rk.agent.llm", "com.rk.agent.common"})
@ConfigurationPropertiesScan("com.rk.agent.common.config")
@MapperScan("com.rk.agent.llm.mapper")
@EnableAsync
public class LlmApplication {

    public static void main(String[] args) {
        System.setProperty("spring.application.name", "agent-platform-llm");
        SpringApplication.run(LlmApplication.class, args);
    }
}
