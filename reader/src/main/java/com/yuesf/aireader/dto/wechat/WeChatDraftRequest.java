package com.yuesf.aireader.dto.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 微信公众号新增草稿请求
 */
@Data
public class WeChatDraftRequest {
    
    /**
     * 图文消息列表
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Article> articles;
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
        
        /**
         * 图片消息里的图片相关信息，图片数量最多为20张，首张图片即为封面图
         */
        @JsonProperty("image_info")
        private ImageInfo imageInfo;
        
        /**
         * 图文消息封面裁剪为2.35:1规格的坐标字段
         */
        @JsonProperty("pic_crop_235_1")
        private String picCrop2351;
        
        /**
         * 图文消息封面裁剪为1:1规格的坐标字段
         */
        @JsonProperty("pic_crop_1_1")
        private String picCrop11;
        
        /**
         * 图片消息的封面信息
         */
        @JsonProperty("cover_info")
        private CoverInfo coverInfo;
        
        /**
         * 商品信息
         */
        @JsonProperty("product_info")
        private ProductInfo productInfo;
    }
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageInfo {
        /**
         * 图片列表
         */
        @JsonProperty("image_list")
        private List<Image> imageList;
    }
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Image {
        /**
         * 图片消息里的图片素材id（必须是永久MediaID）
         */
        @JsonProperty("image_media_id")
        private String imageMediaId;
    }
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CoverInfo {
        /**
         * 封面图片素材id
         */
        @JsonProperty("cover_media_id")
        private String coverMediaId;
    }
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductInfo {
        /**
         * 文末插入商品相关信息
         */
        @JsonProperty("footer_product_info")
        private FooterProductInfo footerProductInfo;
    }
    
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FooterProductInfo {
        /**
         * 商品key
         */
        @JsonProperty("product_key")
        private String productKey;
    }
}