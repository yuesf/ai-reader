package com.yuesf.aireader.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.yuesf.aireader.config.OssConfig;
import com.yuesf.aireader.entity.FileInfo;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileUploadService {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssConfig.OssProperties ossProperties;

    @Autowired
    private FileInfoService fileInfoService;

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    /**
     * 上传文件到阿里云OSS并保存文件信息
     *
     * @param file         上传的文件
     * @param folder       存储文件夹，如 "reports", "images" 等
     * @param uploadUserId 上传用户ID
     * @return 文件信息对象
     * @throws IOException
     */
    public FileInfo uploadFile(MultipartFile file, String folder, String uploadUserId) throws IOException {
        // 验证文件
        validateFile(file);

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String fileName = generateFileName(folder, originalFilename);

        // 设置对象元数据
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        // 设置ACL为私有
        metadata.setObjectAcl(com.aliyun.oss.model.CannedAccessControlList.Private);

        // 上传到OSS
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                ossProperties.getBucketName(),
                fileName,
                file.getInputStream(),
                metadata
        );

        PutObjectResult result = ossClient.putObject(putObjectRequest);


        if (null != result && StringUtils.isNotBlank(result.getRequestId())) {
            // 创建文件信息对象
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(fileName); // 存储文件路径
            fileInfo.setOriginalName(originalFilename);
            fileInfo.setFileSize(file.getSize());
            fileInfo.setFileType(extension);
            fileInfo.setFolder(folder);
            fileInfo.setUploadUserId(uploadUserId);
            fileInfo.setRequestId(result.getRequestId()); // 存储请求ID
            fileInfo.setPageNums(getPdfPageCount(file));
            // 保存文件信息到数据库
            return fileInfoService.saveFileInfo(fileInfo);
        } else {
            throw new RuntimeException("文件上传失败");
        }
    }

    public int getPdfPageCount(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            return document.getNumberOfPages();
        }
    }

    /**
     * 上传报告相关文件
     *
     * @param file         上传的文件
     * @param uploadUserId 上传用户ID
     * @return 文件信息对象
     */
    public FileInfo uploadReportFile(MultipartFile file, String uploadUserId) throws IOException {
        return uploadFile(file, ossProperties.getFolder().getReports(), uploadUserId);
    }

    /**
     * 上传报告相关文件（兼容旧版本）
     *
     * @param file 上传的文件
     * @return 文件访问URL
     */
    public String uploadReportFile(MultipartFile file) throws IOException {
        FileInfo fileInfo = uploadReportFile(file, null);
        return generatePresignedUrl(fileInfo.getFileName(), 3600); // 返回临时访问链接
    }

    /**
     * 上传图片文件
     *
     * @param file 上传的图片
     * @return 图片访问URL
     */
    public FileInfo uploadImage(MultipartFile file) throws IOException {
        return uploadFile(file, ossProperties.getFolder().getImages(), null);
    }

    /**
     * 删除OSS上的文件
     *
     * @param fileId 文件URL
     */
    public void deleteFile(String fileId) {
        FileInfo fileInfo = fileInfoService.getFileInfoById(fileId);

        if (fileInfo != null && StringUtils.isNotBlank(fileInfo.getFileName())) {
            String objectKey = fileInfo.getFileName();
            ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
            fileInfoService.deleteFileInfo(fileId);
        }
    }

    /**
     * 通过ID获取文件信息
     */
    public FileInfo getFileInfoById(String id) {
        return fileInfoService.getFileInfoById(id);
    }

    /**
     * 从OSS读取对象并写入HTTP响应，隐藏真实带签名的URL
     */
    public void writeObjectToResponse(String objectKey, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        com.aliyun.oss.model.OSSObject ossObject = null;
        java.io.InputStream inputStream = null;
        try {
            ossObject = ossClient.getObject(ossProperties.getBucketName(), objectKey);
            inputStream = ossObject.getObjectContent();

            // 简单根据扩展名设置Content-Type
            String ext = org.apache.commons.io.FilenameUtils.getExtension(objectKey).toLowerCase();
            String contentType = switch (ext) {
                case "png" -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                default -> "application/octet-stream";
            };
            response.setContentType(contentType);
            // 可选缓存控制（根据业务需要设置）
            response.setHeader("Cache-Control", "public, max-age=31536000");

            java.io.OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } finally {
            if (inputStream != null) try {
                inputStream.close();
            } catch (Exception ignored) {
            }
            if (ossObject != null) try {
                ossObject.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 生成私有文件的临时访问URL
     *
     * @param objectKey  OSS对象键
     * @param expiration 过期时间（秒）
     * @return 临时访问URL
     */
    public String generatePresignedUrl(String objectKey, int expiration) {
        java.util.Date expirationDate = new java.util.Date(new java.util.Date().getTime() + expiration * 1000);
        return ossClient.generatePresignedUrl(ossProperties.getBucketName(), objectKey, expirationDate).toString();
    }


    /**
     * 将PDF文件转换为预览图片
     *
     * @param fileUrl pdf 文件的URL
     * @return 预览图片的URL
     */
    public String convertPdfToPreviewImage(String fileUrl) {
        // 这里需要实现PDF fileUrl 转换逻辑
        // 实际开发中应该调用具体的PDF转换服务

        // 这里返回一个示例URL
        return "preview.jpg";
    }

    /**
     * 验证文件
     *
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
     *
     * @param folder 文件夹
     * @return 生成的文件名
     */
    private String generateFileName(String folder, String originalFilename) {
        String extension = FilenameUtils.getExtension(originalFilename);
        String baseName = FilenameUtils.getBaseName(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        return String.format("%s/%s/%s-%s.%s", folder, timestamp, baseName, uuid, extension);
    }

    /**
     * 解析文件大小字符串
     *
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