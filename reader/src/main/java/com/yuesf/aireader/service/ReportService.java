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
import org.springframework.scheduling.annotation.Async;
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

    @Autowired
    private AliyunDocumentIntelligenceService documentIntelligenceService;


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
//        if (keyword == null || keyword.trim().isEmpty()) {
//            defaultEnd = LocalDate.now();
//            defaultStart = defaultEnd.minusDays(30);
//        }

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
                fileInfo.setPageNums(newFileInfo.getPageNums());
                fileInfoService.updateFileInfo(fileInfo);
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
        if (StringUtils.isNotBlank(request.getSummary())) {
            existingReport.setSummary(request.getSummary());
        }
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
        if (request.getReportFileId() != null && !request.getReportFileId().isBlank()) {
            FileInfo fileInfo = fileInfoService.getFileInfoById(request.getReportFileId());
            if (fileInfo == null || !"ACTIVE".equals(fileInfo.getStatus())) {
                throw new IllegalArgumentException("报告文件信息不存在或已失效，请重新上传文件");
            }

            existingReport.setReportFileId(fileInfo.getId());
            existingReport.setReportFileUrl(fileInfo.getFileName());
            existingReport.setReportFileName(fileInfo.getOriginalName());
            existingReport.setReportFileSize(String.valueOf(fileInfo.getFileSize()));

            // 根据 regenerateThumbnail 参数决定是否重新生成封面
            Boolean shouldRegenerate = request.getRegenerateThumbnail();
            if (shouldRegenerate == null || shouldRegenerate) {
                // 默认生成封面，或明确要求生成
                log.info("开始重新生成封面，报告ID: {}", existingReport.getId());
                try {
                    FileInfo newFileInfo = reportProcessingService.generateAndUploadThumbnailFromPdf(fileInfo);
                    String newThumbnailKey = "/v1/images/" + newFileInfo.getId();
                    existingReport.setThumbnail(newThumbnailKey);
                    existingReport.setPages(newFileInfo.getPageNums());
                    fileInfo.setPageNums(newFileInfo.getPageNums());
                    fileInfoService.updateFileInfo(fileInfo);
                    log.info("重新生成封面成功，报告ID: {}, 封面: {}", existingReport.getId(), newThumbnailKey);
                } catch (Exception e) {
                    log.error("重新生成缩略图失败: " + e.getMessage());
                }
            } else {
                log.info("用户选择不重新生成封面，保留原封面，报告ID: {}", existingReport.getId());
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
     * 生成报告摘要(异步)
     * 使用阿里云文档智能服务进行PDF解析,然后使用通义API生成摘要
     */
    @Transactional
    @Async
    public void generateReportSummary(String reportId) {
        try {
            Report report = reportMapper.selectById(reportId);
            if (report == null) {
                log.error("报告不存在，ID: {}", reportId);
                return;
            }

            // 检查是否有报告文件
            if (report.getReportFileId() == null || report.getReportFileId().isBlank()) {
                log.error("报告没有关联文件，无法生成摘要，ID: {}", reportId);
                updateSummaryStatus(reportId, "FAILED");
                return;
            }

            FileInfo fileInfo = fileInfoService.getFileInfoById(report.getReportFileId());
            if (fileInfo == null) {
                log.error("报告文件信息不存在，ID: {}", reportId);
                updateSummaryStatus(reportId, "FAILED");
                return;
            }

            log.info("开始为报告生成摘要，ID: {}, 文件名: {}", reportId, fileInfo.getOriginalName());
            
            // 更新状态为生成中
            updateSummaryStatus(reportId, "GENERATING");

            // 步骤1: 从OSS获取PDF文件URL
            String ossFileUrl = getOssFileUrl(fileInfo);
            if (ossFileUrl == null) {
                log.error("无法获取OSS文件URL，报告ID: {}", reportId);
                updateSummaryStatus(reportId, "FAILED");
                return;
            }

            // 步骤2: 使用阿里云文档智能服务解析PDF内容
            String documentContent = parseDocumentWithAliyun(ossFileUrl, reportId);
            if (documentContent == null || documentContent.isBlank()) {
                log.error("PDF文档解析失败或内容为空，报告ID: {}", reportId);
                updateSummaryStatus(reportId, "FAILED");
                return;
            }

            log.info("PDF文档解析成功，内容长度: {}, 报告ID: {}", documentContent.length(), reportId);

            // 步骤3: 使用通义API生成摘要
            String summary = aiTextSummaryService.summarize(documentContent);
            if (summary != null && !summary.isBlank()) {
                // 更新报告摘要
                report.setSummary(summary);
                report.setSummaryStatus("COMPLETED");
                reportMapper.updateReport(report);
                log.info("报告摘要生成并更新成功，摘要长度: {}, 报告ID: {}", summary.length(), reportId);
            } else {
                log.error("AI摘要生成失败，报告ID: {}", reportId);
                updateSummaryStatus(reportId, "FAILED");
            }

        } catch (Exception e) {
            log.error("生成报告摘要失败，报告ID: {}", reportId, e);
            updateSummaryStatus(reportId, "FAILED");
        }
    }
    
    /**
     * 更新摘要生成状态
     */
    @Transactional
    public void updateSummaryStatus(String reportId, String status) {
        Report report = reportMapper.selectById(reportId);
        if (report != null) {
            report.setSummaryStatus(status);
            reportMapper.updateReport(report);
            log.info("更新报告摘要状态: {}, 报告ID: {}", status, reportId);
        }
    }

    /**
     * 获取OSS文件URL
     */
    private String getOssFileUrl(FileInfo fileInfo) {
        try {
            // 使用fileInfo.getFileName()获取OSS文件路径
            String ossFilePath = fileInfo.getFileName();
            if (ossFilePath == null || ossFilePath.isBlank()) {
                log.error("OSS文件路径为空，文件ID: {}", fileInfo.getId());
                return null;
            }

            // 生成临时访问URL（有效期1小时）
            String presignedUrl = fileUploadService.generatePresignedUrl(ossFilePath, 3600);
            log.info("生成OSS临时访问URL成功，文件: {}", ossFilePath);
            return presignedUrl;

        } catch (Exception e) {
            log.error("生成OSS文件URL失败，文件ID: {}", fileInfo.getId(), e);
            return null;
        }
    }

    /**
     * 使用阿里云文档智能服务解析PDF文档
     */
    private String parseDocumentWithAliyun(String ossFileUrl, String reportId) {
        try {
            log.info("开始使用阿里云文档智能服务解析PDF，报告ID: {}", reportId);

            // 同步解析文档，最大等待时间300秒（5分钟）
            String documentContent = documentIntelligenceService.parseDocumentSync(ossFileUrl, 300);

            if (documentContent != null && !documentContent.isBlank()) {
                log.info("阿里云文档智能服务解析成功，内容长度: {}, 报告ID: {}", documentContent.length(), reportId);
                return documentContent;
            } else {
                log.warn("阿里云文档智能服务解析结果为空，报告ID: {}", reportId);
                return null;
            }

        } catch (Exception e) {
            log.error("阿里云文档智能服务解析失败，报告ID: {}", reportId, e);
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
        
        // 获取报告信息，用于删除关联的OSS文件
        Report report = reportMapper.selectById(id);
        if (report == null) {
            log.warn("报告不存在，ID: {}", id);
            return 0;
        }
        
        try {
            // 1. 删除OSS上的报告文件
            if (report.getReportFileId() != null && !report.getReportFileId().trim().isEmpty()) {
                log.info("删除报告文件，fileId: {}", report.getReportFileId());
                fileUploadService.deleteFile(report.getReportFileId());
            }
            
            // 2. 删除OSS上的缩略图文件
            if (report.getThumbnail() != null && !report.getThumbnail().trim().isEmpty()) {
                // 从缩略图URL中提取文件ID (格式: /v1/images/{fileId})
                String thumbnailPath = report.getThumbnail();
                if (thumbnailPath.startsWith("/v1/images/")) {
                    String thumbnailFileId = thumbnailPath.substring("/v1/images/".length());
                    log.info("删除缩略图文件，fileId: {}", thumbnailFileId);
                    fileUploadService.deleteFile(thumbnailFileId);
                }
            }
            
        } catch (Exception e) {
            log.error("删除OSS文件失败，报告ID: {}, 错误: {}", id, e.getMessage());
            // 继续执行数据库删除，不因为OSS删除失败而中断
        }
        
        // 3. 删除数据库记录
        // 先删除标签（子表）
        reportMapper.deleteTagsByReportId(id);
        // 再删除报告（主表）
        int deleted = reportMapper.deleteById(id);
        
        if (deleted > 0) {
            log.info("报告删除成功，ID: {}", id);
        } else {
            log.warn("报告删除失败，ID: {}", id);
        }
        
        return deleted;
    }

    @Transactional
    public int batchDelete(ReportBatchDeleteRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("ids不能为空");
        }
        
        int totalDeleted = 0;
        
        // 逐个删除以确保OSS文件也被清理
        for (String id : request.getIds()) {
            try {
                int deleted = deleteById(id);
                totalDeleted += deleted;
            } catch (Exception e) {
                log.error("批量删除中单个报告删除失败，ID: {}, 错误: {}", id, e.getMessage());
                // 继续删除其他报告，不因为单个失败而中断整个批量操作
            }
        }
        
        log.info("批量删除完成，成功删除 {} 个报告，共 {} 个", totalDeleted, request.getIds().size());
        return totalDeleted;
    }

    /**
     * 验证报告是否可以发布到公众号
     * 检查报告存在性和摘要完整性
     */
    public void validateReportForPublish(String reportId) {
        if (reportId == null || reportId.trim().isEmpty()) {
            throw new IllegalArgumentException("报告ID不能为空");
        }

        // 检查报告是否存在
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new IllegalArgumentException("报告不存在，请检查报告ID");
        }

        // 检查报告摘要是否存在
        if (report.getSummary() == null || report.getSummary().trim().isEmpty()) {
            throw new IllegalArgumentException("报告摘要不存在，请先生成报告摘要后再发布到公众号");
        }

        // 检查报告标题是否存在
        if (report.getTitle() == null || report.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("报告标题不能为空");
        }

        // 检查报告封面是否存在
        if (report.getThumbnail() == null || report.getThumbnail().trim().isEmpty()) {
            throw new IllegalArgumentException("报告封面不存在，无法发布到公众号");
        }

        log.info("报告验证通过，可以发布到公众号，报告ID: {}, 标题: {}", reportId, report.getTitle());
    }

    /**
     * 获取报告用于发布公众号
     */
    public Report getReportForPublish(String reportId) {
        validateReportForPublish(reportId);
        return reportMapper.selectById(reportId);
    }
}
