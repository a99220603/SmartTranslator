package com.smarttranslator.ratelimit;

import com.smarttranslator.config.SmartTranslatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶算法實現的速率限制器
 * 用於控制翻譯API的調用頻率，避免超出服務限制
 */
public class RateLimiter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiter.class);
    
    private final int maxTokens;           // 桶的最大容量
    private final int refillRate;          // 每秒補充的令牌數
    private final AtomicInteger tokens;    // 當前令牌數
    private final AtomicLong lastRefill;   // 上次補充時間
    private final ScheduledExecutorService scheduler;
    
    // 統計信息
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong rejectedRequests = new AtomicLong(0);
    private final AtomicLong waitingRequests = new AtomicLong(0);
    
    public RateLimiter(int maxTokens, int refillRate) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
        this.tokens = new AtomicInteger(maxTokens);
        this.lastRefill = new AtomicLong(System.currentTimeMillis());
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // 定期補充令牌
        scheduler.scheduleAtFixedRate(this::refillTokens, 1, 1, TimeUnit.SECONDS);
        
        LOGGER.info("速率限制器已初始化: maxTokens={}, refillRate={}/秒", maxTokens, refillRate);
    }
    
    /**
     * 嘗試獲取令牌（非阻塞）
     * @return 是否成功獲取令牌
     */
    public boolean tryAcquire() {
        totalRequests.incrementAndGet();
        
        refillTokens();
        
        int currentTokens = tokens.get();
        if (currentTokens > 0 && tokens.compareAndSet(currentTokens, currentTokens - 1)) {
            LOGGER.debug("令牌獲取成功，剩餘令牌: {}", currentTokens - 1);
            return true;
        }
        
        rejectedRequests.incrementAndGet();
        LOGGER.debug("令牌獲取失敗，當前令牌數: {}", currentTokens);
        return false;
    }
    
    /**
     * 獲取令牌（阻塞直到獲得令牌）
     * @return CompletableFuture，完成時表示獲得了令牌
     */
    public CompletableFuture<Void> acquire() {
        return CompletableFuture.supplyAsync(() -> {
            waitingRequests.incrementAndGet();
            
            while (!tryAcquire()) {
                try {
                    // 計算等待時間
                    long waitTime = calculateWaitTime();
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待令牌時被中斷", e);
                }
            }
            
            waitingRequests.decrementAndGet();
            return null;
        });
    }
    
    /**
     * 獲取令牌（帶超時）
     * @param timeout 超時時間
     * @return CompletableFuture，完成時表示獲得了令牌，超時則拋出異常
     */
    public CompletableFuture<Void> acquire(Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            waitingRequests.incrementAndGet();
            long startTime = System.currentTimeMillis();
            long timeoutMs = timeout.toMillis();
            
            while (!tryAcquire()) {
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    waitingRequests.decrementAndGet();
                    throw new RuntimeException("獲取令牌超時: " + timeout);
                }
                
                try {
                    long waitTime = Math.min(calculateWaitTime(), 
                        timeoutMs - (System.currentTimeMillis() - startTime));
                    if (waitTime > 0) {
                        Thread.sleep(waitTime);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    waitingRequests.decrementAndGet();
                    throw new RuntimeException("等待令牌時被中斷", e);
                }
            }
            
            waitingRequests.decrementAndGet();
            return null;
        });
    }
    
    /**
     * 補充令牌
     */
    private void refillTokens() {
        long now = System.currentTimeMillis();
        long lastRefillTime = lastRefill.get();
        long timePassed = now - lastRefillTime;
        
        if (timePassed >= 1000) { // 至少過了1秒
            long tokensToAdd = (timePassed / 1000) * refillRate;
            if (tokensToAdd > 0 && lastRefill.compareAndSet(lastRefillTime, now)) {
                int currentTokens = tokens.get();
                int newTokens = Math.min(maxTokens, (int) (currentTokens + tokensToAdd));
                tokens.set(newTokens);
                
                if (tokensToAdd > 0) {
                    LOGGER.debug("補充令牌: +{}, 當前令牌數: {}", tokensToAdd, newTokens);
                }
            }
        }
    }
    
    /**
     * 計算等待時間
     */
    private long calculateWaitTime() {
        int currentTokens = tokens.get();
        if (currentTokens > 0) {
            return 50; // 短暫等待後重試
        }
        
        // 計算下次補充令牌的時間
        long now = System.currentTimeMillis();
        long lastRefillTime = lastRefill.get();
        long nextRefillTime = lastRefillTime + 1000; // 下一秒
        
        return Math.max(100, nextRefillTime - now);
    }
    
    /**
     * 獲取當前令牌數
     */
    public int getAvailableTokens() {
        refillTokens();
        return tokens.get();
    }
    
    /**
     * 獲取統計信息
     */
    public RateLimiterStats getStats() {
        return new RateLimiterStats(
            totalRequests.get(),
            rejectedRequests.get(),
            waitingRequests.get(),
            getAvailableTokens(),
            maxTokens,
            refillRate
        );
    }
    
    /**
     * 重置統計信息
     */
    public void resetStats() {
        totalRequests.set(0);
        rejectedRequests.set(0);
        LOGGER.info("速率限制器統計信息已重置");
    }
    
    /**
     * 關閉速率限制器
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
        LOGGER.info("速率限制器已關閉");
    }
    
    /**
     * 速率限制器統計信息
     */
    public static class RateLimiterStats {
        private final long totalRequests;
        private final long rejectedRequests;
        private final long waitingRequests;
        private final int availableTokens;
        private final int maxTokens;
        private final int refillRate;
        
        public RateLimiterStats(long totalRequests, long rejectedRequests, 
                              long waitingRequests, int availableTokens, 
                              int maxTokens, int refillRate) {
            this.totalRequests = totalRequests;
            this.rejectedRequests = rejectedRequests;
            this.waitingRequests = waitingRequests;
            this.availableTokens = availableTokens;
            this.maxTokens = maxTokens;
            this.refillRate = refillRate;
        }
        
        public long getTotalRequests() { return totalRequests; }
        public long getRejectedRequests() { return rejectedRequests; }
        public long getWaitingRequests() { return waitingRequests; }
        public int getAvailableTokens() { return availableTokens; }
        public int getMaxTokens() { return maxTokens; }
        public int getRefillRate() { return refillRate; }
        
        public double getRejectionRate() {
            return totalRequests > 0 ? (double) rejectedRequests / totalRequests : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "RateLimiterStats{total=%d, rejected=%d, waiting=%d, available=%d/%d, rate=%d/s, rejectionRate=%.2f%%}",
                totalRequests, rejectedRequests, waitingRequests, 
                availableTokens, maxTokens, refillRate, getRejectionRate() * 100
            );
        }
    }
}