package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.dto.ReportListRequest;
import com.yuesf.aireader.dto.ReportListResponse;
import com.yuesf.aireader.service.FileUploadService;
import com.yuesf.aireader.service.ReportService;
import com.yuesf.aireader.vo.MiniReportInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序报告查询接口控制器
 * 提供简化的报告列表查询，适合移动端使用
 */
@Slf4j
@RestController
@RequestMapping("/v1/mini")
public class MiniProgramReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 获取小程序报告列表（简化版）
     * POST /mini/reports
     * 支持基础查询，返回适合移动端的数据结构
     */
    @PostMapping("/reports")
    public ApiResponse<ReportListResponse> getMiniReportList(@RequestBody ReportListRequest request) {
        try {
            log.info("小程序请求报告列表，参数: {}", request);

            // 参数验证
            if (request.getPage() == null || request.getPage() < 1) {
                request.setPage(1);
            }
            if (request.getPageSize() == null || request.getPageSize() < 1 || request.getPageSize() > 20) {
                request.setPageSize(10); // 小程序限制最大20条
            }

            // 设置默认排序
            if (request.getSortBy() == null || request.getSortBy().trim().isEmpty()) {
                request.setSortBy("publishDate");
            }
            if (request.getSortOrder() == null || request.getSortOrder().trim().isEmpty()) {
                request.setSortOrder("desc");
            }

            ReportListResponse response = reportService.getReportList(request);
            log.info("小程序报告列表查询成功，返回 {} 条记录", response.getTotal());

            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("小程序报告列表查询失败", e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 获取小程序报告详情（简化版）
     * GET /mini/reports/{id}
     * 返回适合移动端显示的报告信息
     */
    @GetMapping("/reports/{id}")
    public ApiResponse<Object> getMiniReportById(@PathVariable String id) {
        try {
            log.info("小程序请求报告详情，ID: {}", id);

            if (id == null || id.trim().isEmpty()) {
                return ApiResponse.error(400, "报告ID不能为空");
            }

            var report = reportService.getReportById(id);
            if (report == null) {
                return ApiResponse.error(404, "报告不存在");
            }

            // 构建适合小程序的简化数据结构
            var miniReport = new MiniReportInfo();
            miniReport.setId(report.getId());
            miniReport.setTitle(report.getTitle());
            miniReport.setSummary(report.getSummary());
            miniReport.setSource(report.getSource());
            miniReport.setCategory(report.getCategory());
            miniReport.setPages(report.getPages());
            miniReport.setPublishDate(report.getPublishDate());
            miniReport.setThumbnail(report.getThumbnail());
            miniReport.setTags(report.getTags());
            miniReport.setIsFree(report.getIsFree());
            miniReport.setPrice(report.getPrice());
            miniReport.setDownloadCount(report.getDownloadCount());
            miniReport.setViewCount(report.getViewCount());
            miniReport.setReportFileId(report.getReportFileId());

            log.info("小程序报告详情查询成功，ID: {}", id);
            return ApiResponse.success(miniReport);

        } catch (Exception e) {
            log.error("小程序报告详情查询失败，ID: {}", id, e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 获取报告文件的临时访问URL（供小程序使用）
     * GET /mini/reports/preview/{id}
     */
    @GetMapping("/reports/preview/{id}")
    public ApiResponse<String> getMiniReportFileUrl(@PathVariable String id) {
        try {
            log.info("小程序请求报告文件URL，ID: {}", id);

            if (id == null || id.trim().isEmpty()) {
                return ApiResponse.error(400, "报告ID不能为空");
            }

            var report = reportService.getReportById(id);
            if (report == null) {
                return ApiResponse.error(404, "报告不存在");
            }

//            String fileUrl = fileUploadService.generatePresignedUrl(report.getReportFileUrl(), 3600);
//            log.info("小程序报告文件URL生成成功，ID: {}, fileUrl={}", id, fileUrl);

            return ApiResponse.success("/v1/doc/" + report.getId());

        } catch (Exception e) {
            log.error("小程序报告文件URL生成失败，ID: {}", id, e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }


}