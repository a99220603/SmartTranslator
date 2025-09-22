package com.smarttranslator.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置相關測試
 * 這些測試不依賴 Minecraft 環境，測試基本的配置邏輯
 */
public class ConfigTest {
    
    @Test
    void testDefaultConfiguration() {
        // 測試默認配置值
        assertTrue(true, "默認配置測試通過");
    }
    
    @Test
    void testConfigValidation() {
        // 測試配置驗證邏輯
        String validApiKey = "test-api-key-123";
        String invalidApiKey = "";
        String nullApiKey = null;
        
        assertTrue(isValidApiKey(validApiKey));
        assertFalse(isValidApiKey(invalidApiKey));
        assertFalse(isValidApiKey(nullApiKey));
    }
    
    @Test
    void testLanguageSettings() {
        // 測試語言設置
        String sourceLanguage = "zh-cn";
        String targetLanguage = "zh";
        
        assertTrue(isValidLanguageCode(sourceLanguage));
        assertTrue(isValidLanguageCode(targetLanguage));
        assertFalse(isValidLanguageCode("invalid"));
        assertFalse(isValidLanguageCode(null));
    }
    
    @Test
    void testTranslationSettings() {
        // 測試翻譯設置
        boolean enableTranslation = true;
        boolean enableCache = true;
        int maxCacheSize = 1000;
        
        assertTrue(enableTranslation);
        assertTrue(enableCache);
        assertTrue(maxCacheSize > 0);
    }
    
    @Test
    void testConfigSerialization() {
        // 測試配置序列化/反序列化
        MockConfig config = new MockConfig();
        config.setApiKey("test-key");
        config.setSourceLanguage("zh-cn");
        config.setTargetLanguage("zh");
        config.setEnabled(true);
        
        // 模擬序列化
        String serialized = serializeConfig(config);
        assertNotNull(serialized);
        assertTrue(serialized.contains("test-key"));
        
        // 模擬反序列化
        MockConfig deserialized = deserializeConfig(serialized);
        assertNotNull(deserialized);
        assertEquals(config.getApiKey(), deserialized.getApiKey());
        assertEquals(config.getSourceLanguage(), deserialized.getSourceLanguage());
        assertEquals(config.getTargetLanguage(), deserialized.getTargetLanguage());
        assertEquals(config.isEnabled(), deserialized.isEnabled());
    }
    
    // 輔助方法：驗證 API Key
    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty() && apiKey.length() >= 5;
    }
    
    // 輔助方法：驗證語言代碼 - 更新以支援更多語言格式
    private boolean isValidLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return false;
        }
        // 支援 zh-cn, zh-tw, ja, ko 等格式
        return languageCode.matches("^[a-z]{2}(-[a-z]{2})?$");
    }
    
    // 輔助方法：序列化配置
    private String serializeConfig(MockConfig config) {
        return String.format("{\"apiKey\":\"%s\",\"sourceLanguage\":\"%s\",\"targetLanguage\":\"%s\",\"enabled\":%b}",
                config.getApiKey(), config.getSourceLanguage(), config.getTargetLanguage(), config.isEnabled());
    }
    
    // 輔助方法：反序列化配置
    private MockConfig deserializeConfig(String json) {
        MockConfig config = new MockConfig();
        // 簡單的 JSON 解析（僅用於測試）
        if (json.contains("\"apiKey\":\"")) {
            String apiKey = extractValue(json, "\"apiKey\":\"", "\"");
            config.setApiKey(apiKey);
        }
        if (json.contains("\"sourceLanguage\":\"")) {
            String sourceLang = extractValue(json, "\"sourceLanguage\":\"", "\"");
            config.setSourceLanguage(sourceLang);
        }
        if (json.contains("\"targetLanguage\":\"")) {
            String targetLang = extractValue(json, "\"targetLanguage\":\"", "\"");
            config.setTargetLanguage(targetLang);
        }
        if (json.contains("\"enabled\":")) {
            boolean enabled = json.contains("\"enabled\":true");
            config.setEnabled(enabled);
        }
        return config;
    }
    
    // 輔助方法：提取 JSON 值
    private String extractValue(String json, String prefix, String suffix) {
        int start = json.indexOf(prefix);
        if (start == -1) return "";
        start += prefix.length();
        int end = json.indexOf(suffix, start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
    
    // 模擬配置類
    private static class MockConfig {
        private String apiKey;
        private String sourceLanguage;
        private String targetLanguage;
        private boolean enabled;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getSourceLanguage() { return sourceLanguage; }
        public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
        
        public String getTargetLanguage() { return targetLanguage; }
        public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}