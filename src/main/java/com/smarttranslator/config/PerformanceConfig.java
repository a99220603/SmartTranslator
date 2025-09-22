package com.smarttranslator.config;

/**
 * 性能配置類
 * 管理翻譯系統的性能相關設置
 */
public class PerformanceConfig {
    
    // 節流配置
    public static final int MAX_CONCURRENT_REQUESTS = 5;
    public static final long MIN_REQUEST_INTERVAL_MS = 100;
    public static final long BATCH_DELAY_MS = 50;
    
    // 緩存配置 - 增加記憶體使用，移除英文翻譯後可分配更多資源
    public static final int DEFAULT_CACHE_SIZE = 5000; // 增加到 5000
    public static final long CACHE_EXPIRE_TIME_MS = 60 * 60 * 1000; // 增加到 60分鐘
    
    // 線程池配置
    public static final int TRANSLATION_THREAD_POOL_SIZE = 8;
    public static final int CLEANUP_THREAD_POOL_SIZE = 1;
    
    // 清理配置
    public static final long CLEANUP_INTERVAL_SECONDS = 30;
    public static final long REQUEST_TIMEOUT_MS = 10000; // 10秒
    public static final long REQUEST_HISTORY_EXPIRE_MS = 30000; // 30秒
    
    // 批量處理配置
    public static final int BATCH_SIZE = 10;
    public static final long BATCH_TIMEOUT_MS = 200;
    
    // 優先級配置
    public static final int HIGH_PRIORITY_QUEUE_SIZE = 50;
    public static final int NORMAL_PRIORITY_QUEUE_SIZE = 100;
    public static final int LOW_PRIORITY_QUEUE_SIZE = 200;
    
    private PerformanceConfig() {
        // 工具類，不允許實例化
    }
    
    /**
     * 根據系統性能動態調整配置
     */
    public static class DynamicConfig {
        private static final Runtime runtime = Runtime.getRuntime();
        
        public static int getOptimalThreadPoolSize() {
            int processors = runtime.availableProcessors();
            // 使用配置文件中的線程池大小設置，如果可用的話
            int configuredSize = 8; // 默認值
            try {
                configuredSize = com.smarttranslator.config.SmartTranslatorConfig.THREAD_POOL_SIZE.get();
            } catch (Exception e) {
                // 如果配置未加載，使用默認值
            }
            
            // 對於翻譯任務，使用更積極的線程池策略
            // 最小 4 個線程，最大為配置值和 CPU 核心數的 1.5 倍中的較小值，但不超過 16
            int maxThreads = Math.min(16, Math.max(configuredSize, (int)(processors * 1.5)));
            return Math.max(4, maxThreads);
        }
        
        public static int getOptimalCacheSize() {
            long maxMemory = runtime.maxMemory();
            long freeMemory = runtime.freeMemory();
            
            // 根據可用內存調整緩存大小 - 更積極的記憶體使用
            if (freeMemory > 200 * 1024 * 1024) { // 200MB以上
                return DEFAULT_CACHE_SIZE * 3; // 15000
            } else if (freeMemory > 100 * 1024 * 1024) { // 100MB以上
                return DEFAULT_CACHE_SIZE * 2; // 10000
            } else if (freeMemory > 50 * 1024 * 1024) { // 50MB以上
                return DEFAULT_CACHE_SIZE; // 5000
            } else {
                return DEFAULT_CACHE_SIZE / 2; // 2500
            }
        }
        
        public static long getOptimalRequestInterval() {
            long freeMemory = runtime.freeMemory();
            
            // 內存不足時增加請求間隔
            if (freeMemory < 50 * 1024 * 1024) { // 50MB以下
                return MIN_REQUEST_INTERVAL_MS * 2;
            }
            return MIN_REQUEST_INTERVAL_MS;
        }
    }
}