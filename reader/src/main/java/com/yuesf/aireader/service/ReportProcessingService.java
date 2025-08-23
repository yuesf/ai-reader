package com.yuesf.aireader.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import com.yuesf.aireader.config.OssConfig.OssProperties;
import com.yuesf.aireader.entity.FileInfo;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * 报告处理：从PDF提取首图缩略图并上传到OSS
 */
@Slf4j
@Service
public class ReportProcessingService {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssProperties ossProperties;

    @Autowired
    private FileInfoService fileInfoService;

    // 目前不直接复用上传服务

    /**
     * 生成缩略图并上传，返回缩略图对象在OSS中的路径（objectKey）
     */
    public FileInfo generateAndUploadThumbnailFromPdf(FileInfo pdfFileInfo) throws IOException {
        String objectKey = pdfFileInfo.getFileName();
        // 从OSS读取PDF
        var ossObject = ossClient.getObject(ossProperties.getBucketName(), objectKey);
        try (var input = ossObject.getObjectContent(); var document = PDDocument.load(input)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage pageImage = renderer.renderImageWithDPI(0, 180, ImageType.RGB);
            // 生成缩略图，最大边不超过 600px
            ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();
            Thumbnails.of(pageImage)
                    .size(600, 600)
                    .outputFormat("jpg")
                    .outputQuality(0.85f)
                    .toOutputStream(thumbOut);

            byte[] bytes = thumbOut.toByteArray();
            String folder = ossProperties.getFolder().getImages();
            String imageObjectKey = buildImageKey(folder);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType("image/jpeg");

            PutObjectResult result = ossClient.putObject(
                    ossProperties.getBucketName(),
                    imageObjectKey,
                    new ByteArrayInputStream(bytes),
                    metadata
            );
            if (null != result && StringUtils.isNotBlank(result.getRequestId())) {
                // 创建文件信息对象
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFileName(imageObjectKey); // 存储文件路径
                fileInfo.setOriginalName("image.jpg");
                fileInfo.setFileSize(metadata.getContentLength());
                fileInfo.setFileType("jpg");
                fileInfo.setFolder(folder);
//                fileInfo.setUploadUserId(uploadUserId);
                fileInfo.setRequestId(result.getRequestId()); // 存储请求ID

                // 保存文件信息到数据库
                return fileInfoService.saveFileInfo(fileInfo);
            } else {
                throw new RuntimeException("文件上传失败");
            }
        }
    }

    private String buildImageKey(String folder) {
//        String baseName = originalFilename;
//        int dot = baseName.lastIndexOf('.');
//        if (dot > 0) baseName = baseName.substring(0, dot);
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
//        return String.format("%s/%s/%s-%s.jpg", ossProperties.getFolder().getImages(), timestamp, baseName, uuid);

//        String extension = FilenameUtils.getExtension(originalFilename);
//        String baseName = FilenameUtils.getBaseName(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        return String.format("%s/%s/%s-%s.%s", folder, timestamp, "image", uuid, "jpg");
    }

    /**
     * 删除缩略图
     * @param thumbnailKey OSS中的缩略图对象键
     */
    public void deleteThumbnail(String thumbnailKey) {
        if (thumbnailKey != null && !thumbnailKey.isBlank()) {
            try {
                ossClient.deleteObject(ossProperties.getBucketName(), thumbnailKey);
                log.info("缩略图删除成功: {}", thumbnailKey);
            } catch (Exception e) {
                log.error("删除缩略图失败: {}", thumbnailKey, e);
            }
        }
    }

    /**
     * 重新生成缩略图（删除旧的，生成新的）
     * @param pdfFileInfo PDF文件信息
     * @return 新的缩略图对象键
     */
    public FileInfo regenerateThumbnail(FileInfo pdfFileInfo, String oldThumbnailKey) throws IOException {
        // 删除旧的缩略图
        if (oldThumbnailKey != null && !oldThumbnailKey.isBlank()) {
            deleteThumbnail(oldThumbnailKey);
        }
        // 生成新的缩略图
        return generateAndUploadThumbnailFromPdf(pdfFileInfo);
    }
}


