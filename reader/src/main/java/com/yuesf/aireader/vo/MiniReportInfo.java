package com.yuesf.aireader.vo;

import lombok.Getter;
import lombok.Setter;

/**
 *小程序报告信息简化类
 * @author yuesf
 * @date 2025/8/23
 */
@Setter
@Getter
public class MiniReportInfo {
    /**
     * 报告ID
     */
    private String id;

    /**
     * 报告标题
     */
    private String title;

    /**
     * 报告摘要
     */
    private String summary;

    /**
     * 报告来源
     */
    private String source;

    /**
     * 报告分类
     */
    private String category;

    /**
     * 报告页数
     */
    private Integer pages;
    private String reportFileId;

    /**
     * 报告发布日期
     */
    private java.time.LocalDate publishDate;

    /**
     * 报告缩略图
     */
    private String thumbnail;

    /**
     * 报告标签列表
     */
    private java.util.List<String> tags;

    /**
     * 是否免费
     */
    private Boolean isFree;

    /**
     * 报告价格
     */
    private Integer price;

    /**
     * 报告下载次数
     */
    private Integer downloadCount;

    /**
     * 报告浏览次数
     */
    private Integer viewCount;
}
