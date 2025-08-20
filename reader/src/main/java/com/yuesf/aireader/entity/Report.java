package com.yuesf.aireader.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 报告实体类
 */
@Entity
@Table(name = "reports")
public class Report {
    @Id
    private String id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(length = 100)
    private String source;
    
    @Column(length = 50)
    private String category;
    
    private Integer pages;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "publish_date")
    private LocalDate publishDate;
    
    @Column(name = "update_date")
    private LocalDate updateDate;
    
    @Column(length = 500)
    private String thumbnail;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_tags", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "tag")
    private List<String> tags;
    
    @Column(name = "download_count")
    private Integer downloadCount;
    
    @Column(name = "view_count")
    private Integer viewCount;
    
    @Column(name = "is_free")
    private Boolean isFree;
    
    private Integer price;
    
    // 文件上传相关字段
    @Column(name = "report_file_url")
    private String reportFileUrl;
    
    @Column(name = "report_file_name")
    private String reportFileName;
    
    @Column(name = "report_file_size")
    private String reportFileSize;

    // 构造函数
    public Report() {}

    public Report(String id, String title, String summary, String source, String category,
                  Integer pages, Long fileSize, LocalDate publishDate, LocalDate updateDate,
                  String thumbnail, List<String> tags, Integer downloadCount, Integer viewCount,
                  Boolean isFree, Integer price, String reportFileUrl, String reportFileName, String reportFileSize) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.source = source;
        this.category = category;
        this.pages = pages;
        this.fileSize = fileSize;
        this.publishDate = publishDate;
        this.updateDate = updateDate;
        this.thumbnail = thumbnail;
        this.tags = tags;
        this.downloadCount = downloadCount;
        this.viewCount = viewCount;
        this.isFree = isFree;
        this.price = price;
        this.reportFileUrl = reportFileUrl;
        this.reportFileName = reportFileName;
        this.reportFileSize = reportFileSize;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDate publishDate) {
        this.publishDate = publishDate;
    }

    public LocalDate getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDate updateDate) {
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

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
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
}
