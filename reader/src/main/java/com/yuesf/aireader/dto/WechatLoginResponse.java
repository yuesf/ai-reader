package com.yuesf.aireader.dto;

public class WechatLoginResponse {
    private boolean success;
    private String message;
    private String openId;
    private String sessionKey;
    private String token;
    private UserInfo userInfo;

    public static class UserInfo {
        private String nickName;
        private String avatarUrl;
        private Integer gender;
        private String country;
        private String province;
        private String city;
        private String language;

        // Getters and Setters
        public String getNickName() { return nickName; }
        public void setNickName(String nickName) { this.nickName = nickName; }
        
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        
        public Integer getGender() { return gender; }
        public void setGender(Integer gender) { this.gender = gender; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }
    
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public UserInfo getUserInfo() { return userInfo; }
    public void setUserInfo(UserInfo userInfo) { this.userInfo = userInfo; }
}