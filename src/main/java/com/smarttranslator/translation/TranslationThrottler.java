package com.smarttranslator.translation;

import com.smarttranslator.config.PerformanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 翻譯請求節流器
 * 防止過於頻繁的翻譯請求造成遊戲卡頓
 */
public class TranslationThrottler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationThrottler.class);
    
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, PendingTranslation> pendingTranslations;
    private final ConcurrentHashMap<String, Long> lastRequestTime;
    private final AtomicLong requestCounter;
    
    // 性能配置
    private final int maxConcurrentRequests;
    private final long minRequestIntervalMs;
    private final long batchDelayMs;
    
    public TranslationThrottler() {
        this.scheduler = Executors.newScheduledThreadPool(PerformanceConfig.CLEANUP_THREAD_POOL_SIZE);
        this.pendingTranslations = new ConcurrentHashMap<>();
        this.lastRequestTime = new ConcurrentHashMap<>();
        this.requestCounter = new AtomicLong(0);
        
        // 使用動態配置
        this.maxConcurrentRequests = PerformanceConfig.MAX_CONCURRENT_REQUESTS;
        this.minRequestIntervalMs = PerformanceConfig.DynamicConfig.getOptimalRequestInterval();
        this.batchDelayMs = PerformanceConfig.BATCH_DELAY_MS;
    }
    
    /**
     * 節流翻譯請求
     */
    public CompletableFuture<String> throttledTranslate(String originalText, TranslationManager translationManager) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return CompletableFuture.completedFuture(originalText);
        }
        
        String trimmedText = originalText.trim();
        
        // 檢查是否已有相同的待處理請求
        PendingTranslation existing = pendingTranslations.get(trimmedText);
        if (existing != null) {
            LOGGER.debug("重用待處理的翻譯請求: {}", trimmedText);
            return existing.future;
        }
        
        // 檢查請求頻率
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastRequestTime.get(trimmedText);
        
        if (lastTime != null && (currentTime - lastTime) < minRequestIntervalMs) {
            // 請求過於頻繁，延遲處理
            return scheduleDelayedTranslation(trimmedText, translationManager, minRequestIntervalMs);
        }
        
        // 檢查並發請求數量
        if (pendingTranslations.size() >= maxConcurrentRequests) {
            // 達到最大並發數，延遲處理
            return scheduleDelayedTranslation(trimmedText, translationManager, batchDelayMs);
        }
        
        // 立即處理翻譯請求
        return processTranslation(trimmedText, translationManager);
    }
    
    /**
     * 處理翻譯請求
     */
    private CompletableFuture<String> processTranslation(String text, TranslationManager translationManager) {
        CompletableFuture<String> future = new CompletableFuture<>();
        PendingTranslation pending = new PendingTranslation(text, future, System.currentTimeMillis());
        
        pendingTranslations.put(text, pending);
        lastRequestTime.put(text, System.currentTimeMillis());
        
        long requestId = requestCounter.incrementAndGet();
        LOGGER.debug("開始翻譯請求 #{}: {}", requestId, text);
        
        // 異步執行翻譯
        translationManager.internalTranslateAsync(text)
            .whenComplete((result, throwable) -> {
                pendingTranslations.remove(text);
                
                if (throwable != null) {
                    LOGGER.error("翻譯請求 #{} 失敗: {}", requestId, text, throwable);
                    future.complete(text); // 返回原文
                } else {
                    LOGGER.debug("翻譯請求 #{} 完成: {} -> {}", requestId, text, result);
                    future.complete(result);
                }
            });
        
        return future;
    }
    
    /**
     * 延遲處理翻譯請求
     */
    private CompletableFuture<String> scheduleDelayedTranslation(String text, TranslationManager translationManager, long delayMs) {
        CompletableFuture<String> future = new CompletableFuture<>();
        PendingTranslation pending = new PendingTranslation(text, future, System.currentTimeMillis());
        
        pendingTranslations.put(text, pending);
        
        scheduler.schedule(() -> {
            pendingTranslations.remove(text);
            processTranslation(text, translationManager)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        future.complete(text);
                    } else {
                        future.complete(result);
                    }
                });
        }, delayMs, TimeUnit.MILLISECONDS);
        
        LOGGER.debug("延遲 {}ms 處理翻譯請求: {}", delayMs, text);
        return future;
    }
    
    /**
     * 獲取當前狀態統計
     */
    public ThrottlerStats getStats() {
        return new ThrottlerStats(
            pendingTranslations.size(),
            requestCounter.get(),
            maxConcurrentRequests
        );
    }
    
    /**
     * 清理過期的請求記錄
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        lastRequestTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > PerformanceConfig.REQUEST_HISTORY_EXPIRE_MS);
        
        // 清理超時的待處理請求
        pendingTranslations.entrySet().removeIf(entry -> {
            PendingTranslation pending = entry.getValue();
            if ((currentTime - pending.timestamp) > PerformanceConfig.REQUEST_TIMEOUT_MS) {
                pending.future.complete(pending.text); // 返回原文
                return true;
            }
            return false;
        });
    }
    
    /**
     * 關閉節流器
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 完成所有待處理的請求
        pendingTranslations.values().forEach(pending -> {
            if (!pending.future.isDone()) {
                pending.future.complete(pending.text);
            }
        });
        
        pendingTranslations.clear();
        lastRequestTime.clear();
    }
    
    /**
     * 待處理的翻譯請求
     */
    private static class PendingTranslation {
        final String text;
        final CompletableFuture<String> future;
        final long timestamp;
        
        PendingTranslation(String text, CompletableFuture<String> future, long timestamp) {
            this.text = text;
            this.future = future;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 節流器統計信息
     */
    public static class ThrottlerStats {
        private final int pendingRequests;
        private final long totalRequests;
        private final int maxConcurrentRequests;
        
        public ThrottlerStats(int pendingRequests, long totalRequests, int maxConcurrentRequests) {
            this.pendingRequests = pendingRequests;
            this.totalRequests = totalRequests;
            this.maxConcurrentRequests = maxConcurrentRequests;
        }
        
        public int getPendingRequests() { return pendingRequests; }
        public long getTotalRequests() { return totalRequests; }
        public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
        
        @Override
        public String toString() {
            return String.format("待處理: %d/%d, 總請求: %d", 
                pendingRequests, maxConcurrentRequests, totalRequests);
        }
    }
}