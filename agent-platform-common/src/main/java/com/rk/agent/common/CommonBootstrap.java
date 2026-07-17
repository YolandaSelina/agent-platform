package com.rk.agent.common;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Common 模块不直接启动，作为依赖被引入。
 * 这里放一个空主类仅供单元测试时启动用。
 */
@SpringBootApplication(scanBasePackages = {"com.rk.agent"})
@ConfigurationPropertiesScan("com.rk.agent.common.config")
@MapperScan("com.rk.agent.**.mapper")
@EnableAsync
@EnableScheduling
public class CommonBootstrap {
    // no-op
}
