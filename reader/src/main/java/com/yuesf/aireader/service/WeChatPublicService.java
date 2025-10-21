package com.yuesf.aireader.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuesf.aireader.config.WeChatPublicConfig;
import com.yuesf.aireader.dto.wechat.WeChatAccessTokenResponse;
import com.yuesf.aireader.dto.wechat.WeChatDraftRequest;
import com.yuesf.aireader.dto.wechat.WeChatDraftResponse;
import com.yuesf.aireader.exception.WeChatPublicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 微信公众号API服务类
 */
@Slf4j
@Service
public class WeChatPublicService {
    
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String DRAFT_ADD_URL = "https://api.weixin.qq.com/cgi-bin/draft/add?access_token={access_token}";
    
    @Autowired
    private WeChatPublicConfig weChatConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Access Token缓存
    private static final ConcurrentHashMap<String, AccessTokenCache> tokenCache = new ConcurrentHashMap<>();
    private static final ReentrantLock tokenLock = new ReentrantLock();
    
    /**
     * Access Token缓存类
     */
    private static class AccessTokenCache {
        private final String token;
        private final long expireTime;
        
        public AccessTokenCache(String token, int expiresIn) {
            this.token = token;
            // 提前5分钟过期，避免边界问题
            this.expireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() >= expireTime;
        }
        
        public String getToken() {
            return token;
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
        try {
            String accessToken = getAccessToken();
            
            log.info("创建微信公众号草稿，文章数量: {}", request.getArticles().size());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<WeChatDraftRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<WeChatDraftResponse> response = restTemplate.postForEntity(
                DRAFT_ADD_URL,
                entity,
                WeChatDraftResponse.class,
                accessToken
            );
            
            WeChatDraftResponse draftResponse = response.getBody();
            if (draftResponse == null) {
                throw new WeChatPublicException("创建草稿失败: 响应为空");
            }
            
            if (!draftResponse.isSuccess()) {
                log.error("创建草稿失败: {} - {}", draftResponse.getErrcode(), draftResponse.getErrmsg());
                throw new WeChatPublicException(draftResponse.getErrcode(), draftResponse.getErrmsg());
            }
            
            log.info("创建微信公众号草稿成功，media_id: {}", draftResponse.getMediaId());
            return draftResponse;
            
        } catch (WeChatPublicException e) {
            // 重新抛出微信公众号异常
            throw e;
        } catch (Exception e) {
            log.error("创建微信公众号草稿失败", e);
            throw new WeChatPublicException("创建微信公众号草稿失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清除Access Token缓存
     */
    public void clearTokenCache() {
        tokenCache.clear();
        log.info("已清除微信公众号Access Token缓存");
    }
}