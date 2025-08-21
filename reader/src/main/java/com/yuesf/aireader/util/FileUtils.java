package com.yuesf.aireader.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件工具类
 */
@Slf4j
public class FileUtils {

    /**
     * 生成安全的文件名
     * @param originalName 原始文件名
     * @return 安全的文件名
     */
    public static String generateSafeFileName(String originalName) {
        if (StringUtils.isBlank(originalName)) {
            return "file_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }

        // 移除不安全的字符
        String safeName = originalName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
        
        // 限制文件名长度
        if (safeName.length() > 100) {
            String extension = StringUtils.substringAfterLast(safeName, ".");
            if (StringUtils.isNotEmpty(extension)) {
                String namePart = StringUtils.substringBeforeLast(safeName, ".");
                if (namePart.length() > (100 - extension.length() - 1)) {
                    namePart = namePart.substring(0, 100 - extension.length() - 1);
                }
                safeName = namePart + "." + extension;
            } else {
                safeName = safeName.substring(0, 100);
            }
        }
        
        log.debug("Generated safe filename: {} from original: {}", safeName, originalName);
        return safeName;
    }

    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    public static String formatFileSize(long size) {
        if (size <= 0) {
            return "0B";
        }
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return StringUtils.stripEnd(String.format("%.1f", size / Math.pow(1024, digitGroups)), "0") 
               + units[digitGroups];
    }
}