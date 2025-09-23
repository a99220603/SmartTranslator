package com.smarttranslator.cache;

import com.smarttranslator.config.PerformanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 增強版緩存優化器
 * 實現智能 LRU 算法、預加載機制和動態大小調整
 */
public class EnhancedCacheOptimizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedCacheOptimizer.class);
    
    private final TranslationCache cache;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService preloadExecutor;
    
    // 緩存統計
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    private final AtomicLong preloadCount = new AtomicLong(0);
    
    // 訪問模式分析
    private final Map<String, AccessPattern> accessPatterns = new ConcurrentHashMap<>();
    private final Queue<String> recentAccesses = new ConcurrentLinkedQueue<>();
    
    // 預加載候選
    private final Set<String> preloadCandidates = ConcurrentHashMap.newKeySet();
    
    // 動態配置
    private volatile int targetCacheSize;
    private volatile double lastHitRate = 0.0;
    
    public EnhancedCacheOptimizer(TranslationCache cache) {
        this.cache = cache;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.preloadExecutor = Executors.newFixedThreadPool(2);
        this.targetCacheSize = AdvancedCacheConfig.getRecommendedCacheSize();
        
        // 定期優化任務
        scheduler.scheduleAtFixedRate(
            this::performOptimization, 
            30, 30, TimeUnit.SECONDS
        );
        
        // 定期分析訪問模式
        scheduler.scheduleAtFixedRate(
            this::analyzeAccessPatterns, 
            60, 60, TimeUnit.SECONDS
        );
        
        LOGGER.info("增強版緩存優化器已啟動，目標緩存大小: {}", targetCacheSize);
    }
    
    /**
     * 記錄緩存命中
     */
    public void recordHit(String key) {
        hitCount.incrementAndGet();
        updateAccessPattern(key, true);
        trackRecentAccess(key);
    }
    
    /**
     * 記錄緩存未命中
     */
    public void recordMiss(String key) {
        missCount.incrementAndGet();
        updateAccessPattern(key, false);
        trackRecentAccess(key);
        
        // 將未命中的項目加入預加載候選
        if (shouldPreload(key)) {
            preloadCandidates.add(key);
        }
    }
    
    /**
     * 更新訪問模式
     */
    private void updateAccessPattern(String key, boolean hit) {
        accessPatterns.compute(key, (k, pattern) -> {
            if (pattern == null) {
                pattern = new AccessPattern();
            }
            pattern.recordAccess(hit);
            return pattern;
        });
    }
    
    /**
     * 追蹤最近訪問
     */
    private void trackRecentAccess(String key) {
        recentAccesses.offer(key);
        
        // 保持最近訪問記錄在合理範圍內
        while (recentAccesses.size() > 1000) {
            recentAccesses.poll();
        }
    }
    
    /**
     * 執行緩存優化
     */
    private void performOptimization() {
        try {
            // 計算當前統計信息
            long hits = hitCount.get();
            long misses = missCount.get();
            double currentHitRate = hits + misses > 0 ? (double) hits / (hits + misses) : 0.0;
            double memoryUsage = AdvancedCacheConfig.getMemoryUsage();
            
            // 動態調整緩存大小
            if (AdvancedCacheConfig.shouldAdjustCacheSize(targetCacheSize, currentHitRate, memoryUsage)) {
                int newSize = AdvancedCacheConfig.calculateNewCacheSize(targetCacheSize, currentHitRate, memoryUsage);
                if (newSize != targetCacheSize) {
                    LOGGER.info("調整緩存大小: {} -> {} (命中率: {:.2f}%, 內存使用: {:.2f}%)", 
                        targetCacheSize, newSize, currentHitRate * 100, memoryUsage * 100);
                    targetCacheSize = newSize;
                }
            }
            
            // 執行智能 LRU 清理
            performIntelligentEviction();
            
            // 執行預加載
            performPreloading();
            
            // 記錄統計信息
            if (hits + misses > 0) {
                LOGGER.debug("緩存統計 - 命中率: {:.2f}%, 緩存大小: {}, 內存使用: {:.2f}%, 預加載: {}", 
                    currentHitRate * 100, cache.getCacheSize(), memoryUsage * 100, preloadCount.get());
            }
            
            lastHitRate = currentHitRate;
            
        } catch (Exception e) {
            LOGGER.error("緩存優化過程中發生錯誤", e);
        }
    }
    
    /**
     * 執行智能驅逐
     */
    private void performIntelligentEviction() {
        int currentSize = cache.getCacheSize();
        if (currentSize <= targetCacheSize) {
            return;
        }
        
        int itemsToEvict = currentSize - targetCacheSize;
        
        // 獲取所有緩存項目的訪問模式
        List<Map.Entry<String, AccessPattern>> sortedEntries = accessPatterns.entrySet()
            .stream()
            .filter(entry -> cache.containsKey(entry.getKey()))
            .sorted((e1, e2) -> {
                AccessPattern p1 = e1.getValue();
                AccessPattern p2 = e2.getValue();
                
                // 綜合考慮訪問頻率、最近訪問時間和命中率
                double score1 = calculateEvictionScore(p1);
                double score2 = calculateEvictionScore(p2);
                
                return Double.compare(score1, score2);
            })
            .collect(Collectors.toList());
        
        // 驅逐得分最低的項目
        int evicted = 0;
        for (Map.Entry<String, AccessPattern> entry : sortedEntries) {
            if (evicted >= itemsToEvict) {
                break;
            }
            
            String key = entry.getKey();
            if (cache.removeFromCache(key)) {
                accessPatterns.remove(key);
                evictionCount.incrementAndGet();
                evicted++;
            }
        }
        
        if (evicted > 0) {
            LOGGER.debug("智能驅逐完成，移除 {} 個項目", evicted);
        }
    }
    
    /**
     * 計算驅逐得分（越低越容易被驅逐）
     */
    private double calculateEvictionScore(AccessPattern pattern) {
        long timeSinceLastAccess = System.currentTimeMillis() - pattern.getLastAccessTime();
        double accessFrequency = pattern.getAccessCount() / Math.max(1.0, timeSinceLastAccess / 3600000.0); // 每小時訪問次數
        double hitRate = pattern.getHitRate();
        
        // 綜合得分：訪問頻率 * 命中率 / 時間衰減
        double timeDecay = Math.exp(-timeSinceLastAccess / 86400000.0); // 24小時衰減
        return accessFrequency * hitRate * timeDecay;
    }
    
    /**
     * 分析訪問模式
     */
    private void analyzeAccessPatterns() {
        try {
            // 分析最近訪問的模式
            Map<String, Integer> recentAccessCount = new HashMap<>();
            for (String key : recentAccesses) {
                recentAccessCount.merge(key, 1, Integer::sum);
            }
            
            // 識別熱點數據
            List<String> hotKeys = recentAccessCount.entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= 3) // 最近被訪問3次以上
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            if (!hotKeys.isEmpty()) {
                LOGGER.debug("識別到 {} 個熱點數據項", hotKeys.size());
            }
            
            // 清理過期的訪問模式
            long cutoffTime = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L; // 7天前
            accessPatterns.entrySet().removeIf(entry -> 
                entry.getValue().getLastAccessTime() < cutoffTime
            );
            
        } catch (Exception e) {
            LOGGER.error("訪問模式分析失敗", e);
        }
    }
    
    /**
     * 執行預加載
     */
    private void performPreloading() {
        if (preloadCandidates.isEmpty()) {
            return;
        }
        
        // 限制預加載數量
        int maxPreload = Math.min(10, preloadCandidates.size());
        List<String> toPreload = preloadCandidates.stream()
            .limit(maxPreload)
            .collect(Collectors.toList());
        
        for (String key : toPreload) {
            preloadExecutor.submit(() -> {
                try {
                    // 這裡可以實現預加載邏輯
                    // 例如：預測性翻譯、相關內容預加載等
                    preloadCount.incrementAndGet();
                    LOGGER.debug("預加載項目: {}", key);
                } catch (Exception e) {
                    LOGGER.warn("預加載失敗: {}", key, e);
                }
            });
            
            preloadCandidates.remove(key);
        }
    }
    
    /**
     * 判斷是否應該預加載
     */
    private boolean shouldPreload(String key) {
        AccessPattern pattern = accessPatterns.get(key);
        if (pattern == null) {
            return false;
        }
        
        // 如果訪問頻率高且最近被訪問過，則考慮預加載
        return pattern.getAccessCount() >= 2 && 
               (System.currentTimeMillis() - pattern.getLastAccessTime()) < 3600000; // 1小時內
    }
    
    /**
     * 獲取緩存統計信息
     */
    public CacheStats getStats() {
        long hits = hitCount.get();
        long misses = missCount.get();
        double hitRate = hits + misses > 0 ? (double) hits / (hits + misses) : 0.0;
        
        return new CacheStats(
            hits, misses, hitRate, 
            cache.getCacheSize(), targetCacheSize,
            evictionCount.get(), preloadCount.get(),
            AdvancedCacheConfig.getMemoryUsage()
        );
    }
    
    /**
     * 關閉優化器
     */
    public void shutdown() {
        scheduler.shutdown();
        preloadExecutor.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!preloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                preloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            preloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("增強版緩存優化器已關閉");
    }
    
    /**
     * 訪問模式類
     */
    private static class AccessPattern {
        private int accessCount = 0;
        private int hitCount = 0;
        private long lastAccessTime = System.currentTimeMillis();
        
        public void recordAccess(boolean hit) {
            accessCount++;
            if (hit) {
                hitCount++;
            }
            lastAccessTime = System.currentTimeMillis();
        }
        
        public int getAccessCount() {
            return accessCount;
        }
        
        public double getHitRate() {
            return accessCount > 0 ? (double) hitCount / accessCount : 0.0;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
    
    /**
     * 緩存統計信息類
     */
    public static class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final int currentSize;
        private final int targetSize;
        private final long evictionCount;
        private final long preloadCount;
        private final double memoryUsage;
        
        public CacheStats(long hitCount, long missCount, double hitRate, 
                         int currentSize, int targetSize, long evictionCount, 
                         long preloadCount, double memoryUsage) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.currentSize = currentSize;
            this.targetSize = targetSize;
            this.evictionCount = evictionCount;
            this.preloadCount = preloadCount;
            this.memoryUsage = memoryUsage;
        }
        
        // Getters
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public int getCurrentSize() { return currentSize; }
        public int getTargetSize() { return targetSize; }
        public long getEvictionCount() { return evictionCount; }
        public long getPreloadCount() { return preloadCount; }
        public double getMemoryUsage() { return memoryUsage; }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{命中率=%.2f%%, 大小=%d/%d, 驅逐=%d, 預加載=%d, 內存=%.2f%%}",
                hitRate * 100, currentSize, targetSize, evictionCount, preloadCount, memoryUsage * 100
            );
        }
    }
}