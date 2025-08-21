package com.yuesf.aireader.dto;

/**
 * 获取/搜索用户列表请求DTO
 */
public class UserListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword; // 匹配 username/displayName
    private Integer status; // 1 启用 0 禁用

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

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}


