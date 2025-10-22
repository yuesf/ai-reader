package com.yuesf.aireader.exception;

/**
 * 微信公众号相关异常
 */
public class WeChatPublicException extends RuntimeException {
    
    private final Integer errorCode;
    
    public WeChatPublicException(String message) {
        super(message);
        this.errorCode = null;
    }
    
    public WeChatPublicException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }
    
    public WeChatPublicException(Integer errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public WeChatPublicException(Integer errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public Integer getErrorCode() {
        return errorCode;
    }
    
    /**
     * 根据微信错误码获取友好的错误信息
     */
    public String getFriendlyMessage() {
        if (errorCode == null) {
            return getMessage();
        }
        
        switch (errorCode) {
            case 40001:
                return "微信公众号配置错误，请检查AppSecret";
            case 40002:
                return "微信公众号配置错误，请检查AppId";
            case 40014:
                return "微信公众号Access Token无效";
            case 42001:
                return "微信公众号Access Token已过期";
            case 45009:
                return "微信API调用次数超过限制，请稍后重试";
            case 44004:
                return "文章内容为空，请检查报告摘要";
            case 85023:
                return "文章内容包含违规信息，请修改后重试";
            case 85024:
                return "文章标题包含违规信息，请修改后重试";
            default:
                // 检查是否是IP白名单问题
                if (getMessage() != null && getMessage().contains("invalid ip")) {
                    return "服务器IP地址不在微信公众号白名单中，请联系管理员配置";
                }
                // 检查是否是412错误
                if (getMessage() != null && getMessage().contains("412 Precondition Failed")) {
                    return "请求内容不符合微信API要求，请检查文章标题、内容等字段是否符合规范";
                }
                return String.format("微信公众号API错误 [%d]: %s", errorCode, getMessage());
        }
    }
}