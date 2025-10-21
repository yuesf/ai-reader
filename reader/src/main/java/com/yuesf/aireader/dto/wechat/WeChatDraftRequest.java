package com.yuesf.aireader.dto.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 微信公众号新增草稿请求
 */
@Data
public class WeChatDraftRequest {
    
    /**
     * 图文消息列表
     */
    private List<Article> articles;
    
    @Data
    public static class Article {
        /**
         * 标题
         */
        private String title;
        
        /**
         * 作者
         */
        private String author;
        
        /**
         * 图文消息的摘要，仅有单图文消息才有摘要，多图文此处为空
         */
        private String digest;
        
        /**
         * 图文消息的具体内容，支持HTML标签，必须少于2万字符，小于1M，且此处会去除JS
         */
        private String content;
        
        /**
         * 图文消息的原文地址，即点击"阅读原文"后的URL
         */
        @JsonProperty("content_source_url")
        private String contentSourceUrl;
        
        /**
         * 图文消息的封面图片素材id（必须是永久mediaID）
         */
        @JsonProperty("thumb_media_id")
        private String thumbMediaId;
        
        /**
         * 是否显示封面，0为false，即不显示，1为true，即显示
         */
        @JsonProperty("show_cover_pic")
        private Integer showCoverPic;
        
        /**
         * 是否打开评论，0不打开，1打开
         */
        @JsonProperty("need_open_comment")
        private Integer needOpenComment;
        
        /**
         * 是否粉丝才可评论，0所有人可评论，1粉丝才可评论
         */
        @JsonProperty("only_fans_can_comment")
        private Integer onlyFansCanComment;
    }
}