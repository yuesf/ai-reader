package com.yuesf.aireader.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


/**
 * 微信用户实体类
 */
@Setter
@Getter
@Table(name = "wechat_user")
public class WechatUser {
    /**
     * 主键ID
     */
    @Id
    private Long id;

    /**
     * 微信openid
     */
    private String openId;

    /**
     * 微信unionid
     */
    private String unionId;

    /**
     * 会话密钥
     */
    private String sessionKey;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 性别（0：未知，1：男，2：女）
     */
    private Integer gender;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 语言
     */
    private String language;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

}