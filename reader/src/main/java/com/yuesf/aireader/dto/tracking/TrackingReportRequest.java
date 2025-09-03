package com.yuesf.aireader.dto.tracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 埋点上报请求DTO
 * 小程序端上报埋点数据使用
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingReportRequest {
    
    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
    
    /**
     * 事件类型
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;
    
    /**
     * 页面路径
     */
    @NotBlank(message = "页面路径不能为空")
    private String pagePath;
    
    /**
     * 元素ID
     */
    private String elementId;
    
    /**
     * 元素文本
     */
    private String elementText;
    
    /**
     * 自定义属性
     */
    private Map<String, Object> properties;
    
    /**
     * 事件时间戳
     */
    @NotNull(message = "时间戳不能为空")
    private Long timestamp;
    
    /**
     * 设备信息
     */
    private Map<String, Object> deviceInfo;
    
    /**
     * 网络类型
     */
    private String networkType;
}