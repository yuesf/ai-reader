package com.yuesf.aireader.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * AI配置测试类，用于验证配置是否正确加载
 */
@Slf4j
@Component
public class AIConfigTest implements CommandLineRunner {

    private final AIConfig aiConfig;

    public AIConfigTest(AIConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=== AI配置加载验证 ===");
        log.info("API Key: {}", aiConfig.getApiKey().isEmpty() ? "未设置" : "已设置");
        log.info("模型: {}", aiConfig.getSummarize().getModel());
        log.info("端点: {}", aiConfig.getEndpoint());
        log.info("=====================");
    }
}
