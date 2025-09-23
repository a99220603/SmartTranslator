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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google AI Studio (Gemini API) 翻譯實現
 * 使用 Gemini API 進行翻譯
 */
public class GoogleAIStudioAPI implements TranslationAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAIStudioAPI.class);
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    // 占位符保護模式
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[FORMAT\\]", Pattern.CASE_INSENSITIVE);
    private static final String PROTECTED_PREFIX = "ZZPROTECTEDPLACEHOLDERZZZ";
    
    @Override
    public String translate(String text, String targetLanguage) throws Exception {
        String apiKey = SmartTranslatorConfig.GOOGLE_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Google AI Studio API 金鑰未設定");
        }
        
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return performTranslation(text, targetLanguage, apiKey);
            } catch (Exception e) {
                lastException = e;
                LOGGER.warn("翻譯嘗試 {}/{} 失敗: {}", attempt, MAX_RETRIES, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // 指數退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Exception("翻譯被中斷", ie);
                    }
                }
            }
        }
        
        throw new Exception("翻譯失敗，已重試 " + MAX_RETRIES + " 次", lastException);
    }
    
    private String performTranslation(String text, String targetLanguage, String apiKey) throws Exception {
        
        // 保護占位符
        Map<String, String> placeholderMap = new HashMap<>();
        String protectedText = protectPlaceholders(text, placeholderMap);
        
        // 構建請求URL
        String requestUrl = API_BASE_URL + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        
        // 構建請求體
        JsonObject requestBody = buildRequestBody(protectedText, targetLanguage);
        
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
                    String translatedText = parseTranslationResponse(response.toString());
                    
                    // 恢復占位符
                    return restorePlaceholders(translatedText, placeholderMap);
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
     * 保護占位符，將其替換為不會被翻譯的標記
     */
    private String protectPlaceholders(String text, Map<String, String> placeholderMap) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        int counter = 0;
        
        while (matcher.find()) {
            String placeholder = matcher.group();
            String protectedToken = PROTECTED_PREFIX + counter + "ZZEND";
            placeholderMap.put(protectedToken, placeholder);
            matcher.appendReplacement(sb, protectedToken);
            counter++;
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 恢復占位符
     */
    private String restorePlaceholders(String text, Map<String, String> placeholderMap) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    /**
     * 構建請求體
     */
    private JsonObject buildRequestBody(String text, String targetLanguage) {
        // 將語言代碼轉換為語言名稱
        String languageName = getLanguageName(targetLanguage);
        
        // 構建提示詞，特別強調不要翻譯占位符
        String prompt = String.format(
            "請將以下文字翻譯成%s。注意：請保持所有以ZZPROTECTEDPLACEHOLDERZZZ開頭和ZZEND結尾的標記不變，不要翻譯它們。只返回翻譯結果，不要添加任何解釋或額外內容：\n\n%s",
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
        generationConfig.addProperty("topP", 0.8);
        generationConfig.addProperty("topK", 10);
        requestBody.add("generationConfig", generationConfig);
        
        // 設定安全設置
        JsonArray safetySettings = new JsonArray();
        String[] categories = {
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH", 
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT"
        };
        
        for (String category : categories) {
            JsonObject safetySetting = new JsonObject();
            safetySetting.addProperty("category", category);
            safetySetting.addProperty("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            safetySettings.add(safetySetting);
        }
        requestBody.add("safetySettings", safetySettings);
        
        return requestBody;
    }
    
    /**
     * 解析翻譯響應
     */
    private String parseTranslationResponse(String jsonResponse) throws Exception {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            // 檢查是否有錯誤
            if (response.has("error")) {
                JsonObject error = response.getAsJsonObject("error");
                String errorMessage = error.has("message") ? error.get("message").getAsString() : "未知錯誤";
                int errorCode = error.has("code") ? error.get("code").getAsInt() : -1;
                throw new Exception("API 錯誤 (" + errorCode + "): " + errorMessage);
            }
            
            if (response.has("candidates")) {
                JsonArray candidates = response.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    
                    // 檢查是否被安全過濾器阻擋
                    if (candidate.has("finishReason")) {
                        String finishReason = candidate.get("finishReason").getAsString();
                        if ("SAFETY".equals(finishReason)) {
                            throw new Exception("內容被安全過濾器阻擋");
                        } else if ("RECITATION".equals(finishReason)) {
                            throw new Exception("內容可能包含重複內容");
                        }
                    }
                    
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                JsonObject part = parts.get(0).getAsJsonObject();
                                if (part.has("text")) {
                                    String translatedText = part.get("text").getAsString().trim();
                                    // 移除可能的引號或格式化字符
                                    translatedText = translatedText.replaceAll("^[\"'`]+|[\"'`]+$", "");
                                    return translatedText;
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
            if (e.getMessage().startsWith("API 錯誤") || e.getMessage().startsWith("內容被")) {
                throw e; // 重新拋出已知錯誤
            }
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