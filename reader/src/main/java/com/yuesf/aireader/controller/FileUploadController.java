package com.yuesf.aireader.controller;

import com.yuesf.aireader.annotation.RequireAuth;
import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.entity.FileInfo;
import com.yuesf.aireader.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequireAuth(requireAdmin = true) // 整个控制器需要管理员权限
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 上传报告文件
     * POST /upload/report
     */
    @PostMapping("/upload/report")
    public ApiResponse<Map<String, String>> uploadReportFile(@RequestParam("file") MultipartFile file) {
        try {
            log.info("上传报告文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
            
            FileInfo fileInfo = fileUploadService.uploadReportFile(file, null);
            Map<String, String> result = new HashMap<>();
            result.put("fileId", fileInfo.getId());
            result.put("filename", file.getOriginalFilename());
            result.put("size", String.valueOf(file.getSize()));
            
            log.info("报告文件上传成功，文件ID: {}", fileInfo.getId());
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("报告文件上传参数错误: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (IOException e) {
            log.error("报告文件上传IO错误", e);
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("报告文件上传失败", e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 上传报告文件并返回文件信息（新建报告专用）
     * POST /upload/report/info
     */
    @PostMapping("/upload/report/info")
    public ApiResponse<FileInfo> uploadReportFileWithInfo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadUserId", required = false) String uploadUserId) {
        try {
            log.info("上传报告文件并返回信息: {}, 用户ID: {}", file.getOriginalFilename(), uploadUserId);
            
            FileInfo fileInfo = fileUploadService.uploadReportFile(file, uploadUserId);
            log.info("报告文件上传成功，文件ID: {}", fileInfo.getId());
            
            return ApiResponse.success(fileInfo);
        } catch (IllegalArgumentException e) {
            log.warn("报告文件上传参数错误: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (IOException e) {
            log.error("报告文件上传IO错误", e);
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("报告文件上传失败", e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 上传图片文件
     * POST /upload/image
     */
    @PostMapping("/upload/image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("上传图片文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
            
            FileInfo fileInfo = fileUploadService.uploadImage(file);
            Map<String, String> result = new HashMap<>();
            result.put("fileId", fileInfo.getId());
            result.put("filename", fileInfo.getFileName());
            // 返回后端代理访问URL，避免将带签名的OSS地址暴露给前端
            result.put("thumbnail", "/v1/images/" + fileInfo.getId());
            
            log.info("图片文件上传成功，文件ID: {}", fileInfo.getId());
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("图片文件上传参数错误: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (IOException e) {
            log.error("图片文件上传IO错误", e);
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("图片文件上传失败", e);
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 公开访问图片：通过后端从OSS读取并回写响应，避免暴露带AK的签名URL
     * GET /images/{id}
     */
    @GetMapping("/images/{id}")
    @RequireAuth(requireAdmin = false) // 图片访问只需要登录，不需要管理员权限
    public void getImage(@PathVariable("id") String id, jakarta.servlet.http.HttpServletResponse response) {
        try {
            log.debug("请求图片文件，ID: {}", id);
            
            FileInfo fileInfo = fileUploadService.getFileInfoById(id);
            if (fileInfo == null) {
                log.warn("图片文件不存在，ID: {}", id);
                response.setStatus(404);
                return;
            }

            // 通过服务读取OSS对象并写回
            fileUploadService.writeObjectToResponse(fileInfo.getFileName(), response);
            log.debug("图片文件响应成功，ID: {}", id);
        } catch (Exception e) {
            log.error("图片文件响应失败，ID: {}", id, e);
            try {
                response.setStatus(500);
            } catch (Exception ignored) {}
        }
    }

    /**
     * 通用文件上传
     * POST /upload/file
     */
    @PostMapping("/upload/file")
    public ApiResponse<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "files") String folder) {
        try {
            FileInfo fileInfo = fileUploadService.uploadFile(file, folder, null);
//            String fileUrl = fileUploadService.uploadFile(file, folder);
            Map<String, String> result = new HashMap<>();
            result.put("fileId", fileInfo.getId());
            result.put("filename", file.getOriginalFilename());
            result.put("size", String.valueOf(file.getSize()));
            result.put("folder", folder);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (IOException e) {
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     * DELETE /upload/file
     */
    @DeleteMapping("/upload/file")
    public ApiResponse<String> deleteFile(@RequestParam("fileId") String fileId) {
        try {
            fileUploadService.deleteFile(fileId);
            return ApiResponse.success("文件删除成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "文件删除失败: " + e.getMessage());
        }
    }
}
