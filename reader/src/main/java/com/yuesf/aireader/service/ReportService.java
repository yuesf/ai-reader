package com.yuesf.aireader.service;

import com.yuesf.aireader.dto.ReportListRequest;
import com.yuesf.aireader.dto.ReportCreateRequest;
import com.yuesf.aireader.dto.ReportBatchDeleteRequest;
import com.yuesf.aireader.dto.ReportListResponse;
import com.yuesf.aireader.entity.Report;
import com.yuesf.aireader.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 报告业务逻辑类（MyBatis XML 查询）
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    @Autowired
    private ReportMapper reportMapper;

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

    public Report getReportById(String id) {
        return reportMapper.selectById(id);
    }

    @Transactional
    public Report createReport(ReportCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
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
        report.setReportFileUrl(request.getReportFileUrl());
        report.setReportFileName(request.getReportFileName());
        report.setReportFileSize(request.getReportFileSize());

        reportMapper.insertReport(report);
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            reportMapper.insertReportTags(report.getId(), request.getTags());
        }
        return reportMapper.selectById(report.getId());
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
