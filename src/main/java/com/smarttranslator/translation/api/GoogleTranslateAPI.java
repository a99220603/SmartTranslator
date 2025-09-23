package com.smarttranslator.translation.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google 翻譯 API 實現
 * 使用免費的 Google Translate API
 */
public class GoogleTranslateAPI implements TranslationAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTranslateAPI.class);
    private static final String API_URL = "https://translate.googleapis.com/translate_a/single";
    
    // 占位符保護模式
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[FORMAT\\]", Pattern.CASE_INSENSITIVE);
    private static final String PROTECTED_PREFIX = "ZZPROTECTEDPLACEHOLDERZZZ";
    
    @Override
    public String translate(String text, String targetLanguage) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // 保護占位符
        Map<String, String> placeholderMap = new HashMap<>();
        String protectedText = protectPlaceholders(text, placeholderMap);
        
        // 構建請求 URL
        String encodedText = URLEncoder.encode(protectedText, StandardCharsets.UTF_8);
        String url = String.format("%s?client=gtx&sl=auto&tl=%s&dt=t&q=%s", 
                API_URL, targetLanguage, encodedText);
        
        // 發送 HTTP 請求
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("HTTP 請求失敗，響應碼: " + responseCode);
        }
        
        // 讀取響應
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        // 解析 JSON 響應
        String translatedText = parseTranslationResponse(response.toString());
        
        // 恢復占位符
        return restorePlaceholders(translatedText, placeholderMap);
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
     * 解析 Google 翻譯 API 的響應
     */
    private String parseTranslationResponse(String jsonResponse) throws Exception {
        try {
            JsonElement root = JsonParser.parseString(jsonResponse);
            JsonArray mainArray = root.getAsJsonArray();
            
            if (mainArray.size() > 0) {
                JsonArray translationArray = mainArray.get(0).getAsJsonArray();
                StringBuilder translatedText = new StringBuilder();
                
                for (JsonElement element : translationArray) {
                    if (element.isJsonArray()) {
                        JsonArray segmentArray = element.getAsJsonArray();
                        if (segmentArray.size() > 0) {
                            translatedText.append(segmentArray.get(0).getAsString());
                        }
                    }
                }
                
                return translatedText.toString();
            }
        } catch (Exception e) {
            LOGGER.error("解析翻譯響應失敗: {}", jsonResponse, e);
            throw new Exception("解析翻譯響應失敗", e);
        }
        
        throw new Exception("無法從響應中提取翻譯結果");
    }
    
    @Override
    public String getApiName() {
        return "Google Translate";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // 測試連接
            URL url = new URL("https://translate.googleapis.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200 || responseCode == 301 || responseCode == 302;
        } catch (Exception e) {
            LOGGER.debug("Google Translate API 不可用", e);
            return false;
        }
    }
}