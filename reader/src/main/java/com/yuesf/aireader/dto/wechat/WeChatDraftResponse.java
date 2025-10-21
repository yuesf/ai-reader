package com.yuesf.aireader.dto.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信公众号新增草稿响应
 */
@Data
public class WeChatDraftResponse {
    
    /**
     * 媒体文件上传后，获取标识，3天内有效
     */
    @JsonProperty("media_id")
    private String mediaId;
    
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