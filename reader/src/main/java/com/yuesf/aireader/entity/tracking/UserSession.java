package com.yuesf.aireader.entity.tracking;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 用户会话实体类
 * 记录用户在小程序中的会话信息
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 会话ID
     * 唯一标识一次用户会话
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 会话开始时间
     * 毫秒级时间戳
     */
    private Long startTime;
    
    /**
     * 会话结束时间
     * 毫秒级时间戳，可为空表示会话未结束
     */
    private Long endTime;
    
    /**
     * 页面访问数量
     */
    private Integer pageCount;
    
    /**
     * 事件触发数量
     */
    private Integer eventCount;
    
    /**
     * 会话时长
     * 单位：秒
     */
    private Integer duration;
    
    /**
     * 设备信息
     * JSON格式存储设备相关信息
     */
    private String deviceInfo;
    
    /**
     * 网络类型
     */
    private String networkType;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}