package com.smarttranslator.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.smarttranslator.config.SmartTranslatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基於Caffeine的高性能翻譯緩存實現
 * 提供更好的性能和內存管理
 */
public class CaffeineTranslationCache extends TranslationCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaffeineTranslationCache.class);
    
    private final Cache<String, CachedTranslation> cache;
    private final CacheOptimizer optimizer;
    private final String cacheFilePath;
    
    public CaffeineTranslationCache() {
        this.cacheFilePath = "translation_cache.dat";
        
        // 使用Caffeine構建高性能緩存
        this.cache = Caffeine.newBuilder()
                .maximumSize(SmartTranslatorConfig.MAX_CACHE_SIZE.get())
                .expireAfterWrite(Duration.ofHours(24)) // 24小時後過期
                .expireAfterAccess(Duration.ofHours(6)) // 6小時未訪問後過期
                .recordStats() // 啟用統計功能
                .removalListener((key, value, cause) -> {
                    LOGGER.debug("緩存項被移除: key={}, cause={}", key, cause);
                })
                .build();
        
        this.optimizer = new CacheOptimizer(this);
        loadCache();
        
        LOGGER.info("Caffeine翻譯緩存已初始化，最大容量: {}", SmartTranslatorConfig.MAX_CACHE_SIZE.get());
    }
    
    /**
     * 獲取緩存的翻譯
     */
    public String getCachedTranslation(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return null;
        }
        
        String key = generateCacheKey(originalText);
        CachedTranslation cachedTranslation = cache.getIfPresent(key);
        
        if (cachedTranslation != null) {
            // 檢查緩存是否過期
            if (isCacheExpired(cachedTranslation)) {
                cache.invalidate(key);
                optimizer.recordMiss(key);
                LOGGER.debug("緩存已過期並移除: {}", key);
                return null;
            }
            
            optimizer.recordHit(key);
            LOGGER.debug("緩存命中: {}", key);
            return cachedTranslation.getTranslatedText();
        } else {
            optimizer.recordMiss(key);
            LOGGER.debug("緩存未命中: {}", key);
            return null;
        }
    }
    
    /**
     * 檢查緩存項是否過期
     */
    private boolean isCacheExpired(CachedTranslation cached) {
        if (cached == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long cacheTime = cached.getTimestamp();
        long maxAge = 24 * 60 * 60 * 1000; // 24小時，單位毫秒
        
        return (currentTime - cacheTime) > maxAge;
    }
    
    /**
     * 添加翻譯到緩存
     */
    public void putTranslation(String originalText, String translatedText) {
        if (originalText == null || originalText.trim().isEmpty() || 
            translatedText == null || translatedText.trim().isEmpty()) {
            return;
        }
        
        String key = generateCacheKey(originalText);
        CachedTranslation cached = new CachedTranslation(
            originalText, 
            translatedText, 
            "auto", // 默認語言
            System.currentTimeMillis()
        );
        cache.put(key, cached);
        
        LOGGER.debug("翻譯已緩存: {} -> {}", key, translatedText);
    }
    
    /**
     * 生成緩存鍵
     */
    private String generateCacheKey(String text) {
        return text.trim().toLowerCase();
    }
    
    /**
     * 保存緩存到文件
     */
    public void saveCache() {
        try {
            Path cachePath = Paths.get(cacheFilePath);
            Files.createDirectories(cachePath.getParent());
            
            Map<String, CachedTranslation> cacheMap = cache.asMap();
            
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(cacheFilePath))) {
                oos.writeObject(new ConcurrentHashMap<>(cacheMap));
                LOGGER.info("緩存已保存到文件: {}, 條目數: {}", cacheFilePath, cacheMap.size());
            }
        } catch (IOException e) {
            LOGGER.error("保存緩存失敗", e);
        }
    }
    
    /**
     * 從文件加載緩存
     */
    @SuppressWarnings("unchecked")
    public void loadCache() {
        File cacheFile = new File(cacheFilePath);
        if (!cacheFile.exists()) {
            LOGGER.info("緩存文件不存在，將創建新緩存");
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(cacheFile))) {
            Map<String, CachedTranslation> loadedCache = (Map<String, CachedTranslation>) ois.readObject();
            
            // 將加載的數據放入Caffeine緩存
            cache.putAll(loadedCache);
            
            LOGGER.info("緩存已從文件加載: {}, 條目數: {}", cacheFilePath, loadedCache.size());
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("加載緩存失敗", e);
        }
    }
    
    /**
     * 清除所有緩存
     */
    public void clearCache() {
        cache.invalidateAll();
        optimizer.reset();
        saveCache();
        LOGGER.info("緩存已清除");
    }
    
    /**
     * 獲取緩存大小
     */
    public int getCacheSize() {
        return (int) cache.estimatedSize();
    }
    
    /**
     * 獲取緩存統計信息
     */
    public TranslationCache.CacheStats getStats() {
        return new TranslationCache.CacheStats((int) cache.estimatedSize(), cacheFilePath);
    }
    
    /**
     * 獲取緩存優化器
     */
    public CacheOptimizer getOptimizer() {
        return optimizer;
    }
    
    /**
     * 獲取內部緩存映射（供優化器使用）
     */
    Map<String, CachedTranslation> getCacheMap() {
        return cache.asMap();
    }
    
    /**
     * 關閉緩存和相關資源
     */
    public void shutdown() {
        saveCache();
        optimizer.shutdown();
        cache.cleanUp();
        LOGGER.info("Caffeine翻譯緩存已關閉");
    }
    
    /**
     * 緩存統計信息類
     */
    public static class CacheStatistics {
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long evictionCount;
        private final long totalEntries;
        private final String cacheFilePath;
        
        public CacheStatistics(long hitCount, long missCount, double hitRate, 
                             long evictionCount, long totalEntries, String cacheFilePath) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.evictionCount = evictionCount;
            this.totalEntries = totalEntries;
            this.cacheFilePath = cacheFilePath;
        }
        
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public long getEvictionCount() { return evictionCount; }
        public long getTotalEntries() { return totalEntries; }
        public String getCacheFilePath() { return cacheFilePath; }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, evictions=%d, entries=%d}",
                hitCount, missCount, hitRate * 100, evictionCount, totalEntries
            );
        }
    }
}