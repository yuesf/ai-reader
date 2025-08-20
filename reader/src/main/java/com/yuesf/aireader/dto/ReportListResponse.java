package com.yuesf.aireader.dto;

import com.yuesf.aireader.entity.Report;
import java.util.List;

/**
 * 报告列表响应DTO
 */
public class ReportListResponse {
    private Long total;
    private Integer page;
    private Integer pageSize;
    private List<Report> list;

    // 构造函数
    public ReportListResponse() {}

    public ReportListResponse(Long total, Integer page, Integer pageSize, List<Report> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }

    // Getter和Setter方法
    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<Report> getList() {
        return list;
    }

    public void setList(List<Report> list) {
        this.list = list;
    }
}
