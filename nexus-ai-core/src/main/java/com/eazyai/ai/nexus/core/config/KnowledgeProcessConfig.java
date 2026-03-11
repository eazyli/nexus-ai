package com.eazyai.ai.nexus.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 知识库处理线程池配置
 */
@Configuration
@EnableAsync
public class KnowledgeProcessConfig {

    /**
     * 知识库文档处理线程池
     */
    @Bean("knowledgeProcessExecutor")
    public Executor knowledgeProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("kb-process-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // 队列满时，由调用线程执行
            r.run();
        });
        executor.initialize();
        return executor;
    }
}
