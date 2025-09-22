package com.smarttranslator.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試性能配置類
 */
public class PerformanceConfigTest {

    @Test
    void testDefaultCacheSize() {
        // 測試默認緩存大小是否為 5000
        assertEquals(5000, PerformanceConfig.DEFAULT_CACHE_SIZE);
    }

    @Test
    void testCacheExpireTime() {
        // 測試緩存過期時間是否為 60 分鐘 (3600000 毫秒)
        assertEquals(60 * 60 * 1000, PerformanceConfig.CACHE_EXPIRE_TIME_MS);
    }

    @Test
    void testOptimalCacheSize() {
        // 測試動態緩存大小計算
        int cacheSize = PerformanceConfig.DynamicConfig.getOptimalCacheSize();
        
        // 測試不同記憶體情況下的緩存大小
        assertTrue(cacheSize > 0, "緩存大小應該大於 0");
        
        // 緩存大小應該是 DEFAULT_CACHE_SIZE 的倍數
        assertTrue(cacheSize % (PerformanceConfig.DEFAULT_CACHE_SIZE / 2) == 0 || 
                  cacheSize == PerformanceConfig.DEFAULT_CACHE_SIZE ||
                  cacheSize == PerformanceConfig.DEFAULT_CACHE_SIZE * 2 ||
                  cacheSize == PerformanceConfig.DEFAULT_CACHE_SIZE * 3);
    }

    @Test
    void testBasicThreadPoolSize() {
        // 測試基本線程池大小配置 - 避免依賴外部配置
        int defaultSize = PerformanceConfig.TRANSLATION_THREAD_POOL_SIZE;
        assertTrue(defaultSize >= 4, "線程池大小應該至少為 4");
        assertTrue(defaultSize <= 32, "線程池大小不應該超過 32");
    }

    @Test
    void testStaticThreadPoolSize() {
        // 測試靜態線程池大小配置
        assertEquals(8, PerformanceConfig.TRANSLATION_THREAD_POOL_SIZE);
        assertEquals(1, PerformanceConfig.CLEANUP_THREAD_POOL_SIZE);
    }

    @Test
    void testRequestInterval() {
        // 測試請求間隔配置
        long interval = PerformanceConfig.DynamicConfig.getOptimalRequestInterval();
        
        assertTrue(interval >= 100, "請求間隔應該至少為 100ms");
        assertTrue(interval <= 2000, "請求間隔不應該超過 2000ms");
    }

    @Test
    void testMemoryOptimization() {
        // 測試記憶體優化後的配置
        assertTrue(PerformanceConfig.DEFAULT_CACHE_SIZE > 1000, 
                  "優化後的緩存大小應該大於原來的 1000");
        
        assertTrue(PerformanceConfig.CACHE_EXPIRE_TIME_MS > 30 * 60 * 1000, 
                  "優化後的緩存過期時間應該大於原來的 30 分鐘");
    }
}