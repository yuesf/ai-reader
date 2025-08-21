package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.dto.ReportListRequest;
import com.yuesf.aireader.dto.ReportListResponse;
import com.yuesf.aireader.dto.ReportCreateRequest;
import com.yuesf.aireader.dto.ReportBatchDeleteRequest;
import com.yuesf.aireader.entity.Report;
import com.yuesf.aireader.service.ReportService;
import com.yuesf.aireader.service.FileUploadService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 报告查询接口控制器
 */
@RestController
@RequestMapping("/v1")
public class ReportController {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 获取/搜索报告列表
     * POST /reports
     * 支持基础查询和高级搜索
     */
    @PostMapping("/reports")
    public ApiResponse<ReportListResponse> getReportList(@RequestBody ReportListRequest request) {
        try {
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
            return ApiResponse.success(response);
            
        } catch (Exception e) {
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
            if (id == null || id.trim().isEmpty()) {
                return ApiResponse.error(400, "报告ID不能为空");
            }
            
            Report report = reportService.getReportById(id);
            if (report == null) {
                return ApiResponse.error(404, "报告不存在");
            }
            
            return ApiResponse.success(report);
            
        } catch (Exception e) {
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
            Report created = reportService.createReport(request);
            return ApiResponse.success(created);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 获取私有文件的临时访问URL
     * GET /reports/file/{id}
     */
    @GetMapping("/reports/file/{id}")
    public ApiResponse<String> getReportFileUrl(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ApiResponse.error(400, "报告ID不能为空");
            }
            
            Report report = reportService.getReportById(id);
            if (report == null) {
                return ApiResponse.error(404, "报告不存在");
            }
            
            if (report.getReportFileId() == null || report.getReportFileId().trim().isEmpty()) {
                return ApiResponse.error(404, "报告文件不存在");
            }
            
            // 从文件URL中提取对象键
            String baseUrl = report.getReportFileUrl();
            String objectKey = baseUrl.substring(baseUrl.lastIndexOf("/") + 1);
            objectKey = "reports/" + objectKey;
            
            String presignedUrl = fileUploadService.generatePresignedUrl(objectKey, 3600);
            return ApiResponse.success(presignedUrl);
            
        } catch (Exception e) {
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("服务运行正常");
    }

    /**
     * 删除报告（单个）
     * DELETE /reports/{id}
     */
    @DeleteMapping("/reports/{id}")
    public ApiResponse<Integer> deleteReport(@PathVariable String id) {
        try {
            int affected = reportService.deleteById(id);
            if (affected == 0) {
                return ApiResponse.error(404, "报告不存在");
            }
            return ApiResponse.success(affected);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 批量删除报告
     * POST /reports/delete
     */
    @PostMapping("/reports/delete")
    public ApiResponse<Integer> batchDelete(@RequestBody ReportBatchDeleteRequest request) {
        try {
            int affected = reportService.batchDelete(request);
            return ApiResponse.success(affected);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }
}