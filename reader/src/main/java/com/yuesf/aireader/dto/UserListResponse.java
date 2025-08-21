package com.yuesf.aireader.dto;

import com.yuesf.aireader.entity.AdminUser;
import java.util.List;

public class UserListResponse {
    private Long total;
    private Integer page;
    private Integer pageSize;
    private List<AdminUser> list;

    public UserListResponse() {}

    public UserListResponse(Long total, Integer page, Integer pageSize, List<AdminUser> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }

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

    public List<AdminUser> getList() {
        return list;
    }

    public void setList(List<AdminUser> list) {
        this.list = list;
    }
}


