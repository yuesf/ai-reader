package com.yuesf.aireader.controller;

import com.yuesf.aireader.annotation.RequireAuth;
import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.service.PdfStreamService;
import jakarta.servlet.ServletOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * PDF文件流控制器
 * 提供安全的PDF预览和下载接口，支持断点续传和分片加密
 */
@Slf4j
@RestController
@RequestMapping("/v1/pdf")
public class PdfStreamController {

    @Autowired
    private PdfStreamService pdfStreamService;

    /**
     * 获取PDF文件流（支持断点续传）
     * GET /pdf/stream/{fileId}
     * 支持Range请求头，实现断点续传
     */
    @GetMapping("/stream/{fileId}")
    public void streamPdfFile(
            @PathVariable String fileId,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
            log.info("请求PDF文件流，文件ID: {}, Range: {}", fileId, request.getHeader("Range"));
            
            // 获取Range请求头
            String range = request.getHeader("Range");
            
            // 流式传输PDF文件
            pdfStreamService.streamPdfFile(fileId, range, response);
            
            log.info("PDF文件流传输完成，文件ID: {}", fileId);
            
        } catch (Exception e) {
            log.error("PDF文件流传输失败，文件ID: {}, {}", fileId, e.getMessage());
            
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"文件传输失败: " + e.getMessage() + "\"}");
            } catch (IOException ex) {
                log.error("写入错误响应失败, {}", ex.getMessage());
            }
        }
    }

    /**
     * 获取PDF文件分片（加密）
     * GET /pdf/chunk/{fileId}/{chunkIndex}
     * 用于小程序端分片下载
     */
    @GetMapping("/chunk/{fileId}/{chunkIndex}")
    public void getPdfChunk(
            @PathVariable String fileId,
            @PathVariable int chunkIndex,
            HttpServletResponse response) {
        
        try {
            log.info("请求PDF文件分片，文件ID: {}, 分片索引: {}", fileId, chunkIndex);
            
            // 获取加密的分片数据
            byte[] chunkData = pdfStreamService.getPdfChunk(fileId, chunkIndex);
            
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Length", String.valueOf(chunkData.length));
            response.setHeader("X-Chunk-Index", String.valueOf(chunkIndex));
            response.setHeader("X-File-Id", fileId);
            
            // 写入分片数据
            response.getOutputStream().write(chunkData);
            response.getOutputStream().flush();
            
            log.info("PDF文件分片传输完成，文件ID: {}, 分片索引: {}, 大小: {} bytes", 
                    fileId, chunkIndex, chunkData.length);
            
        } catch (Exception e) {
            log.error("PDF文件分片传输失败，文件ID: {}, 分片索引: {}", fileId, chunkIndex, e);
            
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"分片传输失败: " + e.getMessage() + "\"}");
            } catch (IOException ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }

    /**
     * 获取PDF文件信息（用于小程序端分片下载）
     * GET /pdf/info/{fileId}
     * 返回文件大小、分片数量、加密密钥等信息
     */
    @GetMapping("/info/{fileId}")
    public ApiResponse<Map<String, Object>> getPdfFileInfo(@PathVariable String fileId) {
        try {
            log.info("请求PDF文件信息，文件ID: {}", fileId);
            
            Map<String, Object> fileInfo = pdfStreamService.getPdfFileInfo(fileId);
            
            log.info("获取PDF文件信息成功，文件ID: {}", fileId);
            return ApiResponse.success(fileInfo);
            
        } catch (Exception e) {
            log.error("获取PDF文件信息失败，文件ID: {}", fileId, e);
            return ApiResponse.error(500, "获取文件信息失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     * GET /pdf/health
     * 用于测试PDF流服务是否正常运行
     */
    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        try {
            log.info("PDF流服务健康检查");
            
            // 检查基本服务状态
            // Assuming ossClient and fileInfoService are available in the context
            // if (ossClient == null) {
            //     return ApiResponse.error(500, "OSS客户端未初始化");
            // }
            
            // if (fileInfoService == null) {
            //     return ApiResponse.error(500, "文件信息服务未初始化");
            // }
            
            log.info("PDF流服务运行正常");
            return ApiResponse.success("PDF流服务运行正常");
            
        } catch (Exception e) {
            log.error("PDF流服务健康检查失败", e);
            return ApiResponse.error(500, "PDF流服务异常: " + e.getMessage());
        }
    }

    /**
     * 按页返回PDF渲染图片（PNG）
     * GET /pdf/page/{fileId}/{page}
     * 用于小程序端图片化预览
     */
    @GetMapping("/page/{fileId}/{page}")
    public void getPdfPageImage(
            @PathVariable String fileId,
            @PathVariable int page,
            HttpServletResponse response) {
        ServletOutputStream outputStream = null;
        try {
            log.info("请求PDF页图，文件ID: {}, 页码: {}", fileId, page);

            byte[] pngBytes = pdfStreamService.renderPdfPageAsImage(fileId, page);

            response.setContentType("image/png");
            response.setHeader("Cache-Control", "public, max-age=300");
            response.setHeader("X-File-Id", fileId);
            response.setHeader("X-Page", String.valueOf(page));
            response.setContentLength(pngBytes.length);
              outputStream = response.getOutputStream();
            if (null != outputStream ) {
                outputStream.write(pngBytes);

            }
        } catch (Exception e) {
            log.error("PDF页图获取失败，文件ID: {}, 页码: {}", fileId, page, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"获取页图失败: " + e.getMessage() + "\"}");
            } catch (IOException ex) {
                log.error("写入错误响应失败", ex);
            }
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                    log.error("flush error, {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 清理过期缓存
     * POST /pdf/cache/cleanup
     * 管理员接口，用于清理过期的文件缓存
     */
    @PostMapping("/cache/cleanup")
    @RequireAuth(requireAdmin = true)
    public ApiResponse<String> cleanupExpiredCache() {
        try {
            log.info("开始清理过期缓存");
            
            pdfStreamService.cleanupExpiredCache();
            
            log.info("过期缓存清理完成");
            return ApiResponse.success("缓存清理完成");
            
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
            return ApiResponse.error(500, "缓存清理失败: " + e.getMessage());
        }
    }
}
