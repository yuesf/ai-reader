package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author yuesf
 * @date 2025/8/26
 */
@RestController
@Slf4j
public class Homecontroller {
    /**
     * 健康检查接口
     * GET /health
     * 用于测试PDF流服务是否正常运行
     */
    @GetMapping({"/health", "/"})
    public ApiResponse<String> healthCheck() {
        log.info("服务健康检查-运行正常");
        return ApiResponse.success("服务运行正常");
    }
}
