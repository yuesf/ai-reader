package com.yuesf.aireader.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 报告实体类
 */
@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @Column(name = "report_file_id")
    private String reportFileId;
    
    @Column(name = "report_file_url")
    private String reportFileUrl;
    
    @Column(name = "report_file_name")
    private String reportFileName;
    
    @Column(name = "report_file_size")
    private String reportFileSize;

    /**
     * 获取报告标题，如果为空则返回默认值
     * @return 报告标题
     */
    public String getTitleWithDefault() {
        return StringUtils.defaultIfBlank(title, "未命名报告");
    }

    /**
     * 检查报告是否免费
     * @return 是否免费
     */
    public boolean isFree() {
        return Boolean.TRUE.equals(isFree);
    }

    /**
     * 获取报告摘要，如果为空则返回默认值
     * @return 报告摘要
     */
    public String getSummaryWithDefault() {
        return StringUtils.defaultIfBlank(summary, "暂无摘要");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("title", getTitleWithDefault())
                .append("source", source)
                .append("category", category)
                .append("publishDate", publishDate)
                .append("isFree", isFree)
                .toString();
    }
}
