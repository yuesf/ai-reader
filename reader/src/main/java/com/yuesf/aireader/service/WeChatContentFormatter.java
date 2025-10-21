package com.yuesf.aireader.service;

import com.yuesf.aireader.config.WeChatPublicConfig;
import com.yuesf.aireader.entity.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 微信公众号内容格式化服务
 */
@Slf4j
@Service
public class WeChatContentFormatter {
    
    @Autowired
    private WeChatPublicConfig weChatConfig;
    
    /**
     * 格式化报告为微信公众号图文消息内容
     * 
     * @param report 报告对象
     * @return 格式化后的HTML内容
     */
    public String formatReportContent(Report report) {
        StringBuilder content = new StringBuilder();
        
        // 1. 添加报告封面图片
        if (report.getThumbnail() != null && !report.getThumbnail().trim().isEmpty()) {
            content.append("<p style=\"text-align: center;\">");
            content.append("<img src=\"").append(getFullImageUrl(report.getThumbnail())).append("\" ");
            content.append("alt=\"").append(report.getTitle()).append("\" ");
            content.append("style=\"max-width: 100%; height: auto;\" />");
            content.append("</p>");
            content.append("<br/>");
        }
        
        // 2. 添加报告摘要
        content.append("<div style=\"margin: 20px 0;\">");
        content.append("<h3 style=\"color: #333; font-size: 18px; margin-bottom: 10px;\">📋 报告摘要</h3>");
        content.append("<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 8px; line-height: 1.6;\">");
        content.append(formatSummaryContent(report.getSummary()));
        content.append("</div>");
        content.append("</div>");
        
        // 3. 添加报告基本信息
        content.append(formatReportInfo(report));
        
        // 4. 添加小程序跳转链接
        content.append(formatMiniProgramLink(report));
        
        // 5. 添加结尾
        content.append(formatFooter());
        
        log.info("报告内容格式化完成，报告ID: {}, 内容长度: {}", report.getId(), content.length());
        return content.toString();
    }
    
    /**
     * 格式化摘要内容
     */
    private String formatSummaryContent(String summary) {
        if (summary == null || summary.trim().isEmpty()) {
            return "暂无摘要";
        }
        
        // 将换行符转换为HTML换行
        String formattedSummary = summary.replace("\n", "<br/>");
        
        // 如果摘要过长，进行截取
        if (formattedSummary.length() > 1000) {
            formattedSummary = formattedSummary.substring(0, 1000) + "...";
        }
        
        return formattedSummary;
    }
    
    /**
     * 格式化报告基本信息
     */
    private String formatReportInfo(Report report) {
        StringBuilder info = new StringBuilder();
        
        info.append("<div style=\"margin: 20px 0; padding: 15px; background-color: #fff; border: 1px solid #e9ecef; border-radius: 8px;\">");
        info.append("<h3 style=\"color: #333; font-size: 16px; margin-bottom: 10px;\">📊 报告信息</h3>");
        
        info.append("<table style=\"width: 100%; border-collapse: collapse;\">");
        
        if (report.getSource() != null && !report.getSource().trim().isEmpty()) {
            info.append("<tr><td style=\"padding: 5px 0; color: #666;\">📈 来源：</td><td style=\"padding: 5px 0;\">")
                .append(report.getSource()).append("</td></tr>");
        }
        
        if (report.getCategory() != null && !report.getCategory().trim().isEmpty()) {
            info.append("<tr><td style=\"padding: 5px 0; color: #666;\">🏷️ 分类：</td><td style=\"padding: 5px 0;\">")
                .append(report.getCategory()).append("</td></tr>");
        }
        
        if (report.getPages() != null && report.getPages() > 0) {
            info.append("<tr><td style=\"padding: 5px 0; color: #666;\">📄 页数：</td><td style=\"padding: 5px 0;\">")
                .append(report.getPages()).append(" 页</td></tr>");
        }
        
        if (report.getPublishDate() != null) {
            info.append("<tr><td style=\"padding: 5px 0; color: #666;\">📅 发布日期：</td><td style=\"padding: 5px 0;\">")
                .append(report.getPublishDate().toString()).append("</td></tr>");
        }
        
        info.append("</table>");
        info.append("</div>");
        
        return info.toString();
    }
    
    /**
     * 格式化小程序跳转链接
     */
    private String formatMiniProgramLink(Report report) {
        StringBuilder link = new StringBuilder();
        
        link.append("<div style=\"margin: 30px 0; text-align: center; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px;\">");
        link.append("<h3 style=\"color: white; font-size: 18px; margin-bottom: 15px;\">🔍 查看完整报告</h3>");
        link.append("<p style=\"color: #f0f0f0; font-size: 14px; margin-bottom: 15px;\">点击下方小程序卡片，查看报告详情和下载完整版本</p>");
        
        // 小程序跳转提示
        String miniProgramPath = String.format("%s?id=%s", 
            weChatConfig.getMiniprogram().getReportDetailPath(), 
            report.getId());
        
        link.append("<div style=\"background-color: rgba(255,255,255,0.2); padding: 10px; border-radius: 8px; margin: 10px 0;\">");
        link.append("<p style=\"color: white; font-size: 12px; margin: 0;\">小程序路径：").append(miniProgramPath).append("</p>");
        link.append("</div>");
        
        link.append("<p style=\"color: #f0f0f0; font-size: 12px; margin: 0;\">💡 提示：在微信中点击小程序卡片即可直接跳转</p>");
        link.append("</div>");
        
        return link.toString();
    }
    
    /**
     * 格式化页脚
     */
    private String formatFooter() {
        StringBuilder footer = new StringBuilder();
        
        footer.append("<div style=\"margin-top: 40px; padding-top: 20px; border-top: 1px solid #e9ecef; text-align: center;\">");
        footer.append("<p style=\"color: #999; font-size: 12px; margin: 5px 0;\">📚 更多精彩报告，尽在AI智能阅读助手</p>");
        footer.append("<p style=\"color: #999; font-size: 12px; margin: 5px 0;\">🤖 AI驱动，智能解读，让阅读更高效</p>");
        footer.append("</div>");
        
        return footer.toString();
    }
    
    /**
     * 获取完整的图片URL
     */
    private String getFullImageUrl(String thumbnailPath) {
        if (thumbnailPath == null || thumbnailPath.trim().isEmpty()) {
            return "";
        }
        
        // 如果已经是完整URL，直接返回
        if (thumbnailPath.startsWith("http://") || thumbnailPath.startsWith("https://")) {
            return thumbnailPath;
        }
        
        // 如果是相对路径，需要拼接完整URL
        // 这里需要根据实际的图片服务配置来构建完整URL
        // 假设图片通过 /v1/images/{fileId} 接口访问
        if (thumbnailPath.startsWith("/v1/images/")) {
            // 这里需要配置实际的域名
            return "https://your-domain.com" + thumbnailPath;
        }
        
        return thumbnailPath;
    }
    
    /**
     * 生成小程序页面路径
     */
    public String generateMiniProgramPath(String reportId) {
        return String.format("%s?id=%s", 
            weChatConfig.getMiniprogram().getReportDetailPath(), 
            reportId);
    }
}