package com.smarttranslator.translation;

import com.smarttranslator.cache.MockTranslationCache;
import java.util.concurrent.CompletableFuture;

/**
 * Mock版本的TranslationManager，用於單元測試
 * 不依賴真實的翻譯服務和RateLimiter
 */
public class MockTranslationManager {
    private final MockTranslationCache cache;
    private int requestCount = 0;
    private final int maxRequestsPerSecond = 10;
    
    public MockTranslationManager(MockTranslationCache cache) {
        this.cache = cache != null ? cache : new MockTranslationCache();
    }
    
    public CompletableFuture<String> translateAsync(String text, String targetLanguage) {
        return CompletableFuture.supplyAsync(() -> {
            // 檢查緩存
            String cached = cache.getCachedTranslation(text, targetLanguage);
            if (cached != null) {
                return cached;
            }
            
            // 模擬速率限制
            if (requestCount >= maxRequestsPerSecond) {
                throw new RuntimeException("Rate limit exceeded");
            }
            requestCount++;
            
            // 模擬翻譯（簡單的字符串轉換）
            String translated = mockTranslate(text, targetLanguage);
            
            // 添加到緩存
            cache.addToCache(text, translated, targetLanguage);
            
            return translated;
        });
    }
    
    public String translate(String text, String targetLanguage) {
        try {
            return translateAsync(text, targetLanguage).get();
        } catch (Exception e) {
            throw new RuntimeException("Translation failed", e);
        }
    }
    
    private String mockTranslate(String text, String targetLanguage) {
        // 簡單的模擬翻譯邏輯
        switch (targetLanguage.toLowerCase()) {
            case "zh-tw":
            case "zh-cn":
                return "[中文]" + text;
            case "ja":
                return "[日本語]" + text;
            case "ko":
                return "[한국어]" + text;
            default:
                return "[" + targetLanguage + "]" + text;
        }
    }
    
    public MockTranslationCache getCache() {
        return cache;
    }
    
    public boolean hasRateLimiter() {
        return true; // 模擬有速率限制器
    }
    
    public void resetRequestCount() {
        requestCount = 0;
    }
}