package com.yuesf.aireader.dto;

import java.util.List;

/**
 * 批量删除报告请求DTO
 */
public class ReportBatchDeleteRequest {
    private List<String> ids;

    public ReportBatchDeleteRequest() {}

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}


