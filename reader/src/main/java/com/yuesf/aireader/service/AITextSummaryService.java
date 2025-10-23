package com.yuesf.aireader.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.yuesf.aireader.config.AIConfig;
import com.yuesf.aireader.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * 阿里云百炼平台通义API摘要服务
 * 使用DashScope SDK进行文档摘要生成
 */
@Slf4j
@Service
public class AITextSummaryService {

    @Autowired
    private AIConfig aiConfig;
    
    @Autowired
    private OssConfig.OssProperties ossProperties;
    
    @Autowired
    private OSS ossClient;
    
    @Autowired
    private AliyunDocumentIntelligenceService documentIntelligenceService;

    /**
     * 同步生成摘要
     */
    public String summarize(String documentContent) {
        try {
            if (documentContent == null || documentContent.isBlank()) {
                log.warn("文档内容为空，无法生成摘要");
                return null;
            }
            
            // 设置API Key
            System.setProperty("DASHSCOPE_API_KEY", aiConfig.getApiKey());
            
            // 生成文档整体概览摘要
            String generalSummary = generateGeneralSummary(documentContent);
            
            // 生成章节详细摘要
            // String chapterSummary = generateChapterSummary(documentContent);

            // 合并摘要结果
            StringBuilder finalSummary = new StringBuilder();
            if (generalSummary != null && !generalSummary.isBlank()) {
                finalSummary.append("【文档总览摘要】\n").append(generalSummary);
            }
            
            // if (chapterSummary != null && !chapterSummary.isBlank()) {
            //     if (finalSummary.length() > 0) {
            //         finalSummary.append("\n\n");
            //     }
            //     finalSummary.append("【章节详细摘要】\n").append(chapterSummary);
            // }
            
            String result = finalSummary.toString();
            log.info("摘要生成成功，长度: {}", result.length());
            return result;
            
        } catch (Exception e) {
            log.error("生成摘要失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 生成文档整体概览摘要
     */
    private String generateGeneralSummary(String documentContent) {
        try {
            String prompt = "作为专业的研究报告分析师，请深入理解以下文档内容，提炼出核心观点和主要信息，形成一份结构清晰的概述。" +
                           "请确保涵盖文档的主要议题、关键发现和重要结论。请用中文回答，内容要简洁明了：\n\n" + documentContent;
            
            return callDashScopeAPI(prompt);
            
        } catch (Exception e) {
            log.error("生成整体摘要失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 生成章节详细摘要
     */
    private String generateChapterSummary(String documentContent) {
        try {
            // 提取目录结构
            String tableOfContents = extractTableOfContents(documentContent);
            
            String prompt = "请作为资深内容分析专家，基于文档的结构层次，深入解读各个部分的核心内容。" +
                           "请识别文档中的章节划分，并针对每个重要部分提供深度分析和要点提炼。" +
                           "请用中文回答，按章节结构组织内容：\n\n" +
                           "文档结构参考：\n" + tableOfContents + "\n\n" +
//                           "完整文档：\n" + truncateContent(documentContent, 8000);
                           "完整文档：\n" + documentContent;

            return callDashScopeAPI(prompt);
            
        } catch (Exception e) {
            log.error("生成章节摘要失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 调用阿里云百炼平台通义API
     */
    private String callDashScopeAPI(String prompt) throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();

        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build();
        
        GenerationParam param = GenerationParam.builder()
                .apiKey(aiConfig.getApiKey())
                .model(aiConfig.getSummarize().getModel())
                .messages(Arrays.asList(userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .topP(aiConfig.getSummarize().getTopP())
                .maxTokens(aiConfig.getSummarize().getMaxTokens())
                .temperature(aiConfig.getSummarize().getTemperature())
                .build();
        
        GenerationResult result = gen.call(param);
        
        if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null 
            && !result.getOutput().getChoices().isEmpty()) {
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        }
        
        log.warn("通义API返回结果为空");
        return null;
    }
    
    /**
     * 提取目录结构
     */
    private String extractTableOfContents(String text) {
        // 查找目录相关关键词
        String[] tocKeywords = {"目录", "目　录", "CONTENTS", "Contents", "章节", "第一章", "第二章", "1.", "2.", "3."};
        
        int tocStart = -1;
        for (String keyword : tocKeywords) {
            int index = text.indexOf(keyword);
            if (index >= 0 && (tocStart == -1 || index < tocStart)) {
                tocStart = index;
            }
        }
        
        if (tocStart >= 0) {
            // 提取目录部分，限制长度
            int tocEnd = Math.min(tocStart + 3000, text.length());
            return text.substring(tocStart, tocEnd);
        }
        
        // 如果没找到目录，返回文档开头部分
        return text.substring(0, Math.min(2000, text.length()));
    }
    
    /**
     * 截断内容到指定长度
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }

    /**
     * 异步生成摘要
     * @param objectKey OSS文件对象键（文件路径）
     * @return 摘要内容的Future对象
     */
    @Async
    public CompletableFuture<String> summarizeAsync(String objectKey) {
        try {
            log.info("开始异步生成摘要，OSS对象键: {}", objectKey);
            
            // 步骤1: 从OSS读取文件并生成临时访问URL
            String ossFileUrl = generateOssFileUrl(objectKey);
            if (ossFileUrl == null) {
                log.error("无法生成OSS文件URL，对象键: {}", objectKey);
                return CompletableFuture.completedFuture(null);
            }
            
            log.info("OSS文件URL生成成功: {}", ossFileUrl);
            
            // 步骤2: 使用阿里云智能文档服务提取文档内容
            String documentContent = extractDocumentContent(ossFileUrl, objectKey);
            if (documentContent == null || documentContent.isBlank()) {
                log.error("文档内容提取失败或为空，对象键: {}", objectKey);
                return CompletableFuture.completedFuture(null);
            }
            
            log.info("文档内容提取成功，内容长度: {} 字符，对象键: {}", documentContent.length(), objectKey);
            
            // 步骤3: 根据文档内容生成摘要
            String summary = summarize(documentContent);
            
            if (summary != null && !summary.isBlank()) {
                log.info("异步摘要生成成功，摘要长度: {} 字符，对象键: {}", summary.length(), objectKey);
            } else {
                log.warn("摘要生成结果为空，对象键: {}", objectKey);
            }
            
            return CompletableFuture.completedFuture(summary);
            
        } catch (Exception e) {
            log.error("异步生成摘要失败，对象键: {}", objectKey, e);
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * 生成OSS文件的临时访问URL
     * @param objectKey OSS对象键
     * @return 临时访问URL，失败返回null
     */
    private String generateOssFileUrl(String objectKey) {
        try {
            // 生成1小时有效期的临时访问URL
            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 3600 * 1000);
            java.net.URL url = ossClient.generatePresignedUrl(
                ossProperties.getBucketName(), 
                objectKey, 
                expiration
            );
            return url.toString();
        } catch (OSSException e) {
            log.error("生成OSS临时URL失败，对象键: {}, 错误: {}", objectKey, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("生成OSS临时URL时发生异常，对象键: {}", objectKey, e);
            return null;
        }
    }
    
    /**
     * 提取文档内容
     * @param ossFileUrl OSS文件URL
     * @param objectKey OSS对象键（用于日志）
     * @return 文档文本内容
     */
    private String extractDocumentContent(String ossFileUrl, String objectKey) {
        try {
            log.info("开始提取文档内容，URL: {}", ossFileUrl);
            
            // 使用阿里云文档智能服务解析PDF（内部使用PDFBox）
            // 最大等待时间300秒（5分钟）
            String content = documentIntelligenceService.parseDocumentSync(ossFileUrl, 300);
            
            if (content != null && !content.isBlank()) {
                log.info("文档内容提取成功，内容长度: {} 字符", content.length());
                return content;
            } else {
                log.warn("文档内容提取结果为空，对象键: {}", objectKey);
                return null;
            }
            
        } catch (Exception e) {
            log.error("文档内容提取失败，对象键: {}", objectKey, e);
            return null;
        }
    }
}