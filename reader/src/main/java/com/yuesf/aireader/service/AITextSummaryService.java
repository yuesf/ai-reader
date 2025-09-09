package com.yuesf.aireader.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.yuesf.aireader.config.AIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
            String chapterSummary = generateChapterSummary(documentContent);
            
            // 合并摘要结果
            StringBuilder finalSummary = new StringBuilder();
            if (generalSummary != null && !generalSummary.isBlank()) {
                finalSummary.append("【文档总览摘要】\n").append(generalSummary);
            }
            
            if (chapterSummary != null && !chapterSummary.isBlank()) {
                if (finalSummary.length() > 0) {
                    finalSummary.append("\n\n");
                }
                finalSummary.append("【章节详细摘要】\n").append(chapterSummary);
            }
            
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
                           "请确保涵盖文档的主要议题、关键发现、重要结论和投资建议。请用中文回答，内容要简洁明了：\n\n" + 
                           truncateContent(documentContent, 8000);
            
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
                           "完整文档：\n" + truncateContent(documentContent, 8000);
            
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
     */
    @Async
    public CompletableFuture<String> summarizeAsync(String documentContent) {
        String summary = summarize(documentContent);
        return CompletableFuture.completedFuture(summary);
    }
}