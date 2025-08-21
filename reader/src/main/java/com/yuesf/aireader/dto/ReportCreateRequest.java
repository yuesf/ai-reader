package com.yuesf.aireader.dto;

import java.util.List;

/**
 * 创建报告请求DTO
 */
public class ReportCreateRequest {
    private String title;
    private String summary;
    private String source;
    private String category;
    private Integer pages;
    private Long fileSize;
    private String publishDate; // yyyy-MM-dd
    private String updateDate; // yyyy-MM-dd，可选
    private String thumbnail;
    private List<String> tags;
    private Boolean isFree;
    private Integer price;
    
    // 文件上传相关字段
    private String reportFileId; // 报告文件ID（必填）
    private String reportFileUrl; // 报告文件URL（从OSS上传后获得）
    private String reportFileName; // 报告文件名
    private String reportFileSize; // 报告文件大小

    public ReportCreateRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getReportFileUrl() {
        return reportFileUrl;
    }

    public void setReportFileUrl(String reportFileUrl) {
        this.reportFileUrl = reportFileUrl;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public void setReportFileName(String reportFileName) {
        this.reportFileName = reportFileName;
    }

    public String getReportFileSize() {
        return reportFileSize;
    }

    public void setReportFileSize(String reportFileSize) {
        this.reportFileSize = reportFileSize;
    }

    public String getReportFileId() {
        return reportFileId;
    }

    public void setReportFileId(String reportFileId) {
        this.reportFileId = reportFileId;
    }
}


