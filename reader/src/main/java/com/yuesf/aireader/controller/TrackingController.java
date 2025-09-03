package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.dto.tracking.TrackingBatchReportRequest;
import com.yuesf.aireader.dto.tracking.TrackingReportRequest;
import com.yuesf.aireader.exception.BusinessException;
import com.yuesf.aireader.service.TrackingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 埋点控制器
 * 提供小程序端专用的轻量级埋点上报接口
 * 
 * 主要功能：
 * 1. 单个埋点事件上报
 * 2. 批量埋点事件上报
 * 3. 用户会话管理
 * 4. 服务健康检查
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
@Slf4j
@RestController
@RequestMapping("/v1/tracking")
@Validated
public class TrackingController {

    @Autowired
    private TrackingService trackingService;

    /**
     * 埋点事件上报
     * 
     * 接收小程序端上报的埋点事件，支持单个事件上报
     * 
     * @param request 埋点上报请求，包含用户ID、事件类型、页面路径等信息
     * @return ApiResponse<String> 上报结果，成功返回success消息，失败返回错误信息
     * @throws BusinessException 当参数验证失败或业务处理异常时抛出
     */
    @PostMapping("/report")
    public ApiResponse<String> reportEvent(@Valid @RequestBody TrackingReportRequest request) {
        log.info("接收埋点事件上报请求: userId={}, eventType={}, pagePath={}", 
                request.getUserId(), request.getEventType(), request.getPagePath());
        
        try {
            boolean success = trackingService.reportEvent(request);
            
            if (success) {
                log.info("埋点事件上报成功: userId={}, eventType={}", 
                        request.getUserId(), request.getEventType());
                return ApiResponse.success("埋点上报成功");
            } else {
                log.warn("埋点事件上报失败: userId={}, eventType={}", 
                        request.getUserId(), request.getEventType());
                return ApiResponse.error("埋点上报失败");
            }
        } catch (BusinessException e) {
            log.error("埋点事件上报业务异常: userId={}, error={}", 
                    request.getUserId(), e.getMessage());
            return ApiResponse.error("埋点上报失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("埋点事件上报系统异常: userId={}, error={}", 
                    request.getUserId(), e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 批量埋点事件上报
     * 
     * 接收小程序端批量上报的埋点事件，提高上报效率，减少网络请求次数
     * 
     * @param request 批量埋点上报请求，包含事件列表和批次ID
     * @return ApiResponse<String> 上报结果，包含成功上报的数量
     * @throws BusinessException 当参数验证失败或业务处理异常时抛出
     */
    @PostMapping("/report/batch")
    public ApiResponse<String> reportBatchEvents(@Valid @RequestBody TrackingBatchReportRequest request) {
        log.info("接收批量埋点事件上报请求: batchId={}, eventCount={}", 
                request.getBatchId(), request.getEvents() != null ? request.getEvents().size() : 0);
        
        try {
            int successCount = trackingService.batchReportEvents(request);
            
            log.info("批量埋点事件上报完成: batchId={}, successCount={}", 
                    request.getBatchId(), successCount);
            return ApiResponse.success("批量埋点上报成功，成功数量: " + successCount);
        } catch (BusinessException e) {
            log.error("批量埋点事件上报业务异常: batchId={}, error={}", 
                    request.getBatchId(), e.getMessage());
            return ApiResponse.error("批量埋点上报失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("批量埋点事件上报系统异常: batchId={}, error={}", 
                    request.getBatchId(), e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 结束用户会话
     * 
     * 当用户退出小程序或会话超时时，调用此接口结束用户会话
     * 
     * @param sessionId 会话ID，用于标识用户会话
     * @return ApiResponse<String> 操作结果，成功或失败信息
     * @throws BusinessException 当会话不存在或操作失败时抛出
     */
    @PostMapping("/session/end")
    public ApiResponse<String> endSession(@RequestParam String sessionId) {
        log.info("接收结束会话请求: sessionId={}", sessionId);
        
        try {
            boolean success = trackingService.endSession(sessionId);
            
            if (success) {
                log.info("会话结束成功: sessionId={}", sessionId);
                return ApiResponse.success("会话结束成功");
            } else {
                log.warn("会话结束失败或会话不存在: sessionId={}", sessionId);
                return ApiResponse.error("会话结束失败或会话不存在");
            }
        } catch (BusinessException e) {
            log.error("结束会话业务异常: sessionId={}, error={}", sessionId, e.getMessage());
            return ApiResponse.error("结束会话失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("结束会话系统异常: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return ApiResponse.error("系统异常，请稍后重试");
        }
    }

    /**
     * 健康检查接口
     * 
     * 用于检查埋点服务的运行状态，供监控系统调用
     * 
     * @return ApiResponse<String> 服务状态信息
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        log.debug("埋点服务健康检查");
        return ApiResponse.success("埋点服务运行正常");
    }
}