package com.yuesf.aireader.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 缓存清理定时任务服务
 * 定期清理过期的PDF缓存和加密密钥
 */
@Slf4j
@Service
public class CacheCleanupService {

    @Autowired
    private PdfStreamService pdfStreamService;

    /**
     * 每小时清理一次过期缓存
     * 固定时间执行，避免频繁清理
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1小时
    public void cleanupExpiredCache() {
        try {
            log.info("开始执行缓存清理任务...");
            
            // 清理PDF流服务的过期缓存
            pdfStreamService.cleanupExpiredCache();
            
            log.info("缓存清理任务执行完成");
            
        } catch (Exception e) {
            log.error("缓存清理任务执行失败", e);
        }
    }

    /**
     * 每天凌晨2点执行深度清理
     * 清理所有过期的临时文件和缓存
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void deepCleanup() {
        try {
            log.info("开始执行深度清理任务...");
            
            // 这里可以添加更多的清理逻辑
            // 比如清理临时文件、清理数据库中的过期记录等
            
            log.info("深度清理任务执行完成");
            
        } catch (Exception e) {
            log.error("深度清理任务执行失败", e);
        }
    }
}
