package com.smarttranslator.translation;

import com.smarttranslator.config.SmartTranslatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 優先級翻譯隊列
 * 根據翻譯請求的優先級和類型進行排序處理
 */
public class PriorityTranslationQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(PriorityTranslationQueue.class);
    
    private final PriorityBlockingQueue<TranslationTask> taskQueue;
    private final ExecutorService executorService;
    private final TranslationManager translationManager;
    private final AtomicLong taskIdGenerator;
    private volatile boolean isShutdown = false;
    
    public PriorityTranslationQueue(TranslationManager translationManager) {
        this.translationManager = translationManager;
        this.taskQueue = new PriorityBlockingQueue<>();
        this.taskIdGenerator = new AtomicLong(0);
        
        int threadCount = Math.max(1, SmartTranslatorConfig.MAX_CONCURRENT_TRANSLATIONS.get());
        this.executorService = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "PriorityTranslation-" + taskIdGenerator.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        
        // 啟動工作線程
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(this::processQueue);
        }
        
        LOGGER.info("優先級翻譯隊列已啟動，工作線程數: {}", threadCount);
    }
    
    /**
     * 翻譯優先級枚舉
     */
    public enum Priority {
        CRITICAL(0),    // 關鍵翻譯（如錯誤訊息）
        HIGH(1),        // 高優先級（如UI文本）
        NORMAL(2),      // 普通優先級（如物品名稱）
        LOW(3),         // 低優先級（如描述文本）
        BACKGROUND(4);  // 背景翻譯（如預載入）
        
        private final int value;
        
        Priority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * 翻譯任務類
     */
    public static class TranslationTask implements Comparable<TranslationTask> {
        private final long id;
        private final String text;
        private final Priority priority;
        private final long submitTime;
        private final CompletableFuture<String> future;
        private final String category;
        
        public TranslationTask(long id, String text, Priority priority, String category) {
            this.id = id;
            this.text = text;
            this.priority = priority;
            this.category = category;
            this.submitTime = System.currentTimeMillis();
            this.future = new CompletableFuture<>();
        }
        
        @Override
        public int compareTo(TranslationTask other) {
            // 首先按優先級排序
            int priorityCompare = Integer.compare(this.priority.getValue(), other.priority.getValue());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            
            // 相同優先級按提交時間排序（FIFO）
            return Long.compare(this.submitTime, other.submitTime);
        }
        
        public long getId() { return id; }
        public String getText() { return text; }
        public Priority getPriority() { return priority; }
        public long getSubmitTime() { return submitTime; }
        public CompletableFuture<String> getFuture() { return future; }
        public String getCategory() { return category; }
        
        public long getWaitTime() {
            return System.currentTimeMillis() - submitTime;
        }
    }
    
    /**
     * 提交翻譯任務
     * 
     * @param text 要翻譯的文本
     * @param priority 優先級
     * @param category 分類（用於統計）
     * @return 翻譯結果的Future
     */
    public CompletableFuture<String> submitTranslation(String text, Priority priority, String category) {
        if (isShutdown) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("翻譯隊列已關閉"));
            return future;
        }
        
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture("");
        }
        
        long taskId = taskIdGenerator.incrementAndGet();
        TranslationTask task = new TranslationTask(taskId, text.trim(), priority, category);
        
        taskQueue.offer(task);
        LOGGER.debug("提交翻譯任務 [{}]: {} (優先級: {}, 分類: {})", 
            taskId, text, priority, category);
        
        return task.getFuture();
    }
    
    /**
     * 提交翻譯任務（使用默認優先級）
     */
    public CompletableFuture<String> submitTranslation(String text, String category) {
        return submitTranslation(text, Priority.NORMAL, category);
    }
    
    /**
     * 提交翻譯任務（使用默認優先級和分類）
     */
    public CompletableFuture<String> submitTranslation(String text) {
        return submitTranslation(text, Priority.NORMAL, "default");
    }
    
    /**
     * 處理隊列中的任務
     */
    private void processQueue() {
        while (!isShutdown && !Thread.currentThread().isInterrupted()) {
            try {
                TranslationTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    processTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.error("處理翻譯隊列時發生錯誤", e);
            }
        }
    }
    
    /**
     * 處理單個翻譯任務
     */
    private void processTask(TranslationTask task) {
        long startTime = System.currentTimeMillis();
        
        try {
            LOGGER.debug("開始處理翻譯任務 [{}]: {} (等待時間: {}ms)", 
                task.getId(), task.getText(), task.getWaitTime());
            
            // 執行翻譯
            String result = translationManager.translate(task.getText());
            
            long processingTime = System.currentTimeMillis() - startTime;
            LOGGER.debug("翻譯任務 [{}] 完成，處理時間: {}ms", task.getId(), processingTime);
            
            task.getFuture().complete(result);
            
        } catch (Exception e) {
            LOGGER.error("翻譯任務 [{}] 失敗: {}", task.getId(), task.getText(), e);
            task.getFuture().completeExceptionally(e);
        }
    }
    
    /**
     * 獲取隊列統計信息
     */
    public QueueStats getStats() {
        return new QueueStats(
            taskQueue.size(),
            taskIdGenerator.get(),
            isShutdown
        );
    }
    
    /**
     * 清空隊列
     */
    public void clearQueue() {
        int clearedTasks = taskQueue.size();
        taskQueue.clear();
        LOGGER.info("已清空翻譯隊列，取消 {} 個待處理任務", clearedTasks);
    }
    
    /**
     * 關閉翻譯隊列
     */
    public void shutdown() {
        isShutdown = true;
        
        // 完成所有待處理任務
        while (!taskQueue.isEmpty()) {
            TranslationTask task = taskQueue.poll();
            if (task != null) {
                task.getFuture().completeExceptionally(
                    new IllegalStateException("翻譯隊列已關閉")
                );
            }
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("優先級翻譯隊列已關閉");
    }
    
    /**
     * 隊列統計信息
     */
    public static class QueueStats {
        private final int pendingTasks;
        private final long totalSubmitted;
        private final boolean isShutdown;
        
        public QueueStats(int pendingTasks, long totalSubmitted, boolean isShutdown) {
            this.pendingTasks = pendingTasks;
            this.totalSubmitted = totalSubmitted;
            this.isShutdown = isShutdown;
        }
        
        public int getPendingTasks() { return pendingTasks; }
        public long getTotalSubmitted() { return totalSubmitted; }
        public boolean isShutdown() { return isShutdown; }
        public long getCompletedTasks() { return totalSubmitted - pendingTasks; }
    }
}