package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.dto.WechatLoginRequest;
import com.yuesf.aireader.dto.WechatLoginResponse;
import com.yuesf.aireader.exception.BusinessException;
import com.yuesf.aireader.service.WechatUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 小程序用户控制器
 * 提供小程序用户相关的接口服务，包括用户登录和用户信息查询
 *
 * @author yuesf
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/v1/mini/user")
@RequiredArgsConstructor
@Validated
public class MiniProgramUserController {

    private final WechatUserService wechatUserService;

    /**
     * 小程序用户登录
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     * @throws BusinessException 业务异常
     */
    @PostMapping("/login")
    public ApiResponse<WechatLoginResponse> login(@Valid @RequestBody WechatLoginRequest request) throws BusinessException {
        log.info("小程序用户登录请求: {}", request);
        WechatLoginResponse response = wechatUserService.login(request);
        log.info("小程序用户登录成功: openId={}", response.getOpenId());
        return ApiResponse.success(response);
    }

    /**
     * 获取用户信息
     *
     * @param openId 用户openId
     * @return 用户信息响应结果
     * @throws BusinessException 业务异常
     */
    @GetMapping("/info")
    public ApiResponse<WechatLoginResponse> getUserInfo(@RequestParam String openId) throws BusinessException {
        log.info("获取用户信息请求: openId={}", openId);
        WechatLoginResponse response = wechatUserService.getUserInfo(openId);
        log.info("获取用户信息成功: openId={}", openId);
        return ApiResponse.success(response);
    }
}
