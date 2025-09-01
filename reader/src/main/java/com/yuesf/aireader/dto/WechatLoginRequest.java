package com.yuesf.aireader.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Setter
@Getter
@ToString
public class WechatLoginRequest {
    private String code;
    private UserInfo userInfo;

    @Setter
    @Getter
    @ToString
    public static class UserInfo {
        private String nickName;
        private String avatarUrl;
        private Integer gender;
        private String country;
        private String province;
        private String city;
        private String language;


    }

}