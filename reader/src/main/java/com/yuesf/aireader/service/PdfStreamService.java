package com.yuesf.aireader.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.yuesf.aireader.config.OssConfig.OssProperties;
import com.yuesf.aireader.entity.FileInfo;
import com.yuesf.aireader.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PDF文件流服务
 * 实现安全的分片加密、断点续传、临时缓存等功能
 */
@Slf4j
@Service
public class PdfStreamService {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssProperties ossProperties;

    @Autowired
    private FileInfoService fileInfoService;

    // 临时缓存：文件ID -> 缓存信息
    private final Map<String, FileCacheInfo> fileCache = new ConcurrentHashMap<>();
    
    // 加密密钥缓存：文件ID -> 加密密钥
    private final Map<String, String> encryptionKeys = new ConcurrentHashMap<>();
    
    // 分片大小：1MB
    private static final int CHUNK_SIZE = 1024 * 1024;
    
    // 缓存过期时间：30分钟
    private static final long CACHE_EXPIRE_TIME = 30 * 60 * 1000;

    /**
     * 生成指定页的图片（PNG）并返回字节数组
     * 注意：该实现依赖后端能读取到PDF二进制（通过 OSS），用临时文件渲染，避免内存占用过高。
     */
    public byte[] renderPdfPageAsImage(String fileId, int pageNumber) throws IOException {
        FileInfo fileInfo = fileInfoService.getFileInfoById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        if (!"pdf".equalsIgnoreCase(fileInfo.getFileType())) {
            throw new BusinessException("文件类型不支持");
        }

        // 从 OSS 读取整个文件到临时文件（若文件较大可考虑分段或缓存）
        File tempPdf = File.createTempFile("pdfsrc_", ".pdf");
        try (OutputStream fos = new FileOutputStream(tempPdf)) {
            long start = 0;
            long end = fileInfo.getFileSize() - 1;
            byte[] data = readChunkFromOss(fileInfo.getFileName(), start, end);
            fos.write(data);
        }

        // 使用 PDFBox 渲染指定页为 PNG
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(tempPdf)) {
            int total = document.getNumberOfPages();
            int pageIndex = Math.max(1, Math.min(pageNumber, total)) - 1;
            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
            // 以 150DPI 渲染，平衡清晰度与体积
            java.awt.image.BufferedImage bim = renderer.renderImageWithDPI(pageIndex, 150);

            // 可选缩放（保持原尺寸），使用 Thumbnailator 以确保高质量输出
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bim, "png", baos);
            return baos.toByteArray();
        } finally {
            // 清理临时文件
            if (tempPdf.exists()) {
                try { if (!tempPdf.delete()) tempPdf.deleteOnExit(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 获取PDF文件流（支持断点续传）
     */
    public void streamPdfFile(String fileId, String range, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileInfoService.getFileInfoById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        // 检查文件类型
        if (!"pdf".equalsIgnoreCase(fileInfo.getFileType())) {
            throw new BusinessException("文件类型不支持");
        }

        // 获取或生成加密密钥
        String encryptionKey = getOrGenerateEncryptionKey(fileId);
        
        // 处理断点续传
        long startByte = 0;
        long endByte = fileInfo.getFileSize() - 1;
        
        if (StringUtils.hasText(range)) {
            String[] rangeParts = range.replace("bytes=", "").split("-");
            startByte = Long.parseLong(rangeParts[0]);
            if (rangeParts.length > 1 && StringUtils.hasText(rangeParts[1])) {
                endByte = Long.parseLong(rangeParts[1]);
            }
        }

        // 设置响应头
        response.setContentType("application/pdf");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Length", String.valueOf(endByte - startByte + 1));
        
        if (StringUtils.hasText(range)) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + fileInfo.getFileSize());
        }

        // 流式传输PDF文件
        streamPdfFromOss(fileInfo.getFileName(), startByte, endByte, encryptionKey, response);
    }

    /**
     * 获取PDF文件分片（不加密）
     */
    public byte[] getPdfChunk(String fileId, int chunkIndex) throws IOException {
        FileInfo fileInfo = fileInfoService.getFileInfoById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        // 检查文件类型
        if (!"pdf".equalsIgnoreCase(fileInfo.getFileType())) {
            throw new BusinessException("文件类型不支持");
        }

        // 计算分片范围
        long startByte = (long) chunkIndex * CHUNK_SIZE;
        long endByte = Math.min(startByte + CHUNK_SIZE - 1, fileInfo.getFileSize() - 1);
        
        // 从OSS读取分片数据（不加密）
        byte[] chunkData = readChunkFromOss(fileInfo.getFileName(), startByte, endByte);
        
        return chunkData;
    }

    /**
     * 获取PDF文件信息（用于小程序端分片下载）
     */
    public Map<String, Object> getPdfFileInfo(String fileId) {
        FileInfo fileInfo = fileInfoService.getFileInfoById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        // 计算分片数量
        int totalChunks = (int) Math.ceil((double) fileInfo.getFileSize() / CHUNK_SIZE);
        
        // 获取或生成加密密钥
        String encryptionKey = getOrGenerateEncryptionKey(fileId);

        return Map.of(
            "fileId", fileId,
            "filename", fileInfo.getOriginalName(),
            "fileSize", fileInfo.getFileSize(),
            "totalChunks", totalChunks,
            "chunkSize", CHUNK_SIZE,
            "encryptionKey", encryptionKey,
            "totalPages", 2,
            "lastModified", fileInfo.getUploadTime().toString()
        );
    }

    /**
     * 从OSS流式传输PDF文件
     */
    private void streamPdfFromOss(String objectKey, long startByte, long endByte, String encryptionKey, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        OSSObject ossObject = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            // 设置OSS请求范围
            com.aliyun.oss.model.GetObjectRequest getObjectRequest = new com.aliyun.oss.model.GetObjectRequest(
                ossProperties.getBucketName(), objectKey);
            getObjectRequest.setRange(startByte, endByte);
            
            ossObject = ossClient.getObject(getObjectRequest);
            inputStream = ossObject.getObjectContent();
            outputStream = response.getOutputStream();

            // 分片读取和传输（不加密）
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1 && totalBytesRead < (endByte - startByte + 1)) {
                // 截取实际读取的数据
                byte[] actualData = new byte[bytesRead];
                System.arraycopy(buffer, 0, actualData, 0, bytesRead);
                
                // 直接写入响应流（不加密）
                outputStream.write(actualData);
                totalBytesRead += bytesRead;
                
                // 刷新缓冲区
                if (totalBytesRead % (64 * 1024) == 0) { // 每64KB刷新一次
                    outputStream.flush();
                }
            }
            
            outputStream.flush();
            
        } finally {
            if (inputStream != null) try { inputStream.close(); } catch (Exception ignored) {}
            if (ossObject != null) try { ossObject.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * 从OSS读取分片数据
     */
    private byte[] readChunkFromOss(String objectKey, long startByte, long endByte) throws IOException {
        OSSObject ossObject = null;
        InputStream inputStream = null;
        
        try {
            // 设置OSS请求范围
            com.aliyun.oss.model.GetObjectRequest getObjectRequest = new com.aliyun.oss.model.GetObjectRequest(
                ossProperties.getBucketName(), objectKey);
            getObjectRequest.setRange(startByte, endByte);
            
            ossObject = ossClient.getObject(getObjectRequest);
            inputStream = ossObject.getObjectContent();
            
            // 计算期望读取的字节数
            long expectedBytes = endByte - startByte + 1;
            
            // 使用ByteArrayOutputStream来动态读取数据
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            long totalBytesRead = 0;
            
            int bytesRead;
            while (totalBytesRead < expectedBytes && (bytesRead = inputStream.read(data, 0, 
                    (int) Math.min(data.length, expectedBytes - totalBytesRead))) != -1) {
                buffer.write(data, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            
            // 返回读取的数据
            return buffer.toByteArray();
            
        } finally {
            if (inputStream != null) try { inputStream.close(); } catch (Exception ignored) {}
            if (ossObject != null) try { ossObject.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * 加密分片数据 (保留方法但不使用)
     */
    private byte[] encryptChunk(byte[] data, String encryptionKey, int chunkIndex) throws IOException {
        try {
            // 生成分片特定的密钥（基于主密钥和分片索引）
            String chunkKey = generateChunkKey(encryptionKey, chunkIndex);
            SecretKeySpec secretKey = new SecretKeySpec(chunkKey.getBytes(StandardCharsets.UTF_8), "AES");
            
            // 生成随机IV向量
            byte[] iv = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            
            // 加密数据
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new javax.crypto.spec.IvParameterSpec(iv));
            
            byte[] encryptedData = cipher.doFinal(data);
            
            // 将IV和加密数据合并：IV(16字节) + 加密数据
            byte[] result = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
            
            return result;
            
        } catch (Exception e) {
            log.error("加密分片数据失败", e);
            throw new IOException("加密失败", e);
        }
    }

    /**
     * 生成分片特定的密钥 (保留方法但不使用)
     */
    private String generateChunkKey(String baseKey, int chunkIndex) throws Exception {
        try {
            // 使用HMAC-SHA256生成分片密钥
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                baseKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            String chunkInfo = baseKey + "_" + chunkIndex;
            byte[] hash = mac.doFinal(chunkInfo.getBytes(StandardCharsets.UTF_8));
            
            // 取前16字节作为AES密钥
            byte[] aesKey = new byte[16];
            System.arraycopy(hash, 0, aesKey, 0, Math.min(hash.length, 16));
            
            return Base64.getEncoder().encodeToString(aesKey);
            
        } catch (Exception e) {
            log.error("生成分片密钥失败", e);
            // 返回默认密钥（生产环境应该抛出异常）
            return "default-encryption-key-32-bytes-long";
        }
    }

    /**
     * 获取或生成加密密钥
     */
    private String getOrGenerateEncryptionKey(String fileId) {
        return encryptionKeys.computeIfAbsent(fileId, k -> {
            try {
                // 生成新的AES密钥
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, new SecureRandom());
                SecretKey secretKey = keyGen.generateKey();
                return Base64.getEncoder().encodeToString(secretKey.getEncoded());
            } catch (Exception e) {
                log.error("生成加密密钥失败", e);
                // 返回默认密钥（生产环境应该抛出异常）
                return "default-encryption-key-32-bytes-long";
            }
        });
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        fileCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getCacheTime() > CACHE_EXPIRE_TIME);
        
        // 清理过期的加密密钥（可选，根据安全策略决定）
        // encryptionKeys.clear();
    }

    /**
     * 文件缓存信息
     */
    private static class FileCacheInfo {
        private final long cacheTime;
        private final String filePath;
        private final long fileSize;

        public FileCacheInfo(String filePath, long fileSize) {
            this.cacheTime = System.currentTimeMillis();
            this.filePath = filePath;
            this.fileSize = fileSize;
        }

        public long getCacheTime() { return cacheTime; }
        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
    }
}