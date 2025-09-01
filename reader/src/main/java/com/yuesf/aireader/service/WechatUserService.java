package com.yuesf.aireader.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuesf.aireader.dto.WechatLoginRequest;
import com.yuesf.aireader.dto.WechatLoginResponse;
import com.yuesf.aireader.entity.WechatUser;
import com.yuesf.aireader.mapper.WechatUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WechatUserService {

    @Autowired
    private WechatUserMapper wechatUserMapper;

    @Value("${wechat.mini.appid}")
    private String appId;

    @Value("${wechat.mini.secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public WechatLoginResponse login(WechatLoginRequest request) {

        WechatLoginResponse response = new WechatLoginResponse();
        
        try {
            // 1. 调用微信接口获取 openid 和 session_key
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, request.getCode()
            );
            
            String responseStr = restTemplate.getForObject(url, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> wxResponse = objectMapper.readValue(responseStr, Map.class);
            
            if (wxResponse != null && wxResponse.containsKey("openid")) {
                String openId = (String) wxResponse.get("openid");
                String sessionKey = (String) wxResponse.get("session_key");
                
                // 2. 查找或创建用户
                WechatUser user = wechatUserMapper.findByOpenId(openId);
                boolean isNewUser = false;
                
                if (user == null) {
                    // 创建新用户
                    user = new WechatUser();
                    user.setOpenId(openId);
                    user.setSessionKey(sessionKey);
                    user.setLastLoginTime(new Date());
                    isNewUser = true;
                } else {
                    // 更新现有用户
                    user.setSessionKey(sessionKey);
                    user.setLastLoginTime(new Date());
                }
                
                // 3. 更新用户信息（如果提供了）
                if (request.getUserInfo() != null) {
                    WechatLoginRequest.UserInfo userInfo = request.getUserInfo();
                    user.setNickName(userInfo.getNickName());
                    user.setAvatarUrl(userInfo.getAvatarUrl());
                    user.setGender(userInfo.getGender());
                    user.setCountry(userInfo.getCountry());
                    user.setProvince(userInfo.getProvince());
                    user.setCity(userInfo.getCity());
                    user.setLanguage(userInfo.getLanguage());
                }
                
                // 4. 保存用户信息
                if (isNewUser) {
                    wechatUserMapper.insert(user);
                } else {
                    wechatUserMapper.update(user);
                }
                
                // 5. 构建响应
                response.setSuccess(true);
                response.setMessage("登录成功");
                response.setOpenId(openId);
                response.setSessionKey(sessionKey);
                response.setToken(generateToken(openId)); // 生成自定义token
                
                // 设置用户信息
                WechatLoginResponse.UserInfo responseUserInfo = new WechatLoginResponse.UserInfo();
                responseUserInfo.setNickName(user.getNickName());
                responseUserInfo.setAvatarUrl(user.getAvatarUrl());
                responseUserInfo.setGender(user.getGender());
                responseUserInfo.setCountry(user.getCountry());
                responseUserInfo.setProvince(user.getProvince());
                responseUserInfo.setCity(user.getCity());
                responseUserInfo.setLanguage(user.getLanguage());
                response.setUserInfo(responseUserInfo);
                
            } else {
                response.setSuccess(false);
                response.setMessage("微信登录失败: " + wxResponse.get("errmsg"));
            }
            
        } catch (Exception e) {
            log.error("登录失败",e);

            response.setSuccess(false);
            response.setMessage("登录失败: " + e.getMessage());
        }
        
        return response;
    }

    public WechatLoginResponse getUserInfo(String openId) {
        WechatLoginResponse response = new WechatLoginResponse();
        try {
            WechatUser user = wechatUserMapper.findByOpenId(openId);
            if (user != null) {
                response.setSuccess(true);
                response.setOpenId(user.getOpenId());
                
                WechatLoginResponse.UserInfo userInfo = new WechatLoginResponse.UserInfo();
                userInfo.setNickName(user.getNickName());
                userInfo.setAvatarUrl(user.getAvatarUrl());
                userInfo.setGender(user.getGender());
                userInfo.setCountry(user.getCountry());
                userInfo.setProvince(user.getProvince());
                userInfo.setCity(user.getCity());
                userInfo.setLanguage(user.getLanguage());
                
                response.setUserInfo(userInfo);
                response.setMessage("获取用户信息成功");
            } else {
                response.setSuccess(false);
                response.setMessage("用户不存在");
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("获取用户信息失败: " + e.getMessage());
        }
        return response;
    }

    private String generateToken(String openId) {
        // 简单的token生成，实际项目中应该使用JWT等更安全的方式
        return UUID.randomUUID().toString().replace("-", "") + "_" + openId.substring(0, 8);
    }
}