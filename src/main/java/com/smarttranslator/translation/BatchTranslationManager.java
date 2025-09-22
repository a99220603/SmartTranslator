package com.smarttranslator.translation;

import com.smarttranslator.cache.TranslationCache;
import com.smarttranslator.config.SmartTranslatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 批量翻譯管理器
 * 負責處理多個文本的批量翻譯，提高翻譯效率
 */
public class BatchTranslationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchTranslationManager.class);
    
    private final TranslationManager translationManager;
    private final TranslationCache cache;
    private final ExecutorService executorService;
    private final int maxBatchSize;
    private final long batchTimeoutMs;
    
    public BatchTranslationManager(TranslationManager translationManager, TranslationCache cache) {
        this.translationManager = translationManager;
        this.cache = cache;
        this.maxBatchSize = SmartTranslatorConfig.MAX_BATCH_SIZE.get();
        this.batchTimeoutMs = SmartTranslatorConfig.BATCH_TIMEOUT_MS.get();
        this.executorService = Executors.newFixedThreadPool(
            Math.min(4, Runtime.getRuntime().availableProcessors())
        );
    }
    
    /**
     * 批量翻譯文本列表
     * 
     * @param texts 要翻譯的文本列表
     * @return 翻譯結果映射（原文 -> 譯文）
     */
    public CompletableFuture<Map<String, String>> translateBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        
        // 去重並過濾空文本
        List<String> uniqueTexts = texts.stream()
            .filter(text -> text != null && !text.trim().isEmpty())
            .distinct()
            .collect(Collectors.toList());
            
        if (uniqueTexts.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        
        LOGGER.debug("開始批量翻譯，共 {} 個文本", uniqueTexts.size());
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> results = new ConcurrentHashMap<>();
            String targetLanguage = SmartTranslatorConfig.TARGET_LANGUAGE.get();
            
            // 首先檢查緩存
            List<String> uncachedTexts = new ArrayList<>();
            for (String text : uniqueTexts) {
                String cached = cache.getCachedTranslation(text, targetLanguage);
                if (cached != null) {
                    results.put(text, cached);
                } else {
                    uncachedTexts.add(text);
                }
            }
            
            LOGGER.debug("從緩存中找到 {} 個翻譯，需要翻譯 {} 個", 
                results.size(), uncachedTexts.size());
            
            if (uncachedTexts.isEmpty()) {
                return results;
            }
            
            // 分批處理未緩存的文本
            List<List<String>> batches = createBatches(uncachedTexts, maxBatchSize);
            List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
            
            for (List<String> batch : batches) {
                CompletableFuture<Void> batchFuture = CompletableFuture.runAsync(() -> {
                    processBatch(batch, results, targetLanguage);
                }, executorService);
                batchFutures.add(batchFuture);
            }
            
            // 等待所有批次完成
            try {
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                    .get(batchTimeoutMs * batches.size(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.error("批量翻譯過程中發生錯誤", e);
                // 對於失敗的文本，嘗試單獨翻譯
                handleFailedTranslations(uncachedTexts, results, targetLanguage);
            }
            
            LOGGER.debug("批量翻譯完成，共翻譯 {} 個文本", results.size());
            return results;
        }, executorService);
    }
    
    /**
     * 將文本列表分割成批次
     */
    private List<List<String>> createBatches(List<String> texts, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            batches.add(new ArrayList<>(texts.subList(i, end)));
        }
        return batches;
    }
    
    /**
     * 處理單個批次的翻譯
     */
    private void processBatch(List<String> batch, Map<String, String> results, String targetLanguage) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String text : batch) {
            CompletableFuture<Void> future = translationManager.translateAsync(text)
                .thenAccept(translatedText -> {
                    if (translatedText != null && !translatedText.isEmpty()) {
                        results.put(text, translatedText);
                        // 添加到緩存
                        cache.addToCache(text, translatedText, targetLanguage);
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.warn("翻譯文本失敗: {}", text, throwable);
                    return null;
                });
            futures.add(future);
        }
        
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(batchTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.warn("批次翻譯超時或失敗", e);
        }
    }
    
    /**
     * 處理失敗的翻譯
     */
    private void handleFailedTranslations(List<String> texts, Map<String, String> results, String targetLanguage) {
        for (String text : texts) {
            if (!results.containsKey(text)) {
                try {
                    String translated = translationManager.translate(text);
                    if (translated != null && !translated.isEmpty()) {
                        results.put(text, translated);
                        cache.addToCache(text, translated, targetLanguage);
                    }
                } catch (Exception e) {
                    LOGGER.warn("單獨翻譯文本失敗: {}", text, e);
                }
            }
        }
    }
    
    /**
     * 批量翻譯文本數組
     */
    public CompletableFuture<String[]> translateArray(String[] texts) {
        if (texts == null || texts.length == 0) {
            return CompletableFuture.completedFuture(new String[0]);
        }
        
        List<String> textList = Arrays.asList(texts);
        return translateBatch(textList).thenApply(resultMap -> {
            String[] results = new String[texts.length];
            for (int i = 0; i < texts.length; i++) {
                results[i] = resultMap.getOrDefault(texts[i], texts[i]);
            }
            return results;
        });
    }
    
    /**
     * 預熱緩存 - 批量翻譯常用文本
     */
    public CompletableFuture<Void> preloadCache(List<String> commonTexts) {
        LOGGER.info("開始預熱緩存，共 {} 個常用文本", commonTexts.size());
        
        return translateBatch(commonTexts).thenAccept(results -> {
            LOGGER.info("緩存預熱完成，成功翻譯 {} 個文本", results.size());
        });
    }
    
    /**
     * 關閉批量翻譯管理器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}