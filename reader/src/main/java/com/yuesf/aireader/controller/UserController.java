package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.dto.UserListRequest;
import com.yuesf.aireader.dto.UserListResponse;
import com.yuesf.aireader.entity.AdminUser;
import com.yuesf.aireader.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/list")
    public ApiResponse<UserListResponse> list(@RequestBody UserListRequest req) {
        return ApiResponse.success(userService.listUsers(req));
    }

    @PostMapping("/create")
    public ApiResponse<Integer> create(@RequestBody AdminUser user) {
        int n = userService.createUser(user);
        return ApiResponse.success(n);
    }

    @PostMapping("/update")
    public ApiResponse<Integer> update(@RequestBody AdminUser user) {
        int n = userService.updateUser(user);
        return ApiResponse.success(n);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Integer> delete(@PathVariable Integer id) {
        int n = userService.deleteUser(id);
        return ApiResponse.success(n);
    }
}


