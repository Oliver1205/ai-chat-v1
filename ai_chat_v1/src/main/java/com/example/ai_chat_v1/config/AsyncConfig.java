package com.example.ai_chat_v1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步线程池配置：我们专属的后台车间
 */
@Configuration
@EnableAsync // 👈 极其重要：打开 Spring 的异步开关！
public class AsyncConfig {

    // 给我们的车间起个名字，以后调用它就用这个名字
    @Bean(name = "knowledgeBaseExecutor")
    public Executor knowledgeBaseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心工人数：平时保持 2 个工人在岗
        executor.setCorePoolSize(2);
        // 最大工人数：如果任务太多，最多可以临时扩招到 5 个人
        executor.setMaxPoolSize(5);
        // 排队区大小：最多允许 50 个文件在这里排队等候处理
        executor.setQueueCapacity(50);
        // 给工人胸前贴上名字标签，以后看报错日志一眼就知道是谁干的
        executor.setThreadNamePrefix("KB-Worker-");

        executor.initialize();
        return executor;
    }
}