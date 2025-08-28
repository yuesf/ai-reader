package com.yuesf.aireader.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

//import javax.servlet.MultipartConfigElement;

/**
 * 文件上传配置
 * 解决大文件上传问题
 */
@Configuration
public class MultipartConfig {

    /**
     * 配置文件上传解析器
     */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    /**
     * 配置文件上传参数
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // 设置单个文件最大大小为500MB
        factory.setMaxFileSize(DataSize.ofMegabytes(500));
        
        // 设置总上传数据最大大小为500MB
        factory.setMaxRequestSize(DataSize.ofMegabytes(500));
        
        // 设置内存临界值，超过此值将写入磁盘
        factory.setFileSizeThreshold(DataSize.ofKilobytes(2));
        
        // 设置临时文件存储位置
        factory.setLocation(System.getProperty("java.io.tmpdir"));
        
        return factory.createMultipartConfig();
    }
}