package com.yuesf.aireader.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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

    @Component
    @ConfigurationProperties(prefix = "app.oss")
    public static class OssProperties {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private UploadConfig upload;

        public static class UploadConfig {
            private String maxFileSize;
            private List<String> allowedExtensions;
            private String baseUrl;

            public String getMaxFileSize() {
                return maxFileSize;
            }

            public void setMaxFileSize(String maxFileSize) {
                this.maxFileSize = maxFileSize;
            }

            public List<String> getAllowedExtensions() {
                return allowedExtensions;
            }

            public void setAllowedExtensions(List<String> allowedExtensions) {
                this.allowedExtensions = allowedExtensions;
            }

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public UploadConfig getUpload() {
            return upload;
        }

        public void setUpload(UploadConfig upload) {
            this.upload = upload;
        }
    }
}
