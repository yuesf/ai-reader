package com.yuesf.aireader.service;

import com.yuesf.aireader.dto.UserListRequest;
import com.yuesf.aireader.dto.UserListResponse;
import com.yuesf.aireader.entity.AdminUser;
import com.yuesf.aireader.mapper.AdminUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private AdminUserMapper adminUserMapper;

    public UserListResponse listUsers(UserListRequest req) {
        int page = req.getPage() == null || req.getPage() < 1 ? 1 : req.getPage();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : req.getPageSize();
        int offset = (page - 1) * pageSize;
        Long total = adminUserMapper.countUsers(req.getKeyword(), req.getStatus());
        List<AdminUser> list = adminUserMapper.listUsers(req.getKeyword(), req.getStatus(), pageSize, offset);
        return new UserListResponse(total, page, pageSize, list);
    }

    public int createUser(AdminUser user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (user.getStatus() == null) user.setStatus(1);
        return adminUserMapper.createUser(user);
    }

    public int updateUser(AdminUser user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("ID不能为空");
        }
        return adminUserMapper.updateUser(user);
    }

    public int deleteUser(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID不能为空");
        }
        return adminUserMapper.deleteUser(id);
    }
}


