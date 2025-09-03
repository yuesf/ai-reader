package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.entity.tracking.TrackingEvent;
import com.yuesf.aireader.exception.BusinessException;
import com.yuesf.aireader.service.TrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 埋点管理控制器
 * 提供后端管理系统使用的埋点数据查询和统计接口
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin/tracking")
public class TrackingAdminController {

    @Autowired
    private TrackingService trackingService;

    /**
     * 获取实时监控面板数据
     * 
     * 提供埋点数据的实时监控面板，包括今日事件数、活跃用户数、热门页面等统计信息
     * 
     * @return ApiResponse<Map<String, Object>> 监控面板数据，包含各项统计指标
     * @throws BusinessException 当数据获取失败时抛出
     */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        log.info("获取实时监控面板数据");
        
        try {
            Map<String, Object> dashboardData = trackingService.getDashboardData();
            
            log.info("监控面板数据获取成功，数据项数量: {}", dashboardData.size());
            return ApiResponse.success(dashboardData);
        } catch (BusinessException e) {
            log.error("获取监控面板数据业务异常: {}", e.getMessage());
            return ApiResponse.error("获取监控面板数据失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取监控面板数据系统异常: {}", e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取用户行为轨迹
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param limit 限制数量
     * @return 用户行为轨迹
     */
    @GetMapping("/users/{userId}/path")
    public ApiResponse<List<TrackingEvent>> getUserPath(
            @PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "100") Integer limit) {
        
        log.info("获取用户行为轨迹: userId={}, startDate={}, endDate={}, limit={}", 
                userId, startDate, endDate, limit);
        
        try {
            Long startTime = startDate != null ? 
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
            Long endTime = endDate != null ? 
                endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;

            List<TrackingEvent> userPath = trackingService.getUserPath(userId, startTime, endTime, limit);
            
            log.info("用户行为轨迹获取成功: userId={}, eventCount={}", userId, userPath.size());
            return ApiResponse.success(userPath);
        } catch (BusinessException e) {
            log.error("获取用户行为轨迹业务异常: userId={}, error={}", userId, e.getMessage());
            return ApiResponse.error("获取用户行为轨迹失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取用户行为轨迹系统异常: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取页面热力图数据
     *
     * @param pagePath 页面路径
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 热力图数据
     */
    @GetMapping("/heatmap")
    public ApiResponse<List<Map<String, Object>>> getHeatmapData(
            @RequestParam(required = false) String pagePath,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        log.info("获取页面热力图数据: pagePath={}, startDate={}, endDate={}", 
                pagePath, startDate, endDate);
        
        try {
            Long startTime = startDate != null ? 
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
            Long endTime = endDate != null ? 
                endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;

            List<Map<String, Object>> heatmapData = trackingService.getHeatmapData(pagePath, startTime, endTime);
            
            log.info("页面热力图数据获取成功: pagePath={}, dataCount={}", pagePath, heatmapData.size());
            return ApiResponse.success(heatmapData);
        } catch (BusinessException e) {
            log.error("获取热力图数据业务异常: pagePath={}, error={}", pagePath, e.getMessage());
            return ApiResponse.error("获取热力图数据失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取热力图数据系统异常: pagePath={}, error={}", pagePath, e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取埋点统计数据
     *
     * @param groupBy 分组维度 (page_path, event_type, date, hour)
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public ApiResponse<List<Map<String, Object>>> getStatistics(
            @RequestParam(defaultValue = "date") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        log.info("获取埋点统计数据: groupBy={}, startDate={}, endDate={}", 
                groupBy, startDate, endDate);
        
        try {
            Long startTime = startDate != null ? 
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
            Long endTime = endDate != null ? 
                endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;

            List<Map<String, Object>> statistics = trackingService.getStatistics(groupBy, startTime, endTime);
            
            log.info("埋点统计数据获取成功: groupBy={}, dataCount={}", groupBy, statistics.size());
            return ApiResponse.success(statistics);
        } catch (BusinessException e) {
            log.error("获取统计数据业务异常: groupBy={}, error={}", groupBy, e.getMessage());
            return ApiResponse.error("获取统计数据失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取统计数据系统异常: groupBy={}, error={}", groupBy, e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取活跃用户数量
     *
     * @param minutes 时间范围（分钟）
     * @return 活跃用户数量
     */
    @GetMapping("/users/active")
    public ApiResponse<Long> getActiveUserCount(@RequestParam(defaultValue = "30") Integer minutes) {
        log.info("获取活跃用户数量: minutes={}", minutes);
        
        try {
            Long timeThreshold = System.currentTimeMillis() - minutes * 60 * 1000L;
            Long activeUserCount = trackingService.getActiveUserCount(timeThreshold);
            
            log.info("活跃用户数量获取成功: minutes={}, count={}", minutes, activeUserCount);
            return ApiResponse.success(activeUserCount);
        } catch (BusinessException e) {
            log.error("获取活跃用户数量业务异常: minutes={}, error={}", minutes, e.getMessage());
            return ApiResponse.error("获取活跃用户数量失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取活跃用户数量系统异常: minutes={}, error={}", minutes, e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 清理过期数据
     *
     * @param expireDays 过期天数
     * @return 清理结果
     */
    @PostMapping("/cleanup")
    public ApiResponse<Map<String, Integer>> cleanupExpiredData(@RequestParam(defaultValue = "90") Integer expireDays) {
        log.info("清理过期数据: expireDays={}", expireDays);
        
        try {
            Map<String, Integer> cleanupResult = trackingService.cleanupExpiredData(expireDays);
            
            log.info("过期数据清理完成: expireDays={}, result={}", expireDays, cleanupResult);
            return ApiResponse.success(cleanupResult);
        } catch (BusinessException e) {
            log.error("清理过期数据业务异常: expireDays={}, error={}", expireDays, e.getMessage());
            return ApiResponse.error("清理过期数据失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("清理过期数据系统异常: expireDays={}, error={}", expireDays, e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取页面访问统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 页面访问统计
     */
    @GetMapping("/pages/views")
    public ApiResponse<List<Map<String, Object>>> getPageViews(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        log.info("获取页面访问统计: startDate={}, endDate={}", startDate, endDate);
        
        try {
            Long startTime = startDate != null ? 
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
            Long endTime = endDate != null ? 
                endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;

            List<Map<String, Object>> pageViews = trackingService.getStatistics("page_path", startTime, endTime);
            
            log.info("页面访问统计获取成功: dataCount={}", pageViews.size());
            return ApiResponse.success(pageViews);
        } catch (BusinessException e) {
            log.error("获取页面访问统计业务异常: error={}", e.getMessage());
            return ApiResponse.error("获取页面访问统计失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取页面访问统计系统异常: error={}", e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取事件类型统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 事件类型统计
     */
    @GetMapping("/events/types")
    public ApiResponse<List<Map<String, Object>>> getEventTypes(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        log.info("获取事件类型统计: startDate={}, endDate={}", startDate, endDate);
        
        try {
            Long startTime = startDate != null ? 
                startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
            Long endTime = endDate != null ? 
                endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;

            List<Map<String, Object>> eventTypes = trackingService.getStatistics("event_type", startTime, endTime);
            
            log.info("事件类型统计获取成功: dataCount={}", eventTypes.size());
            return ApiResponse.success(eventTypes);
        } catch (BusinessException e) {
            log.error("获取事件类型统计业务异常: error={}", e.getMessage());
            return ApiResponse.error("获取事件类型统计失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取事件类型统计系统异常: error={}", e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }
}