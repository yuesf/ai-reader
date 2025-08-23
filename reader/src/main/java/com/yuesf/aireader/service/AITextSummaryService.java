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
            String prompt = "请用中文为以下报告生成150~250字摘要，客观、中性、包含关键信息点：\n" + plainText.substring(0, Math.min(3000, plainText.length()));
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
                // 兼容OpenAI格式提取
                String text = extractFirstChoice(resp.body());
                return text;
            }
        } catch (Exception ignored) {}
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


