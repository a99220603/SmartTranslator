package com.smarttranslator.cache;

import com.smarttranslator.config.PerformanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 緩存優化器
 * 負責緩存的智能管理和優化
 */
public class CacheOptimizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheOptimizer.class);
    
    private final TranslationCache cache;
    private final ScheduledExecutorService scheduler;
    
    // 緩存統計
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    // 訪問頻率統計
    private final Map<String, AccessInfo> accessStats = new ConcurrentHashMap<>();
    
    public CacheOptimizer(TranslationCache cache) {
        this.cache = cache;
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // 定期清理過期緩存和優化
        scheduler.scheduleAtFixedRate(
            this::optimizeCache, 
            PerformanceConfig.CLEANUP_INTERVAL_SECONDS, 
            PerformanceConfig.CLEANUP_INTERVAL_SECONDS, 
            TimeUnit.SECONDS
        );
        
        LOGGER.info("緩存優化器已啟動");
    }
    
    /**
     * 記錄緩存命中
     */
    public void recordHit(String key) {
        hitCount.incrementAndGet();
        updateAccessInfo(key);
    }
    
    /**
     * 記錄緩存未命中
     */
    public void recordMiss(String key) {
        missCount.incrementAndGet();
    }
    
    /**
     * 更新訪問信息
     */
    private void updateAccessInfo(String key) {
        accessStats.compute(key, (k, info) -> {
            if (info == null) {
                return new AccessInfo(1, System.currentTimeMillis());
            } else {
                info.incrementAccess();
                info.updateLastAccess();
                return info;
            }
        });
    }
    
    /**
     * 優化緩存
     */
    private void optimizeCache() {
        try {
            // 清理過期的訪問統計
            cleanupAccessStats();
            
            // 如果緩存大小超過限制，執行LRU清理
            if (cache.getCacheSize() > PerformanceConfig.DEFAULT_CACHE_SIZE) {
                performLRUEviction();
            }
            
            // 記錄統計信息
            logCacheStats();
            
        } catch (Exception e) {
            LOGGER.error("緩存優化過程中發生錯誤", e);
        }
    }
    
    /**
     * 清理過期的訪問統計
     */
    private void cleanupAccessStats() {
        long expireTime = System.currentTimeMillis() - PerformanceConfig.CACHE_EXPIRE_TIME_MS;
        accessStats.entrySet().removeIf(entry -> 
            entry.getValue().getLastAccess() < expireTime
        );
    }
    
    /**
     * 執行LRU清理
     */
    private void performLRUEviction() {
        // 找出最少使用的緩存項目
        String lruKey = accessStats.entrySet().stream()
            .min((e1, e2) -> {
                AccessInfo info1 = e1.getValue();
                AccessInfo info2 = e2.getValue();
                
                // 首先比較訪問次數，然後比較最後訪問時間
                int countCompare = Integer.compare(info1.getAccessCount(), info2.getAccessCount());
                if (countCompare != 0) {
                    return countCompare;
                }
                return Long.compare(info1.getLastAccess(), info2.getLastAccess());
            })
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (lruKey != null) {
            // 移除最少使用的項目
            accessStats.remove(lruKey);
            evictionCount.incrementAndGet();
            LOGGER.debug("執行LRU清理，移除緩存項目: {}", lruKey);
        }
    }
    
    /**
     * 記錄緩存統計信息
     */
    private void logCacheStats() {
        long totalRequests = hitCount.get() + missCount.get();
        if (totalRequests > 0) {
            double hitRate = (double) hitCount.get() / totalRequests * 100;
            LOGGER.info("緩存統計 - 命中率: {:.2f}%, 總請求: {}, 命中: {}, 未命中: {}, 清理: {}, 緩存大小: {}", 
                hitRate, totalRequests, hitCount.get(), missCount.get(), 
                evictionCount.get(), cache.getCacheSize());
        }
    }
    
    /**
     * 獲取緩存命中率
     */
    public double getHitRate() {
        long totalRequests = hitCount.get() + missCount.get();
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) hitCount.get() / totalRequests;
    }
    
    /**
     * 獲取緩存統計信息
     */
    public CacheOptimizerStats getStats() {
        return new CacheOptimizerStats(
            hitCount.get(),
            missCount.get(),
            evictionCount.get(),
            getHitRate(),
            cache.getCacheSize(),
            accessStats.size()
        );
    }
    
    /**
     * 關閉優化器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("緩存優化器已關閉");
    }
    
    /**
     * 重置統計信息
     */
    public void reset() {
        hitCount.set(0);
        missCount.set(0);
        accessStats.clear();
        LOGGER.info("緩存優化器統計信息已重置");
    }
    
    /**
     * 訪問信息類
     */
    private static class AccessInfo {
        private int accessCount;
        private long lastAccess;
        
        public AccessInfo(int accessCount, long lastAccess) {
            this.accessCount = accessCount;
            this.lastAccess = lastAccess;
        }
        
        public void incrementAccess() {
            this.accessCount++;
        }
        
        public void updateLastAccess() {
            this.lastAccess = System.currentTimeMillis();
        }
        
        public int getAccessCount() {
            return accessCount;
        }
        
        public long getLastAccess() {
            return lastAccess;
        }
    }
    
    /**
     * 緩存優化器統計信息
     */
    public static class CacheOptimizerStats {
        private final long hitCount;
        private final long missCount;
        private final long evictionCount;
        private final double hitRate;
        private final int cacheSize;
        private final int accessStatsSize;
        
        public CacheOptimizerStats(long hitCount, long missCount, long evictionCount, 
                                 double hitRate, int cacheSize, int accessStatsSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.evictionCount = evictionCount;
            this.hitRate = hitRate;
            this.cacheSize = cacheSize;
            this.accessStatsSize = accessStatsSize;
        }
        
        // Getters
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getEvictionCount() { return evictionCount; }
        public double getHitRate() { return hitRate; }
        public int getCacheSize() { return cacheSize; }
        public int getAccessStatsSize() { return accessStatsSize; }
    }
}