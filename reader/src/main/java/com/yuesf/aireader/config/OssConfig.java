package com.yuesf.aireader.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Configuration
public class OssConfig {

    @Bean
    public OSS ossClient(OssProperties ossProperties) {
        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }

    /**
     * OSS 配置属性类
     */
    @Setter
    @Getter
    @Component
    @ConfigurationProperties(prefix = "app.oss")
    public static class OssProperties {
        // OSS 服务端点
        private String endpoint;
        // 访问密钥 ID
        private String accessKeyId;
        // 访问密钥密钥
        private String accessKeySecret;
        // 存储桶名称
        private String bucketName;
        // 上传配置
        private UploadConfig upload;
        // 文件夹配置
        private FolderConfig folder;

        /**
         * 文件夹配置类
         */
        @Setter
        @Getter
        public static class FolderConfig {
            // 报告文件夹名称
            private String reports = "reports";
            // 图片文件夹名称
            private String images = "images";
        }

        /**
         * 上传配置类
         */
        @Setter
        @Getter
        public static class UploadConfig {
            // 允许上传的最大文件大小
            private String maxFileSize;
            // 允许上传的文件扩展名列表
            private List<String> allowedExtensions;
            // 上传文件的基准URL
            private String baseUrl;
            private String cdnDomain;
        }
    }
}