package com.smarttranslator.integration;

import com.smarttranslator.cache.MockTranslationCache;
import com.smarttranslator.translation.MockTranslationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 緩存系統集成測試
 */
public class CacheIntegrationTest {
    
    private MockTranslationCache cache;
    private MockTranslationManager manager;
    
    @BeforeEach
    void setUp() {
        cache = new MockTranslationCache();
        manager = new MockTranslationManager(cache);
    }
    
    @Test
    void testCacheAndManagerIntegration() {
        // 測試緩存和翻譯管理器的集成
        String text = "Hello World";
        String targetLanguage = "zh-tw";
        
        // 第一次翻譯，應該調用翻譯服務並緩存結果
        String result1 = manager.translate(text, targetLanguage);
        assertNotNull(result1);
        assertTrue(result1.contains(text));
        
        // 檢查是否已緩存
        assertTrue(cache.containsTranslation(text, targetLanguage));
        
        // 第二次翻譯，應該從緩存獲取
        String result2 = manager.translate(text, targetLanguage);
        assertEquals(result1, result2);
    }
    
    @Test
    void testMultipleLanguageCache() {
        // 測試多語言緩存
        String text = "Test";
        
        String zhResult = manager.translate(text, "zh-tw");
        String jaResult = manager.translate(text, "ja");
        String koResult = manager.translate(text, "ko");
        
        // 驗證不同語言的翻譯結果
        assertNotEquals(zhResult, jaResult);
        assertNotEquals(jaResult, koResult);
        
        // 驗證都已緩存
        assertTrue(cache.containsTranslation(text, "zh-tw"));
        assertTrue(cache.containsTranslation(text, "ja"));
        assertTrue(cache.containsTranslation(text, "ko"));
    }
    
    @Test
    void testCachePersistence() {
        // 測試緩存持久性
        String text = "Persistence Test";
        String language = "zh-tw";
        
        // 添加到緩存
        String translation = manager.translate(text, language);
        
        // 驗證緩存中存在
        assertTrue(cache.containsTranslation(text, language));
        assertEquals(translation, cache.getCachedTranslation(text, language));
        
        // 清空緩存
        cache.clearCache();
        
        // 驗證緩存已清空
        assertFalse(cache.containsTranslation(text, language));
        assertNull(cache.getCachedTranslation(text, language));
    }
    
    @Test
    void testRateLimitingWithCache() {
        // 測試速率限制與緩存的配合
        manager.resetRequestCount();
        
        String text = "Rate Limit Test";
        String language = "zh-tw";
        
        // 第一次請求
        String result1 = manager.translate(text, language);
        assertNotNull(result1);
        
        // 第二次請求應該從緩存獲取，不受速率限制影響
        String result2 = manager.translate(text, language);
        assertEquals(result1, result2);
        
        // 驗證速率限制器存在
        assertTrue(manager.hasRateLimiter());
    }
}