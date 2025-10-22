package com.yuesf.aireader.service;

import com.alibaba.dashscope.utils.JsonUtils;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.yuesf.aireader.config.OssConfig;
import com.yuesf.aireader.config.WeChatPublicConfig;
import com.yuesf.aireader.dto.wechat.WeChatAccessTokenResponse;
import com.yuesf.aireader.dto.wechat.WeChatDraftRequest;
import com.yuesf.aireader.dto.wechat.WeChatDraftResponse;
import com.yuesf.aireader.exception.WeChatPublicException;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 微信公众号API服务类
 */
@Slf4j
@Service
public class WeChatPublicService {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String DRAFT_ADD_URL = "https://api.weixin.qq.com/cgi-bin/draft/add?access_token={access_token}";
    // 临时素材上传URL（有效期3天）
    private static final String MEDIA_UPLOAD_URL = "https://api.weixin.qq.com/cgi-bin/media/upload?access_token={access_token}&type={type}";
    // 永久素材上传URL
    private static final String MATERIAL_ADD_URL = "https://api.weixin.qq.com/cgi-bin/material/add_material?access_token={access_token}&type={type}";

    @Autowired
    private WeChatPublicConfig weChatConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OssConfig.OssProperties ossProperties;

    @Autowired
    private OSS ossClient;

    // Access Token缓存
    private static final ConcurrentHashMap<String, AccessTokenCache> tokenCache = new ConcurrentHashMap<>();
    private static final ReentrantLock tokenLock = new ReentrantLock();

    /**
     * Access Token缓存类
     */
    @Getter
    private static class AccessTokenCache {
        private String token;
        private long expireTime;

        public AccessTokenCache(String token, int expiresIn) {
            this.token = token;
            // 提前5分钟过期，避免边界问题
            this.expireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expireTime;
        }

    }

    /**
     * 获取Access Token
     */
    public String getAccessToken() {
        String appid = weChatConfig.getAppid();

        // 检查缓存
        AccessTokenCache cache = tokenCache.get(appid);
        if (cache != null && !cache.isExpired()) {
            return cache.getToken();
        }

        // 加锁获取新token
        tokenLock.lock();
        try {
            // 双重检查
            cache = tokenCache.get(appid);
            if (cache != null && !cache.isExpired()) {
                return cache.getToken();
            }

            log.info("获取微信公众号Access Token，appid: {}", appid);

            ResponseEntity<WeChatAccessTokenResponse> response = restTemplate.getForEntity(
                    ACCESS_TOKEN_URL,
                    WeChatAccessTokenResponse.class,
                    appid,
                    weChatConfig.getSecret()
            );

            WeChatAccessTokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null || !tokenResponse.isSuccess()) {
                if (tokenResponse != null && tokenResponse.getErrcode() != null) {
                    log.error("获取Access Token失败: {} - {}", tokenResponse.getErrcode(), tokenResponse.getErrmsg());
                    throw new WeChatPublicException(tokenResponse.getErrcode(), tokenResponse.getErrmsg());
                } else {
                    log.error("获取Access Token失败: 响应为空");
                    throw new WeChatPublicException("获取Access Token失败: 响应为空");
                }
            }

            // 缓存token
            tokenCache.put(appid, new AccessTokenCache(tokenResponse.getAccessToken(), tokenResponse.getExpiresIn()));

            log.info("获取微信公众号Access Token成功，有效期: {}秒", tokenResponse.getExpiresIn());
            return tokenResponse.getAccessToken();

        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 创建草稿
     */
    public WeChatDraftResponse createDraft(WeChatDraftRequest request) {
        String accessToken = getAccessToken();

        log.info("创建微信公众号草稿，文章数量: {}", request.getArticles().size());

        // 验证和清理请求内容
        WeChatDraftRequest validatedRequest = validateAndCleanRequest(request);

        String body = JsonUtils.toJson(validatedRequest);
        log.info("请求内容: {}", body);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(body.getBytes(StandardCharsets.UTF_8).length);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        // 修复：将access_token作为查询参数而不是路径参数
        String url = DRAFT_ADD_URL.replace("{access_token}", accessToken);

        try {
//            WeChatDraftResponse draftResponse = restTemplate.postForObject(
//                    url,
//                    entity,
//                    WeChatDraftResponse.class
//            );
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);


            if (response.getBody() == null) {
                // 记录请求内容以便调试
                log.error("创建草稿失败: 响应为空，请求内容: {}", validatedRequest);
                throw new WeChatPublicException("创建草稿失败: 响应为空");
            }

            WeChatDraftResponse draftResponse = JsonUtils.fromJson(response.getBody(), WeChatDraftResponse.class);
            if (!draftResponse.isSuccess()) {
                log.error("创建草稿失败: {} - {}，请求内容: {}",
                        draftResponse.getErrcode(), draftResponse.getErrmsg(), validatedRequest);
                throw new WeChatPublicException(draftResponse.getErrcode(), draftResponse.getErrmsg());
            }

            log.info("创建微信公众号草稿成功，media_id: {}", draftResponse.getMediaId());
            return draftResponse;
        } catch (HttpClientErrorException e) {
            // 特别处理HttpClientErrorException以获取更多错误信息
            log.error("创建草稿时发生HTTP客户端错误: 状态码={}, 响应体={}, 请求URL={}, 请求内容={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), url, validatedRequest, e);
            throw new WeChatPublicException("创建草稿失败: " + e.getMessage() + ", 响应内容: " + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * 验证和清理微信草稿请求内容，确保符合微信API要求
     *
     * @param request 原始请求
     * @return 验证和清理后的请求
     */
    private WeChatDraftRequest validateAndCleanRequest(WeChatDraftRequest request) {
        WeChatDraftRequest cleanedRequest = new WeChatDraftRequest();

        if (request.getArticles() != null) {
            List<WeChatDraftRequest.Article> cleanedArticles = request.getArticles().stream()
                    .map(this::validateAndCleanArticle)
                    .collect(Collectors.toList());
            cleanedRequest.setArticles(cleanedArticles);
        }

        return cleanedRequest;
    }

    /**
     * 验证和清理单个文章内容
     *
     * @param article 原始文章
     * @return 验证和清理后的文章
     */
    private WeChatDraftRequest.Article validateAndCleanArticle(WeChatDraftRequest.Article article) {
        WeChatDraftRequest.Article cleanedArticle = new WeChatDraftRequest.Article();

        // 标题不能超过64个字符
        if (article.getTitle() != null) {
            cleanedArticle.setTitle(article.getTitle().length() > 64 ?
                    article.getTitle().substring(0, 64) : article.getTitle());
        }

        // 作者不能超过8个字符
        if (article.getAuthor() != null) {
            cleanedArticle.setAuthor(article.getAuthor().length() > 8 ?
                    article.getAuthor().substring(0, 8) : article.getAuthor());
        }

        // 摘要不能超过120个字符
        if (article.getDigest() != null) {
            cleanedArticle.setDigest(article.getDigest().length() > 120 ?
                    article.getDigest().substring(0, 120) : article.getDigest());
        }

        // 内容不能为空且不能超过20000个字符
        if (article.getContent() != null) {
            String content = article.getContent().trim();
            if (content.isEmpty()) {
                cleanedArticle.setContent("<p>无内容</p>");
            } else {
                cleanedArticle.setContent(content.length() > 20000 ?
                        content.substring(0, 20000) : content);
            }
        } else {
            cleanedArticle.setContent("<p>无内容</p>");
        }

        // 原文链接不能超过256个字符
        if (article.getContentSourceUrl() != null) {
            cleanedArticle.setContentSourceUrl(article.getContentSourceUrl().length() > 256 ?
                    article.getContentSourceUrl().substring(0, 256) : article.getContentSourceUrl());
        }

        // 封面图片media_id保持原样
        cleanedArticle.setThumbMediaId(article.getThumbMediaId());

        // 其他字段保持原样
        cleanedArticle.setShowCoverPic(article.getShowCoverPic());
        cleanedArticle.setNeedOpenComment(article.getNeedOpenComment());
        cleanedArticle.setOnlyFansCanComment(article.getOnlyFansCanComment());

        return cleanedArticle;
    }

    /**
     * 临时素材上传响应
     */
    @Data
    public static class MediaUploadResponse {
        private String type;
        private String mediaId;
        private Long createdAt;
        private String url;  // 永久素材返回的URL
        private Integer errcode;
        private String errmsg;

        public boolean isSuccess() {
            return errcode == null || errcode == 0;
        }
    }

    /**
     * 上传永久素材
     *
     * @param objectKey OSS对象键
     * @param originalName 原始文件名
     * @param type  媒体文件类型，分别有图片（image）、语音（voice）、视频（video）和缩略图（thumb）
     * @return 上传结果
     */
    public MediaUploadResponse uploadMedia(String objectKey, String originalName, String type) throws IOException {

        final byte[] fileData = getFileData(objectKey);

        Path tempFile = null;
        try {
            String accessToken = getAccessToken();
            
            // 验证文件大小（微信公众号图片限制为2MB）
            final int MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB
            if (fileData.length > MAX_IMAGE_SIZE) {
                log.error("文件大小超过限制: {} bytes > {} bytes", fileData.length, MAX_IMAGE_SIZE);
                throw new WeChatPublicException("图片大小超过微信限制（2MB）");
            }
            
            // 确保文件名有正确的扩展名
            final String filename;
            if (!originalName.toLowerCase().endsWith(".jpg") && 
                !originalName.toLowerCase().endsWith(".jpeg") && 
                !originalName.toLowerCase().endsWith(".png") &&
                !originalName.toLowerCase().endsWith(".gif")) {
                // 如果没有扩展名，根据类型添加默认扩展名
                filename = originalName + ".jpg";
            } else {
                filename = originalName;
            }
            
            // 根据文件扩展名确定Content-Type
            final String contentType;
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else {
                contentType = "image/jpeg";
            }
            
            // 创建临时文件 - 使用FileSystemResource替代ByteArrayResource
            tempFile = Files.createTempFile("wechat_upload_", "_" + filename);
            Files.write(tempFile, fileData);
            
            // 构建multipart请求体 - 注意字段名为"media"
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // 根据类型添加不同的参数
            if ("video".equals(type)) {
                // 视频类型需要description参数
                String description = "{\"title\":\"" + filename + "\", \"introduction\":\"视频素材\"}";
                body.add("description", description);
            }
            // 图片/语音/缩略图类型只需要media字段
            body.add("media", new FileSystemResource(tempFile.toFile()));
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 构建URL - 使用永久素材接口
            String url = MATERIAL_ADD_URL.replace("{access_token}", accessToken).replace("{type}", type);
            
            log.info("开始上传永久素材到微信公众号，URL: {}", url);
            log.info("文件信息 - 文件名: {}, Content-Type: {}, 大小: {} bytes, type: {}", filename, contentType, fileData.length, type);
            log.info("临时文件路径: {}", tempFile.toAbsolutePath());

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            // 检查响应
            if (response.getBody() == null || response.getBody().isEmpty()) {
                log.error("上传永久素材失败: 响应为空");
                throw new WeChatPublicException("上传永久素材失败: 响应为空");
            }

            log.info("微信公众号响应: {}", response.getBody());

            // 解析响应
            MediaUploadResponse mediaResponse = JsonUtils.fromJson(response.getBody(), MediaUploadResponse.class);
            if (!mediaResponse.isSuccess()) {
                log.error("上传永久素材失败: {} - {}", mediaResponse.getErrcode(), mediaResponse.getErrmsg());
                throw new WeChatPublicException(mediaResponse.getErrcode(), mediaResponse.getErrmsg());
            }

            log.info("上传永久素材成功，media_id: {}, type: {}, url: {}", 
                    mediaResponse.getMediaId(), mediaResponse.getType(), mediaResponse.getUrl());
            return mediaResponse;

        } catch (HttpClientErrorException e) {
            // 412 错误通常意味着：
            // 1. access_token 无效或过期
            // 2. 公众号类型不支持该接口（订阅号有限制）
            // 3. IP白名单未配置
            // 4. 文件格式不支持
            log.error("上传永久素材时发生HTTP客户端错误: 状态码={}, 响应体={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            log.error("请检查: 1) access_token是否有效 2) 公众号类型是否支持 3) IP白名单是否配置 4) 文件格式是否支持");
            throw new WeChatPublicException("上传永久素材失败: " + e.getMessage() + ", 响应内容: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("上传永久素材失败", e);
            throw new WeChatPublicException("上传永久素材失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("已删除临时文件: {}", tempFile);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempFile, e);
                }
            }
        }
    }

    private byte[] getFileData(String objectKey) throws IOException {
        final byte[] fileData;


        try (OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(), objectKey)) {
            // 从OSS获取对象

            // 将InputStream读取为字节数组，避免流消耗问题
            try (InputStream inputStream = ossObject.getObjectContent()) {
                fileData = inputStream.readAllBytes();
            }

            log.info("从OSS读取文件成功，文件大小: {} bytes", fileData.length);

        } catch (OSSException e) {
            log.error("从OSS获取图片失败: {}", e.getMessage(), e);
            throw new IOException("获取图片失败: " + e.getMessage(), e);
        }
        // 确保OSS对象关闭
        return fileData;
    }
}