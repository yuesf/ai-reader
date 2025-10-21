package com.yuesf.aireader.dto.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信公众号Access Token响应
 */
@Data
public class WeChatAccessTokenResponse {
    
    /**
     * 访问令牌
     */
    @JsonProperty("access_token")
    private String accessToken;
    
    /**
     * 过期时间（秒）
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;
    
    /**
     * 错误码
     */
    @JsonProperty("errcode")
    private Integer errcode;
    
    /**
     * 错误信息
     */
    @JsonProperty("errmsg")
    private String errmsg;
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return errcode == null || errcode == 0;
    }
}