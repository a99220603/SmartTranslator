package com.smarttranslator.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 翻譯緩存管理器
 * 負責本地翻譯結果的儲存和讀取
 */
public class TranslationCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationCache.class);
    private static final String CACHE_FILE_NAME = "translation_cache.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Map<String, CachedTranslation> cache = new ConcurrentHashMap<>();
    private final Path cacheFilePath;
    private final CacheOptimizer optimizer;
    private final EnhancedCacheOptimizer enhancedOptimizer;
    
    public TranslationCache() {
        // 獲取 Minecraft 配置目錄
        Path configDir = Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "config", "smarttranslator");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("無法創建配置目錄: {}", configDir, e);
        }
        this.cacheFilePath = configDir.resolve(CACHE_FILE_NAME);
        this.optimizer = new CacheOptimizer(this);
        this.enhancedOptimizer = new EnhancedCacheOptimizer(this);
        
        // 載入現有緩存
        loadCache();
    }
    
    /**
     * 生成文本的 MD5 雜湊值作為緩存鍵
     */
    private String generateCacheKey(String originalText, String targetLanguage) {
        try {
            String combined = originalText + "|" + targetLanguage;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("MD5 算法不可用", e);
            return String.valueOf((originalText + targetLanguage).hashCode());
        }
    }
    
    /**
     * 從緩存中獲取翻譯結果
     */
    public String getCachedTranslation(String originalText, String targetLanguage) {
        String key = generateCacheKey(originalText, targetLanguage);
        CachedTranslation cached = cache.get(key);
        
        if (cached != null) {
            // 檢查緩存是否過期（30天）
            long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
            if (System.currentTimeMillis() - cached.getTimestamp() < thirtyDaysInMillis) {
                LOGGER.debug("從緩存中找到翻譯: {} -> {}", originalText, cached.getTranslatedText());
                optimizer.recordHit(key);
                enhancedOptimizer.recordHit(key);
                return cached.getTranslatedText();
            } else {
                // 移除過期的緩存
                cache.remove(key);
                LOGGER.debug("移除過期的緩存項目: {}", originalText);
            }
        }
        
        optimizer.recordMiss(key);
        enhancedOptimizer.recordMiss(key);
        return null;
    }
    
    /**
     * 將翻譯結果添加到緩存
     */
    public void addToCache(String originalText, String translatedText, String targetLanguage) {
        String key = generateCacheKey(originalText, targetLanguage);
        CachedTranslation cached = new CachedTranslation(
            originalText, 
            translatedText, 
            targetLanguage, 
            System.currentTimeMillis()
        );
        
        cache.put(key, cached);
        LOGGER.debug("添加翻譯到緩存: {} -> {}", originalText, translatedText);
        
        // 異步保存緩存
        saveCache();
    }
    
    /**
     * 從檔案載入緩存
     */
    public void loadCache() {
        if (!Files.exists(cacheFilePath)) {
            LOGGER.info("緩存檔案不存在，創建新的緩存");
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(cacheFilePath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, CachedTranslation>>(){}.getType();
            Map<String, CachedTranslation> loadedCache = GSON.fromJson(reader, type);
            
            if (loadedCache != null) {
                cache.clear();
                cache.putAll(loadedCache);
                LOGGER.info("成功載入 {} 個緩存項目", cache.size());
            }
        } catch (IOException e) {
            LOGGER.error("載入緩存檔案失敗", e);
        }
    }
    
    /**
     * 保存緩存到檔案
     */
    public void saveCache() {
        try (Writer writer = Files.newBufferedWriter(cacheFilePath, StandardCharsets.UTF_8)) {
            GSON.toJson(cache, writer);
            LOGGER.debug("緩存已保存到檔案，共 {} 個項目", cache.size());
        } catch (IOException e) {
            LOGGER.error("保存緩存檔案失敗", e);
        }
    }
    
    /**
     * 清除所有緩存
     */
    public void clearCache() {
        cache.clear();
        optimizer.reset();
        saveCache();
        LOGGER.info("翻譯緩存已清除");
    }
    
    /**
     * 獲取緩存大小
     * 
     * @return 緩存項目數量
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * 獲取緩存統計資訊
     */
    public CacheStats getStats() {
        return new CacheStats(cache.size(), cacheFilePath.toString());
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
        return cache;
    }
    
    /**
     * 關閉緩存（清理資源）
     */
    public void shutdown() {
        optimizer.shutdown();
        enhancedOptimizer.shutdown();
        saveCache();
    }
    
    /**
     * 檢查緩存中是否包含指定鍵
     */
    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }
    
    /**
     * 從緩存中移除指定項目
     */
    public boolean removeFromCache(String key) {
        return cache.remove(key) != null;
    }
    
    /**
     * 獲取增強版優化器統計信息
     */
    public EnhancedCacheOptimizer.CacheStats getEnhancedStats() {
        return enhancedOptimizer.getStats();
    }
    
    /**
     * 獲取系統信息
     */
    public String getSystemInfo() {
        return AdvancedCacheConfig.getSystemInfo();
    }
    
    /**
     * 緩存統計資訊類別
     */
    public static class CacheStats {
        private final int totalEntries;
        private final String cacheFilePath;
        
        public CacheStats(int totalEntries, String cacheFilePath) {
            this.totalEntries = totalEntries;
            this.cacheFilePath = cacheFilePath;
        }
        
        public int getTotalEntries() {
            return totalEntries;
        }
        
        public String getCacheFilePath() {
            return cacheFilePath;
        }
    }
}