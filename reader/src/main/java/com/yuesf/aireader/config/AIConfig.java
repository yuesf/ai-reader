package com.yuesf.aireader.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云文档智能服务配置
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AIConfig {
    
    /**
     * API密钥
     */
    private String apiKey = "";
    
    /**
     * 摘要生成配置
     */
    private SummarizeConfig summarize = new SummarizeConfig();
    
    /**
     * 服务端点
     */
    private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
    /**
     * 摘要生成配置类
     */
    @Getter
    @Setter
    public static class SummarizeConfig {
        /**
         * 使用的模型名称
         */
        private String model = "qwen-long";
    }
}
