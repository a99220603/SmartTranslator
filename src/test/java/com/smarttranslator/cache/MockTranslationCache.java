package com.smarttranslator.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock版本的TranslationCache，用於單元測試
 * 不依賴Minecraft環境
 */
public class MockTranslationCache {
    private final Map<String, CachedTranslation> cache = new ConcurrentHashMap<>();
    
    public void addToCache(String originalText, String translatedText, String targetLanguage) {
        String key = generateCacheKey(originalText, targetLanguage);
        CachedTranslation cachedTranslation = new CachedTranslation(
            originalText, 
            translatedText, 
            targetLanguage, 
            System.currentTimeMillis()
        );
        cache.put(key, cachedTranslation);
    }
    
    public String getCachedTranslation(String originalText, String targetLanguage) {
        String key = generateCacheKey(originalText, targetLanguage);
        CachedTranslation cachedTranslation = cache.get(key);
        return cachedTranslation != null ? cachedTranslation.getTranslatedText() : null;
    }
    
    public boolean containsTranslation(String originalText, String targetLanguage) {
        String key = generateCacheKey(originalText, targetLanguage);
        return cache.containsKey(key);
    }
    
    public void clearCache() {
        cache.clear();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    public Map<String, CachedTranslation> getCacheMap() {
        return new ConcurrentHashMap<>(cache);
    }
    
    private String generateCacheKey(String text, String targetLanguage) {
        return text.trim().toLowerCase() + "_" + targetLanguage;
    }
}