package com.eazyai.ai.nexus.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI Agent Web应用启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.eazyai.ai.nexus.core",
        "com.eazyai.ai.nexus.tools",
        "com.eazyai.ai.nexus.web",
        "com.eazyai.ai.nexus.infra",
        "com.eazyai.ai.nexus.application"
})
@MapperScan("com.eazyai.ai.nexus.infra.dal.mapper")
public class AgentWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentWebApplication.class, args);
    }
}
