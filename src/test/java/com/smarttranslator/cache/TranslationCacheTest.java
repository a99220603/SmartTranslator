package com.smarttranslator.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

public class TranslationCacheTest {
    
    private MockTranslationCache cache;
    
    @BeforeEach
    void setUp() {
        cache = new MockTranslationCache();
    }
    
    @AfterEach
    void tearDown() {
        if (cache != null) {
            cache.clearCache();
        }
    }
    
    @Test
    void testCacheCreation() {
        assertNotNull(cache);
        assertEquals(0, cache.getCacheSize());
    }
    
    @Test
    void testAddAndGetTranslation() {
        String originalText = "Hello World";
        String translatedText = "你好世界";
        String targetLanguage = "zh-TW";
        
        // 添加翻譯到緩存
        cache.addToCache(originalText, translatedText, targetLanguage);
        
        // 驗證緩存大小
        assertEquals(1, cache.getCacheSize());
        
        // 從緩存獲取翻譯
        String result = cache.getCachedTranslation(originalText, targetLanguage);
        
        assertNotNull(result);
        assertEquals(translatedText, result);
    }
    
    @Test
    void testCacheContains() {
        String originalText = "Test";
        String translatedText = "測試";
        String targetLanguage = "zh-TW";
        
        // 初始狀態不包含翻譯
        assertFalse(cache.containsTranslation(originalText, targetLanguage));
        
        // 添加翻譯
        cache.addToCache(originalText, translatedText, targetLanguage);
        
        // 現在應該包含翻譯
        assertTrue(cache.containsTranslation(originalText, targetLanguage));
    }
    
    @Test
    void testCacheClear() {
        // 添加一些翻譯
        cache.addToCache("Hello", "你好", "zh-TW");
        cache.addToCache("World", "世界", "zh-TW");
        
        assertEquals(2, cache.getCacheSize());
        
        // 清理緩存
        cache.clearCache();
        
        assertEquals(0, cache.getCacheSize());
    }
}