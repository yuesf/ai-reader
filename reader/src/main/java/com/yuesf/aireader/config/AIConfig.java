package com.yuesf.aireader.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云AI服务配置
 * 包括文档智能服务和百炼平台配置
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AIConfig {
    
    /**
     * 阿里云百炼平台API密钥
     */
    private String apiKey = "";
    
    /**
     * 摘要生成配置
     */
    private SummarizeConfig summarize = new SummarizeConfig();
    
    /**
     * 服务端点（兼容模式）
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
         * 推荐使用 qwen-long 处理长文档
         */
        private String model = "qwen-long";
        
        /**
         * 最大输出token数
         */
        private Integer maxTokens;
        
        /**
         * 温度参数，控制生成的随机性
         */
        private Float temperature = 0.7f;
        
        /**
         * Top-p参数，控制生成的多样性
         */
        private Double topP = 0.8;
    }
}
