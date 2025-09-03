package com.yuesf.aireader.dto.tracking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 批量埋点上报请求DTO
 * 支持小程序端批量上报埋点数据
 *
 * @author AI-Reader Team
 * @since 2025-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingBatchReportRequest {
    
    /**
     * 埋点事件列表
     */
    @NotEmpty(message = "埋点事件列表不能为空")
    @Valid
    private List<TrackingReportRequest> events;
    
    /**
     * 批次ID
     * 用于标识本次批量上报
     */
    private String batchId;
}