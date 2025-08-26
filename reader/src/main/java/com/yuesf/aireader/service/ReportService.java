package com.yuesf.aireader.service;

import com.yuesf.aireader.dto.ReportBatchDeleteRequest;
import com.yuesf.aireader.dto.ReportCreateRequest;
import com.yuesf.aireader.dto.ReportListRequest;
import com.yuesf.aireader.dto.ReportListResponse;
import com.yuesf.aireader.dto.ReportUpdateRequest;
import com.yuesf.aireader.entity.FileInfo;
import com.yuesf.aireader.entity.Report;
import com.yuesf.aireader.mapper.ReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 报告业务逻辑类（MyBatis XML 查询）
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private FileInfoService fileInfoService;
    @Autowired
    private ReportProcessingService reportProcessingService;

    @Autowired
    private AITextSummaryService aiTextSummaryService;

    @Autowired
    private FileUploadService fileUploadService;


    public ReportListResponse getReportList(ReportListRequest request) {
        int page = (request.getPage() == null || request.getPage() < 1) ? 1 : request.getPage();
        int pageSize = (request.getPageSize() == null || request.getPageSize() < 1 || request.getPageSize() > 50) ? 10 : request.getPageSize();
        int offset = (page - 1) * pageSize;

        String sortBy = (request.getSortBy() == null || request.getSortBy().isBlank()) ? "publishDate" : request.getSortBy();
        String sortOrder = (request.getSortOrder() == null || request.getSortOrder().isBlank()) ? "desc" : request.getSortOrder();

        // 必须有条件：若无keyword，默认近30天
        String keyword = request.getKeyword();
        LocalDate defaultStart = null;
        LocalDate defaultEnd = null;
        if (keyword == null || keyword.trim().isEmpty()) {
            defaultEnd = LocalDate.now();
            defaultStart = defaultEnd.minusDays(30);
        }

        List<Report> list = reportMapper.selectReports(
                keyword,
                request.getCategory(),
                request.getSource(),
                request.getStartDate() != null ? LocalDate.parse(request.getStartDate()) : defaultStart,
                request.getEndDate() != null ? LocalDate.parse(request.getEndDate()) : defaultEnd,
                request.getFilters() != null ? request.getFilters().getCategory() : null,
                request.getFilters() != null ? request.getFilters().getSource() : null,
                sortBy,
                sortOrder,
                offset,
                pageSize
        );

        long total = reportMapper.countReports(
                keyword,
                request.getCategory(),
                request.getSource(),
                request.getStartDate() != null ? LocalDate.parse(request.getStartDate()) : defaultStart,
                request.getEndDate() != null ? LocalDate.parse(request.getEndDate()) : defaultEnd,
                request.getFilters() != null ? request.getFilters().getCategory() : null,
                request.getFilters() != null ? request.getFilters().getSource() : null
        );

        return new ReportListResponse(total, page, pageSize, list);
    }

    public FileInfo getFileById(String id) {
        return fileInfoService.getFileInfoById(id);
    }

    public Report getReportById(String id) {
        return reportMapper.selectById(id);
    }


    @Transactional
    public Report createReport(ReportCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }

        // 验证报告文件ID
        if (request.getReportFileId() == null || request.getReportFileId().trim().isEmpty()) {
            throw new IllegalArgumentException("报告文件ID不能为空，必须上传报告文件");
        }

        // 验证文件信息是否存在且有效
        FileInfo fileInfo = fileInfoService.getFileInfoById(request.getReportFileId());
        if (fileInfo == null || !"ACTIVE".equals(fileInfo.getStatus())) {
            throw new IllegalArgumentException("报告文件信息不存在或已失效，请重新上传文件");
        }

        Report report = new Report();
        report.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        report.setTitle(request.getTitle());
        report.setSummary(request.getSummary());
        report.setSource(request.getSource());
        report.setCategory(request.getCategory());
        report.setPages(request.getPages());
        report.setFileSize(request.getFileSize());
        report.setPublishDate(request.getPublishDate() != null && !request.getPublishDate().isBlank() ? LocalDate.parse(request.getPublishDate()) : null);
        report.setUpdateDate(request.getUpdateDate() != null && !request.getUpdateDate().isBlank() ? LocalDate.parse(request.getUpdateDate()) : null);
        report.setThumbnail(request.getThumbnail());
        report.setTags(request.getTags());
        report.setDownloadCount(0);
        report.setViewCount(0);
        report.setIsFree(request.getIsFree() != null ? request.getIsFree() : Boolean.TRUE);
        report.setPrice(request.getPrice() != null ? request.getPrice() : 0);

        // 设置文件上传相关字段
        report.setReportFileId(request.getReportFileId());
        report.setReportFileUrl(fileInfo.getFileName()); // 使用fileName字段存储OSS文件路径
        report.setReportFileName(fileInfo.getOriginalName());
        report.setReportFileSize(String.valueOf(fileInfo.getFileSize()));

        // 如果未提供缩略图，基于PDF首图生成并回填
        try {
            if (StringUtils.isBlank(report.getThumbnail())) {
                FileInfo newFileInfo = reportProcessingService.generateAndUploadThumbnailFromPdf(fileInfo);
                String newThumbnailKey = "/v1/images/" + newFileInfo.getId();
                report.setThumbnail(newThumbnailKey);
                report.setPages(newFileInfo.getPageNums());
            }
        } catch (Exception e) {
            // 不中断创建流程，但记录错误
            log.error("生成缩略图失败: " + e.getMessage());
        }

        // 若未提供摘要，异步生成
        if (request.getSummary() == null || request.getSummary().isBlank()) {
            try {
                // 异步生成摘要，不阻塞报告创建
                CompletableFuture<String> summaryFuture = aiTextSummaryService.summarizeAsync(fileInfo.getOriginalName());
                summaryFuture.thenAccept(summary -> {
                    if (summary != null && !summary.isBlank()) {
                        updateReportSummary(report.getId(), summary);
                    }
                }).exceptionally(throwable -> {
                    log.error("异步生成摘要失败: " + throwable.getMessage());
                    return null;
                });
            } catch (Exception e) {
                log.error("启动异步摘要生成失败: " + e.getMessage());
            }
        }

        reportMapper.insertReport(report);
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            reportMapper.insertReportTags(report.getId(), request.getTags());
        }
        return reportMapper.selectById(report.getId());
    }

    /**
     * 更新报告
     */
    @Transactional
    public Report updateReport(ReportUpdateRequest request) {
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("报告ID不能为空");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }

        // 检查报告是否存在
        Report existingReport = reportMapper.selectById(request.getId());
        if (existingReport == null) {
            throw new IllegalArgumentException("报告不存在");
        }

        // 更新基本信息
        existingReport.setTitle(request.getTitle());
        existingReport.setSummary(request.getSummary());
        existingReport.setSource(request.getSource());
        existingReport.setCategory(request.getCategory());
        existingReport.setPages(request.getPages());
        existingReport.setFileSize(request.getFileSize());
        existingReport.setPublishDate(request.getPublishDate() != null && !request.getPublishDate().isBlank() ? LocalDate.parse(request.getPublishDate()) : null);
        existingReport.setUpdateDate(LocalDate.now()); // 自动设置更新时间为当前时间
        existingReport.setThumbnail(request.getThumbnail());
        existingReport.setIsFree(request.getIsFree() != null ? request.getIsFree() : existingReport.getIsFree());
        existingReport.setPrice(request.getPrice() != null ? request.getPrice() : existingReport.getPrice());

        // 如果提供了新的文件ID，更新文件信息
        if (existingReport.getReportFileId() != null && !existingReport.getReportFileId().isBlank()) {
            FileInfo fileInfo = fileInfoService.getFileInfoById(existingReport.getReportFileId());
            if (fileInfo == null || !"ACTIVE".equals(fileInfo.getStatus())) {
                throw new IllegalArgumentException("报告文件信息不存在或已失效，请重新上传文件");
            }

            existingReport.setReportFileId(request.getReportFileId());
            existingReport.setReportFileUrl(fileInfo.getFileName());
            existingReport.setReportFileName(fileInfo.getOriginalName());
            existingReport.setReportFileSize(String.valueOf(fileInfo.getFileSize()));

            // 重新生成缩略图
            try {
                FileInfo newFileInfo = reportProcessingService.generateAndUploadThumbnailFromPdf(fileInfo);
                String newThumbnailKey = "/v1/images/" + newFileInfo.getId();
                existingReport.setThumbnail(newThumbnailKey);
                existingReport.setPages(newFileInfo.getPageNums());
            } catch (Exception e) {
                log.error("重新生成缩略图失败: " + e.getMessage());
            }
        }

        // 更新标签
        if (request.getTags() != null) {
            // 先删除旧标签
            reportMapper.deleteTagsByReportId(existingReport.getId());
            // 插入新标签
            if (!request.getTags().isEmpty()) {
                reportMapper.insertReportTags(existingReport.getId(), request.getTags());
            }
        }

        // 更新报告
        reportMapper.updateReport(existingReport);

        return reportMapper.selectById(existingReport.getId());
    }

    /**
     * 更新报告摘要
     */
    @Transactional
    public void updateReportSummary(String reportId, String summary) {
        Report report = reportMapper.selectById(reportId);
        if (report != null) {
            report.setSummary(summary);
            reportMapper.updateReport(report);
        }
    }

    /**
     * 生成报告摘要
     */
    @Transactional
    public String generateReportSummary(String reportId) {
        try {
            Report report = reportMapper.selectById(reportId);
            if (report == null) {
                log.error("报告不存在，ID: {}", reportId);
                return null;
            }

            // 检查是否有报告文件
            if (report.getReportFileId() == null || report.getReportFileId().isBlank()) {
                log.error("报告没有关联文件，无法生成摘要，ID: {}", reportId);
                return null;
            }

            FileInfo fileInfo = fileInfoService.getFileInfoById(report.getReportFileId());
            if (fileInfo == null) {
                log.error("报告文件信息不存在，ID: {}", reportId);
                return null;
            }

            // 生成摘要
            String summary = aiTextSummaryService.summarize(fileInfo.getOriginalName());
            if (summary != null && !summary.isBlank()) {
                // 更新报告摘要
                report.setSummary(summary);
                reportMapper.updateReport(report);
                log.info("报告摘要生成并更新成功，ID: {}", reportId);
                return summary;
            } else {
                log.error("AI摘要生成失败，ID: {}", reportId);
                return null;
            }

        } catch (Exception e) {
            log.error("生成报告摘要失败，ID: {}", reportId, e);
            return null;
        }
    }

    /**
     * 删除报告缩略图
     */
    @Transactional
    public void deleteReportThumbnail(String reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report != null && report.getThumbnail() != null && !report.getThumbnail().isBlank()) {
            try {
                reportProcessingService.deleteThumbnail(report.getThumbnail());
                report.setThumbnail(null);
                reportMapper.updateReport(report);
            } catch (Exception e) {
                log.error("删除报告缩略图失败: " + e.getMessage());
            }
        }
    }

    /**
     * 重新生成报告缩略图
     */
    @Transactional
    public String regenerateReportThumbnail(String reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report != null && report.getReportFileId() != null) {
            try {
                FileInfo fileInfo = fileInfoService.getFileInfoById(report.getReportFileId());
                if (fileInfo != null) {
                    FileInfo newFileInfo = reportProcessingService.regenerateThumbnail(fileInfo, report.getThumbnail());
                    String newThumbnailKey = "/v1/images/" + newFileInfo.getId();
                    report.setThumbnail(newThumbnailKey);
                    reportMapper.updateReport(report);
                    return newThumbnailKey;
                }
            } catch (Exception e) {
                log.error("重新生成报告缩略图失败: " + e.getMessage());
            }
        }
        return null;
    }


    /**
     * 获取报告预览图片URL
     *
     * @param id 报告ID
     * @return 预览图片URL
     */
    public String getReportPreviewUrl(String id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("报告不存在");
        }

        // 获取PDF文件的OSS路径
        // pdf 文件临时URL
        String fileUrl = fileUploadService.generatePresignedUrl(report.getReportFileUrl(), 3600);


        // 调用文件上传服务生成预览图片URL
        return fileUploadService.convertPdfToPreviewImage(fileUrl);
    }

    @Transactional
    public int deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID不能为空");
        }
        // 先删子表，再删主表（尽管DB已设ON DELETE CASCADE，但为兼容性手动执行）
        reportMapper.deleteTagsByReportId(id);
        return reportMapper.deleteById(id);
    }

    @Transactional
    public int batchDelete(ReportBatchDeleteRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("ids不能为空");
        }
        reportMapper.batchDeleteTagsByIds(request.getIds());
        return reportMapper.batchDeleteByIds(request.getIds());
    }
}
