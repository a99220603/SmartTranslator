package com.smarttranslator.translation;

import com.smarttranslator.cache.MockTranslationCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

public class TranslationManagerTest {
    
    private MockTranslationManager translationManager;
    private MockTranslationCache cache;
    
    @BeforeEach
    void setUp() {
        cache = new MockTranslationCache();
        translationManager = new MockTranslationManager(cache);
    }
    
    @Test
    void testTranslationManagerCreation() {
        // 測試MockTranslationManager的創建
        assertNotNull(translationManager);
        assertNotNull(translationManager.getCache());
        assertTrue(translationManager.hasRateLimiter());
    }
    
    @Test
    void testTranslationManagerWithNullCache() {
        // 測試使用 null 緩存會創建默認緩存
        MockTranslationManager manager = new MockTranslationManager(null);
        assertNotNull(manager);
        assertNotNull(manager.getCache());
    }
    
    @Test
    void testSyncTranslation() {
        String originalText = "Hello World";
        String targetLanguage = "zh-tw";
        
        String result = translationManager.translate(originalText, targetLanguage);
        
        assertNotNull(result);
        assertTrue(result.contains(originalText));
        assertTrue(result.contains("[中文]"));
    }
    
    @Test
    void testAsyncTranslation() throws Exception {
        String originalText = "Test Message";
        String targetLanguage = "ja";
        
        CompletableFuture<String> future = translationManager.translateAsync(originalText, targetLanguage);
        String result = future.get();
        
        assertNotNull(result);
        assertTrue(result.contains(originalText));
        assertTrue(result.contains("[日本語]"));
    }
    
    @Test
    void testCacheIntegration() {
        String originalText = "Cache Test";
        String targetLanguage = "ko";
        
        // 第一次翻譯
        String result1 = translationManager.translate(originalText, targetLanguage);
        
        // 驗證結果被緩存
        assertTrue(cache.containsTranslation(originalText, targetLanguage));
        
        // 第二次翻譯應該從緩存獲取
        String result2 = translationManager.translate(originalText, targetLanguage);
        
        assertEquals(result1, result2);
    }
}