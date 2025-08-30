package com.yuesf.aireader.controller;

import com.yuesf.aireader.annotation.RequireAuth;
import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.dto.ReportBatchDeleteRequest;
import com.yuesf.aireader.dto.ReportCreateRequest;
import com.yuesf.aireader.dto.ReportListRequest;
import com.yuesf.aireader.dto.ReportListResponse;
import com.yuesf.aireader.dto.ReportUpdateRequest;
import com.yuesf.aireader.entity.Report;
import com.yuesf.aireader.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报告查询接口控制器（后台管理使用）
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequireAuth(requireAdmin = true) // 整个控制器需要管理员权限
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 获取/搜索报告列表
     * POST /reports
     * 支持基础查询和高级搜索
     */
    @PostMapping("/reports")
    public ApiResponse<ReportListResponse> getReportList(@RequestBody ReportListRequest request) {
        try {
            log.info("后台请求报告列表，参数: {}", request);
            
            // 参数验证
            if (request.getPage() == null || request.getPage() < 1) {
                request.setPage(1);
            }
            if (request.getPageSize() == null || request.getPageSize() < 1 || request.getPageSize() > 50) {
                request.setPageSize(10);
            }
            
            // 设置默认排序
            if (request.getSortBy() == null || request.getSortBy().trim().isEmpty()) {
                request.setSortBy("publishDate");
            }
            if (request.getSortOrder() == null || request.getSortOrder().trim().isEmpty()) {
                request.setSortOrder("desc");
            }
            
            ReportListResponse response = reportService.getReportList(request);
            log.info("后台报告列表查询成功，返回 {} 条记录", response.getTotal());
            
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("后台报告列表查询失败", e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 获取报告详情
     * GET /reports/{id}
     */
    @GetMapping("/reports/{id}")
    public ApiResponse<Report> getReportById(@PathVariable String id) {
        try {
            log.info("后台请求报告详情，ID: {}", id);
            
            if (id == null || id.trim().isEmpty()) {
                return ApiResponse.error(400, "报告ID不能为空");
            }
            
            Report report = reportService.getReportById(id);
            if (report == null) {
                return ApiResponse.error(404, "报告不存在");
            }
            
            log.info("后台报告详情查询成功，ID: {}", id);
            return ApiResponse.success(report);
            
        } catch (Exception e) {
            log.error("后台报告详情查询失败，ID: {}", id, e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 创建报告
     * POST /reports/create
     */
    @PostMapping("/reports/create")
    public ApiResponse<Report> createReport(@RequestBody ReportCreateRequest request) {
        try {
            log.info("后台创建报告，标题: {}", request.getTitle());
            
            Report created = reportService.createReport(request);
            log.info("后台报告创建成功，ID: {}", created.getId());
            
            return ApiResponse.success(created);
        } catch (IllegalArgumentException e) {
            log.warn("后台报告创建参数错误: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("后台报告创建失败", e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 更新报告
     * POST /report/update/{id}
     */
    @PostMapping("/report/update/{id}")
    public ApiResponse<Report> updateReport(@PathVariable String id, @RequestBody ReportUpdateRequest request) {
        try {
            log.info("后台更新报告，ID: {}", id);
            
            if (id == null || id.trim().isEmpty()) {
                return ApiResponse.error(400, "报告ID不能为空");
            }
            
            // 设置ID到请求对象中
            request.setId(id);
            
            Report updated = reportService.updateReport(request);
            if (updated != null) {
                log.info("后台报告更新成功，ID: {}", id);
                return ApiResponse.success(updated);
            } else {
                log.error("后台报告更新失败，ID: {}", id);
                return ApiResponse.error(500, "更新失败");
            }
        } catch (IllegalArgumentException e) {
            log.warn("后台报告更新参数错误: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("后台报告更新失败，ID: {}", id, e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }


    /**
     * 删除单个报告
     * POST /reports/delete/{id}
     */
    @PostMapping("/reports/delete/{id}")
    public ApiResponse<Void> deleteReport(@PathVariable String id) {
        try {
            log.info("后台删除报告，ID: {}", id);
            
            if (id == null || id.trim().isEmpty()) {
                return ApiResponse.error(400, "报告ID不能为空");
            }
            
            int deleted = reportService.deleteById(id);
            if (deleted > 0) {
                log.info("后台报告删除成功，ID: {}", id);
                return ApiResponse.success(null);
            } else {
                log.warn("后台报告删除失败，报告不存在，ID: {}", id);
                return ApiResponse.error(404, "报告不存在");
            }
            
        } catch (Exception e) {
            log.error("后台报告删除失败，ID: {}", id, e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 批量删除报告
     * POST /reports/batch-delete
     */
    @PostMapping("/reports/batch-delete")
    public ApiResponse<Void> batchDeleteReports(@RequestBody ReportBatchDeleteRequest request) {
        try {
            log.info("后台批量删除报告，数量: {}", request.getIds().size());
            
            if (request.getIds() == null || request.getIds().isEmpty()) {
                return ApiResponse.error(400, "报告ID列表不能为空");
            }
            
            reportService.batchDelete(request);
            log.info("后台批量删除报告成功，数量: {}", request.getIds().size());
            
            return ApiResponse.success(null);
            
        } catch (Exception e) {
            log.error("后台批量删除报告失败", e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 生成报告摘要
     * POST /reports/{id}/generate-summary
     */
    @PostMapping("/reports/{id}/generate-summary")
    public ApiResponse<String> generateReportSummary(@PathVariable String id) {
        try {
            log.info("后台生成报告摘要，ID: {}", id);
            
            if (id == null || id.trim().isEmpty()) {
                return ApiResponse.error(400, "报告ID不能为空");
            }
            
            String summary = reportService.generateReportSummary(id);
            if (summary != null && !summary.trim().isEmpty()) {
                log.info("后台报告摘要生成成功，ID: {}", id);
                return ApiResponse.success(summary);
            } else {
                log.warn("后台报告摘要生成失败，ID: {}", id);
                return ApiResponse.error(500, "摘要生成失败，请检查报告文件是否存在");
            }
            
        } catch (Exception e) {
            log.error("后台报告摘要生成失败，ID: {}", id, e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }
}
