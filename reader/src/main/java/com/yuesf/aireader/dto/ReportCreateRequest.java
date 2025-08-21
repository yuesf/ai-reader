package com.yuesf.aireader.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 创建报告请求DTO
 */
@Setter
@Getter
public class ReportCreateRequest {
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

  /**
   * 报告文件大小（字节）
   */
  private Long fileSize;

  /**
   * 报告发布日期（yyyy-MM-dd）
   */
  private String publishDate;

  /**
   * 报告更新日期（yyyy-MM-dd），可选
   */
  private String updateDate;

  /**
   * 报告缩略图
   */
  private String thumbnail;

  /**
   * 报告标签列表
   */
  private List<String> tags;

  /**
   * 是否免费
   */
  private Boolean isFree;

  /**
   * 报告价格
   */
  private Integer price;

  // 文件上传相关字段

  /**
   * 报告文件ID（必填）
   */
  private String reportFileId;

  /**
   * 报告文件名
   */
  private String reportFileName;

  /**
   * 报告文件大小
   */
  private String reportFileSize;


}


