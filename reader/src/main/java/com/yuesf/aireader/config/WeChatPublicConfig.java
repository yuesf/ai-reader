package com.yuesf.aireader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.public")
public class WeChatPublicConfig {
    
    /**
     * 公众号AppId
     */
    private String appid;
    
    /**
     * 公众号AppSecret
     */
    private String secret;
    
    /**
     * 小程序配置
     */
    private MiniProgram miniprogram = new MiniProgram();
    
    @Data
    public static class MiniProgram {
        /**
         * 小程序AppId
         */
        private String appid;
        
        /**
         * 报告详情页路径
         */
        private String reportDetailPath;
    }
}