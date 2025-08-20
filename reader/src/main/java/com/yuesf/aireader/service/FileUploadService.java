package com.yuesf.aireader.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.yuesf.aireader.config.OssConfig.OssProperties;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileUploadService {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssProperties ossProperties;

    /**
     * 上传文件到阿里云OSS
     * @param file 上传的文件
     * @param folder 存储文件夹，如 "reports", "images" 等
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String fileName = generateFileName(folder, extension);
        
        // 上传到OSS
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                ossProperties.getBucketName(),
                fileName,
                file.getInputStream()
        );
        
        PutObjectResult result = ossClient.putObject(putObjectRequest);
        
        if (result.getResponse().isSuccessful()) {
            // 返回文件访问URL
            return ossProperties.getUpload().getBaseUrl() + "/" + fileName;
        } else {
            throw new RuntimeException("文件上传失败");
        }
    }

    /**
     * 上传报告相关文件
     * @param file 上传的文件
     * @return 文件访问URL
     */
    public String uploadReportFile(MultipartFile file) throws IOException {
        return uploadFile(file, "reports");
    }

    /**
     * 上传图片文件
     * @param file 上传的图片
     * @return 图片访问URL
     */
    public String uploadImage(MultipartFile file) throws IOException {
        return uploadFile(file, "images");
    }

    /**
     * 删除OSS上的文件
     * @param fileUrl 文件URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl != null && fileUrl.startsWith(ossProperties.getUpload().getBaseUrl())) {
            String objectKey = fileUrl.substring(ossProperties.getUpload().getBaseUrl().length() + 1);
            ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
        }
    }

    /**
     * 验证文件
     * @param file 上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检查文件大小
        long maxSize = parseFileSize(ossProperties.getUpload().getMaxFileSize());
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小超过限制: " + ossProperties.getUpload().getMaxFileSize());
        }

        // 检查文件扩展名
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || !ossProperties.getUpload().getAllowedExtensions().contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
    }

    /**
     * 生成文件名
     * @param folder 文件夹
     * @param extension 文件扩展名
     * @return 生成的文件名
     */
    private String generateFileName(String folder, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        return String.format("%s/%s/%s.%s", folder, timestamp, uuid, extension);
    }

    /**
     * 解析文件大小字符串
     * @param sizeStr 大小字符串，如 "100MB", "1GB"
     * @return 字节数
     */
    private long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return 100 * 1024 * 1024; // 默认100MB
        }
        
        sizeStr = sizeStr.toUpperCase();
        if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024;
        } else if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024;
        } else if (sizeStr.endsWith("GB")) {
            return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024 * 1024;
        } else {
            return Long.parseLong(sizeStr);
        }
    }
}
