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
            
            // 调用API并检查完整性
            return callDashScopeAPIWithContinuation(prompt, "general");
            
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
     * 调用阿里云百炼平台通义API（带续写功能）
     * 检测生成结果是否完整，如果未完整则继续生成
     * 
     * @param prompt 初始提示词
     * @param summaryType 摘要类型（用于日志）
     * @return 完整的摘要内容
     */
    private String callDashScopeAPIWithContinuation(String prompt, String summaryType) {
        try {
            StringBuilder fullSummary = new StringBuilder();
            String currentPrompt = prompt;
            int maxContinuations = 3; // 最多续写3次
            int continuationCount = 0;
            
            while (continuationCount <= maxContinuations) {
                // 调用API生成内容
                String partialResult = callDashScopeAPI(currentPrompt);
                
                if (partialResult == null || partialResult.isBlank()) {
                    log.warn("第 {} 次生成结果为空，{} 摘要", continuationCount + 1, summaryType);
                    break;
                }
                
                fullSummary.append(partialResult);
                
                // 检测是否完整
                if (isContentComplete(partialResult)) {
                    log.info("{} 摘要生成完整，总长度: {} 字符", summaryType, fullSummary.length());
                    break;
                }
                
                // 如果未完整，准备续写
                continuationCount++;
                if (continuationCount > maxContinuations) {
                    log.warn("{} 摘要达到最大续写次数，停止续写，当前长度: {}", 
                            summaryType, fullSummary.length());
                    break;
                }
                
                log.info("{} 摘要未完整，进行第 {} 次续写，当前长度: {}", 
                        summaryType, continuationCount, fullSummary.length());
                
                // 构造续写提示词，保持上下文连贯性
                currentPrompt = buildContinuationPrompt(fullSummary.toString());
            }
            
            return fullSummary.toString();
            
        } catch (Exception e) {
            log.error("调用通义API失败 ({}): {}", summaryType, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 检测内容是否完整
     * 通过多种启发式规则判断
     */
    private boolean isContentComplete(String content) {
        if (content == null || content.isBlank()) {
            return true; // 空内容视为完整
        }
        
        String trimmed = content.trim();
        
        // 1. 检测是否以完整的结束符号结尾
        String[] endMarkers = {
            "。", ".", "\uff01", "!", "\uff1f", "?", 
            "】", "]", ")", "）",
            "结论", "总结", "综上所述", "综上"
        };
        
        boolean endsWithMarker = false;
        for (String marker : endMarkers) {
            if (trimmed.endsWith(marker)) {
                endsWithMarker = true;
                break;
            }
        }
        
        // 2. 检测是否以未完成的标志结尾（说明被截断）
        String[] incompleteMarkers = {
            "...", "…", ",", "，", ";", "；", ":", "：",
            "、", "-", "—", "——"
        };
        
        boolean endsWithIncomplete = false;
        for (String marker : incompleteMarkers) {
            if (trimmed.endsWith(marker)) {
                endsWithIncomplete = true;
                break;
            }
        }
        
        // 3. 检测最后一句话是否完整
        String lastSentence = getLastSentence(trimmed);
        boolean lastSentenceComplete = lastSentence != null && 
                                        lastSentence.length() > 10 && 
                                        !lastSentence.matches(".*[,，;；:：、]$");
        
        // 4. 检测内容长度（如果接近最大token限制，可能被截断）
        // 假设每个字符约2个token，最大token为aiConfig配置值
        int estimatedTokens = content.length() * 2;
        int maxTokens = aiConfig.getSummarize().getMaxTokens();
        boolean nearMaxTokens = estimatedTokens >= (maxTokens * 0.95); // 超过95%认为可能被截断
        
        // 综合判断：
        // - 如果以完整符号结尾且最后一句完整，且未超token限制 -> 完整
        // - 如果以未完成标志结尾 -> 未完整
        // - 如果接近token限制 -> 可能未完整
        
        if (endsWithIncomplete) {
            log.debug("检测到内容以未完成标志结尾，判定为未完整");
            return false;
        }
        
        if (nearMaxTokens) {
            log.debug("检测到内容接近token限制 ({}/{})，判定为可能未完整", estimatedTokens, maxTokens);
            return false;
        }
        
        if (endsWithMarker && lastSentenceComplete) {
            log.debug("检测到内容以完整符号结尾且最后一句完整，判定为完整");
            return true;
        }
        
        // 默认情况：如果以句号结尾则认为完整
        return endsWithMarker;
    }
    
    /**
     * 获取最后一句话
     */
    private String getLastSentence(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        
        String[] sentenceEnders = {"。", ".", "\uff01", "!", "\uff1f", "?"};
        int lastIndex = -1;
        
        for (String ender : sentenceEnders) {
            int index = content.lastIndexOf(ender);
            if (index > lastIndex) {
                lastIndex = index;
            }
        }
        
        if (lastIndex >= 0 && lastIndex < content.length() - 1) {
            return content.substring(lastIndex + 1).trim();
        }
        
        return content; // 没有句号，返回整个内容
    }
    
    /**
     * 构造续写提示词，保持上下文连贯性
     */
    private String buildContinuationPrompt(String previousContent) {
        // 提取最后200个字符作为上下文
        String context = previousContent.length() > 200 
                ? previousContent.substring(previousContent.length() - 200) 
                : previousContent;
        
        return "以下是之前生成的摘要内容的末尾部分：\n\n" + 
               context + 
               "\n\n请继续完成剩余的摘要内容，保持上下文连贯性和内容的完整性。请直接继续书写，不要重复之前的内容。";
    }
    
    /**
     * 调用阿里云百炼平台通义API（基础方法）
     */
    private String callDashScopeAPI(String prompt) {
        try {
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
            
        } catch (Exception e) {
            log.error("调用通义API异常: {}", e.getMessage(), e);
            throw new RuntimeException("调用通义API失败", e);
        }
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