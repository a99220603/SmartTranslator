package com.smarttranslator.translation.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smarttranslator.config.SmartTranslatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Google AI Studio (Gemini API) 翻譯實現
 * 使用 Gemini API 進行翻譯
 */
public class GoogleAIStudioAPI implements TranslationAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAIStudioAPI.class);
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
    
    @Override
    public String translate(String text, String targetLanguage) throws Exception {
        String apiKey = SmartTranslatorConfig.GOOGLE_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Google AI Studio API 金鑰未設定");
        }
        
        // 構建請求URL
        String requestUrl = API_BASE_URL + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        
        // 構建請求體
        JsonObject requestBody = buildRequestBody(text, targetLanguage);
        
        // 發送HTTP請求
        HttpURLConnection connection = null;
        try {
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            
            // 寫入請求體
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody.toString());
                writer.flush();
            }
            
            // 讀取響應
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return parseTranslationResponse(response.toString());
                }
            } else {
                // 讀取錯誤響應
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    LOGGER.error("Google AI Studio API 錯誤 ({}): {}", responseCode, errorResponse.toString());
                    throw new IOException("API 請求失敗: " + responseCode + " - " + errorResponse.toString());
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 構建請求體
     */
    private JsonObject buildRequestBody(String text, String targetLanguage) {
        // 將語言代碼轉換為語言名稱
        String languageName = getLanguageName(targetLanguage);
        
        // 構建提示詞
        String prompt = String.format(
            "請將以下文字翻譯成%s，只返回翻譯結果，不要添加任何解釋或額外內容：\n\n%s",
            languageName, text
        );
        
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);
        
        // 設定生成配置
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.1);
        generationConfig.addProperty("maxOutputTokens", 1000);
        requestBody.add("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    /**
     * 解析翻譯響應
     */
    private String parseTranslationResponse(String jsonResponse) throws Exception {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            if (response.has("candidates")) {
                JsonArray candidates = response.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                JsonObject part = parts.get(0).getAsJsonObject();
                                if (part.has("text")) {
                                    return part.get("text").getAsString().trim();
                                }
                            }
                        }
                    }
                }
            }
            
            // 如果沒有找到翻譯結果，記錄錯誤
            LOGGER.error("無法解析 Google AI Studio 響應: {}", jsonResponse);
            throw new Exception("無法解析翻譯響應");
            
        } catch (Exception e) {
            LOGGER.error("解析 Google AI Studio 響應時發生錯誤: {}", jsonResponse, e);
            throw new Exception("解析翻譯響應失敗: " + e.getMessage());
        }
    }
    
    /**
     * 將語言代碼轉換為語言名稱
     */
    private String getLanguageName(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "zh-tw":
            case "zh-hant":
                return "繁體中文";
            case "zh-cn":
            case "zh-hans":
            case "zh":
                return "簡體中文";
            case "ja":
                return "日文";
            case "ko":
                return "韓文";
            case "fr":
                return "法文";
            case "de":
                return "德文";
            case "es":
                return "西班牙文";
            case "it":
                return "義大利文";
            case "pt":
                return "葡萄牙文";
            case "ru":
                return "俄文";
            case "ar":
                return "阿拉伯文";
            case "th":
                return "泰文";
            case "vi":
                return "越南文";
            default:
                return "英文"; // 預設為英文
        }
    }
    
    @Override
    public String getApiName() {
        return "Google AI Studio";
    }
    
    @Override
    public boolean isAvailable() {
        String apiKey = SmartTranslatorConfig.GOOGLE_API_KEY.get();
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}