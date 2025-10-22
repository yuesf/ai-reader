package com.yuesf.aireader.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.yuesf.aireader.config.OssConfig;
import com.yuesf.aireader.config.WeChatPublicConfig;
import com.yuesf.aireader.dto.wechat.WeChatDraftRequest;
import com.yuesf.aireader.dto.wechat.WeChatDraftResponse;
import com.yuesf.aireader.entity.FileInfo;
import com.yuesf.aireader.entity.Report;
import com.yuesf.aireader.exception.WeChatPublicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * 报告发布服务类
 * 整合报告验证、内容格式化和微信公众号发布功能
 */
@Slf4j
@Service
public class ReportPublishService {

    @Autowired
    private ReportService reportService;

    @Autowired
    private WeChatPublicService weChatPublicService;

    @Autowired
    private WeChatContentFormatter contentFormatter;

    @Autowired
    private WeChatPublicConfig weChatConfig;


    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 发布报告到微信公众号
     *
     * @param reportId 报告ID
     * @return 发布结果，包含草稿媒体ID
     */
    public WeChatDraftResponse publishReportToWeChat(String reportId) {
        try {
            log.info("开始发布报告到微信公众号，报告ID: {}", reportId);

            // 1. 验证报告并获取报告信息
            Report report = reportService.getReportForPublish(reportId);
            log.info("报告验证通过，标题: {}", report.getTitle());

            // 2. 格式化图文消息内容
            String content = contentFormatter.formatReportContent(report);
            log.info("报告内容格式化完成，内容长度: {}", content.length());

            // 3. 生成小程序页面路径
            String miniProgramPath = contentFormatter.generateMiniProgramPath(reportId);

            // 4. 构建微信公众号草稿请求
            WeChatDraftRequest draftRequest = buildDraftRequest(report, content, miniProgramPath);

            // 5. 调用微信公众号API创建草稿
            WeChatDraftResponse response = weChatPublicService.createDraft(draftRequest);

            log.info("报告发布到微信公众号成功，报告ID: {}, 草稿媒体ID: {}",
                    reportId, response.getMediaId());

            return response;

        } catch (IllegalArgumentException e) {
            // 报告验证失败，重新抛出
            throw e;
        } catch (WeChatPublicException e) {
            // 微信公众号API异常，使用友好的错误信息
            log.error("微信公众号API调用失败，报告ID: {}, 错误码: {}", reportId, e.getErrorCode(), e);
            throw new RuntimeException(e.getFriendlyMessage(), e);
        } catch (Exception e) {
            log.error("发布报告到微信公众号失败，报告ID: {}", reportId, e);
            throw new RuntimeException("发布报告到微信公众号失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建微信公众号草稿请求
     */
    private WeChatDraftRequest buildDraftRequest(Report report, String content, String miniProgramPath) {
        WeChatDraftRequest request = new WeChatDraftRequest();

        // 创建图文消息
        WeChatDraftRequest.Article article = new WeChatDraftRequest.Article();

        // 设置标题
        article.setTitle(report.getTitle());

        // 设置作者
        article.setAuthor("小飞哥");

        // 设置摘要（从报告摘要中提取前100字符作为摘要）
        String digest = extractDigest(report.getSummary());
        article.setDigest(digest);

        // 设置内容
        article.setContent(content);

        // 设置图文源地址（小程序报告详情页）
        article.setContentSourceUrl(generateMiniProgramUrl(report.getId()));

        // 设置封面图片（这里需要先上传图片到微信服务器获取media_id）
        article.setThumbMediaId(null); // TODO: 实现图片上传获取media_id

        // 设置显示封面
        article.setShowCoverPic(1);

        // 设置评论配置
        article.setNeedOpenComment(1); // 开启评论
        article.setOnlyFansCanComment(0); // 所有人可评论

        if (null == report.getThumbnail()) {
            throw new RuntimeException("报告封面图片为空");
        }
      String fileId=  report.getThumbnail().substring(report.getThumbnail().lastIndexOf("/")+1);
        FileInfo fileInfo = fileUploadService.getFileInfoById(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("报告封面图片不存在");
        }
        try {
//            InputStream imageBytes = getImageBytes(fileInfo.getFileName());
        // 设置图片信息（用于图片消息类型）

            WeChatPublicService.MediaUploadResponse mediaResponse = weChatPublicService.uploadMedia(fileInfo.getFileName(),fileInfo.getOriginalName(), "image");
            if (mediaResponse != null) {
                WeChatDraftRequest.ImageInfo imageInfo = new WeChatDraftRequest.ImageInfo();
                WeChatDraftRequest.Image image = new WeChatDraftRequest.Image();
                image.setImageMediaId(mediaResponse.getMediaId());
                imageInfo.setImageList(Collections.singletonList(image));
                article.setImageInfo(imageInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException("上传封面图片到微信公众号失败: " + e.getMessage(), e);
        }

        request.setArticles(Collections.singletonList(article));

        log.debug("构建的草稿请求: {}", request);
        return request;
    }

    /**
     * 从摘要中提取简短描述作为图文消息摘要
     */
    private String extractDigest(String summary) {
        if (summary == null || summary.trim().isEmpty()) {
            return "精彩报告内容，点击查看详情";
        }

        // 移除HTML标签和换行符
        String cleanSummary = summary.replaceAll("<[^>]*>", "")
                .replaceAll("\n", " ")
                .trim();

        // 限制长度为100字符
        if (cleanSummary.length() > 100) {
            cleanSummary = cleanSummary.substring(0, 100) + "...";
        }

        return cleanSummary;
    }

    /**
     * 生成小程序URL（用作图文源地址）
     * 注意：这里返回的是一个网页链接，用户点击后会提示在微信中打开小程序
     */
    private String generateMiniProgramUrl(String reportId) {
        // 这里可以返回一个H5页面链接，该页面会引导用户跳转到小程序
        // 或者返回小程序的二维码页面
        return String.format("https://your-domain.com/miniprogram-redirect?reportId=%s", reportId);
    }

    /**
     * 验证报告是否可以发布（不抛出异常，返回验证结果）
     */
    public boolean canPublishReport(String reportId) {
        try {
            reportService.validateReportForPublish(reportId);
            return true;
        } catch (Exception e) {
            log.warn("报告无法发布到公众号，报告ID: {}, 原因: {}", reportId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取报告发布状态信息
     */
    public String getPublishStatusMessage(String reportId) {
        try {
            reportService.validateReportForPublish(reportId);
            return "报告可以发布到公众号";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

//    /**
//     * 根据对象键获取图片数据
//     * 参考 FileUploadService.writeObjectToResponse 方法实现
//     *
//     * @param objectKey OSS对象键
//     * @return 图片字节数组
//     * @throws IOException IO异常
//     */
//    public InputStream getImageBytes(String objectKey) throws IOException {
//        com.aliyun.oss.model.OSSObject ossObject = null;
//        InputStream inputStream = null;
//        try {
//            ossObject = ossClient.getObject(ossProperties.getBucketName(), objectKey);
//            return ossObject.getObjectContent();
//
//
//        } catch (OSSException e) {
//            log.error("从OSS获取图片失败，objectKey: {}", objectKey, e);
//            throw new IOException("获取图片失败: " + e.getMessage(), e);
//        }
//    }

}