package com.smarttranslator.cache;

import com.smarttranslator.config.SmartTranslatorConfig;

/**
 * 高級緩存配置管理器
 * 基於系統資源和使用模式動態調整緩存參數
 */
public class AdvancedCacheConfig {
    
    // 緩存大小等級
    public enum CacheSize {
        SMALL(5000, "小型緩存 - 適合低內存系統"),
        MEDIUM(15000, "中型緩存 - 平衡性能與內存"),
        LARGE(30000, "大型緩存 - 高性能模式"),
        EXTRA_LARGE(50000, "超大緩存 - 最大性能"),
        ADAPTIVE(-1, "自適應緩存 - 根據系統資源動態調整");
        
        private final int size;
        private final String description;
        
        CacheSize(int size, String description) {
            this.size = size;
            this.description = description;
        }
        
        public int getSize() {
            return size;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 根據系統內存計算最佳緩存大小
     */
    public static int calculateOptimalCacheSize() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long availableMemory = maxMemory - (totalMemory - freeMemory);
        
        // 將可用內存的 2% 用於翻譯緩存
        long cacheMemoryBudget = (long) (availableMemory * 0.02);
        
        // 估算每個緩存項目約佔用 200 字節（包括鍵值對和開銷）
        int estimatedCacheSize = (int) (cacheMemoryBudget / 200);
        
        // 設置合理的範圍限制
        estimatedCacheSize = Math.max(1000, Math.min(estimatedCacheSize, 100000));
        
        return estimatedCacheSize;
    }
    
    /**
     * 獲取推薦的緩存大小
     */
    public static int getRecommendedCacheSize() {
        int configuredSize = SmartTranslatorConfig.MAX_CACHE_SIZE.get();
        
        // 如果配置為自適應模式（-1），則計算最佳大小
        if (configuredSize <= 0) {
            return calculateOptimalCacheSize();
        }
        
        return configuredSize;
    }
    
    /**
     * 檢查是否需要調整緩存大小
     */
    public static boolean shouldAdjustCacheSize(int currentSize, double hitRate, double memoryUsage) {
        // 如果命中率低於 70% 且內存使用率低於 80%，建議增大緩存
        if (hitRate < 0.7 && memoryUsage < 0.8) {
            return true;
        }
        
        // 如果內存使用率超過 90%，建議減小緩存
        if (memoryUsage > 0.9) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 計算建議的新緩存大小
     */
    public static int calculateNewCacheSize(int currentSize, double hitRate, double memoryUsage) {
        if (hitRate < 0.7 && memoryUsage < 0.8) {
            // 增大緩存 20%
            return Math.min((int) (currentSize * 1.2), 100000);
        } else if (memoryUsage > 0.9) {
            // 減小緩存 15%
            return Math.max((int) (currentSize * 0.85), 1000);
        }
        
        return currentSize;
    }
    
    /**
     * 獲取內存使用率
     */
    public static double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return (double) usedMemory / maxMemory;
    }
    
    /**
     * 獲取系統信息摘要
     */
    public static String getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024); // MB
        long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
        long freeMemory = runtime.freeMemory() / (1024 * 1024); // MB
        
        return String.format(
            "系統內存: 最大=%dMB, 總計=%dMB, 可用=%dMB, 使用率=%.1f%%",
            maxMemory, totalMemory, freeMemory, getMemoryUsage() * 100
        );
    }
}