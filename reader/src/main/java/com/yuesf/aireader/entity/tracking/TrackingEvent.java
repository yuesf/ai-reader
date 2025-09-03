package com.yuesf.aireader.entity.tracking;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 埋点事件实体类
 * 记录用户在小程序中的各种操作行为
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEvent {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 事件类型
     * 如: button_click, page_view, download, search等
     */
    private String eventType;
    
    /**
     * 页面路径
     * 如: /pages/index/index, /pages/reportDetail/reportDetail
     */
    private String pagePath;
    
    /**
     * 元素ID
     * 如: search_btn, download_btn, report_card
     */
    private String elementId;
    
    /**
     * 元素文本
     * 如: "搜索", "下载", "查看详情"
     */
    private String elementText;
    
    /**
     * 自定义属性
     * JSON格式存储额外的事件属性
     */
    private String properties;
    
    /**
     * 事件时间戳
     * 毫秒级时间戳
     */
    private Long timestamp;
    
    /**
     * 设备信息
     * JSON格式存储设备相关信息
     */
    private String deviceInfo;
    
    /**
     * 网络类型
     * 如: wifi, 4g, 5g等
     */
    private String networkType;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}