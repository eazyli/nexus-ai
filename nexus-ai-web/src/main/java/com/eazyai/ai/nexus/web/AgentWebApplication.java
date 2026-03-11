package com.eazyai.ai.nexus.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI Agent Web应用启动类
 * 
 * <p>Web层只负责启动应用，基础设施配置由infra层提供</p>
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {
        "com.eazyai.ai.nexus.core",
        "com.eazyai.ai.nexus.tools",
        "com.eazyai.ai.nexus.web",
        "com.eazyai.ai.nexus.infra",
        "com.eazyai.ai.nexus.application"
})
public class AgentWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentWebApplication.class, args);
    }
}
