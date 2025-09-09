package com.yuesf.aireader.service;

import com.yuesf.aireader.config.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * 阿里云文档智能/通义千问DashScope简单摘要服务
 * 注意：这里提供最小可运行HTTP实现，需在 application.properties 配置 apiKey。
 */
@Service
public class AITextSummaryService {

    @Autowired
    private AIConfig aiConfig;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 同步生成摘要
     */
    public String summarize(String plainText) {
        try {
            if (plainText == null || plainText.isBlank()) return null;
            
            // 生成文档整体概览
            String generalPrompt = "作为专业的文档分析师，请深入理解以下文档内容，提炼出核心观点和主要信息，形成一份结构清晰的概述。请确保涵盖文档的主要议题、关键发现和重要结论：\n" + 
                               plainText.substring(0, Math.min(5000, plainText.length()));
            
            // 提取目录结构用于章节摘要（假设目录在文档前部）
            String tableOfContents = extractTableOfContents(plainText);
            
            // 生成章节层次分析
            String chapterPrompt = "请作为资深内容分析专家，基于文档的结构层次，深入解读各个部分的核心内容。请识别文档中的章节划分，并针对每个重要部分提供深度分析和要点提炼：\n" + 
                              "文档结构参考：\n" + tableOfContents + "\n\n" + 
                              "完整文档：\n" + 
                              plainText.substring(0, Math.min(5000, plainText.length()));
            
            // 执行两次AI调用并合并结果
            String generalSummary = callAIForSummary(generalPrompt);
            String chapterSummary = callAIForSummary(chapterPrompt);
            
            return "【文档总览摘要】\n" + generalSummary + "\n\n【章节详细摘要】\n" + chapterSummary;
        } catch (Exception ignored) {}
        return null;
    }
    
    // 新增辅助方法
    private String extractTableOfContents(String text) {
        // 简单实现：提取包含目录/章节信息的部分
        int start = text.indexOf("目录");
        if (start < 0) start = 0;
        return text.substring(start, Math.min(start + 2000, text.length()));
    }
    
    private String callAIForSummary(String prompt) throws Exception {
        String body = "{\n" + 
                     "  \"model\": \"" + aiConfig.getSummarize().getModel() + "\",\n" + 
                     "  \"messages\": [ { \"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\" } ]\n" + 
                     "}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(aiConfig.getEndpoint()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + aiConfig.getApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return extractFirstChoice(resp.body());
        }
        return null;
    }

    /**
     * 异步生成摘要
     */
    @Async
    public CompletableFuture<String> summarizeAsync(String plainText) {
        String summary = summarize(plainText);
        return CompletableFuture.completedFuture(summary);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String extractFirstChoice(String json) {
        // 简化版解析，避免引入额外依赖：寻找 "content":"..."
        int idx = json.indexOf("\"content\":");
        if (idx < 0) return null;
        int start = json.indexOf('"', idx + 10);
        int end = json.indexOf('"', start + 1);
        if (start >= 0 && end > start) {
            String raw = json.substring(start + 1, end);
            return raw.replace("\\n", "\n").replace("\\\"", "\"");
        }
        return null;
    }
}