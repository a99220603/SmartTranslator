package com.smarttranslator.translation;

import com.smarttranslator.translation.api.TranslationAPI;
import com.smarttranslator.translation.api.GoogleTranslateAPI;
import com.smarttranslator.translation.api.GoogleAIStudioAPI;
import com.smarttranslator.cache.TranslationCache;
import com.smarttranslator.config.SmartTranslatorConfig;
import com.smarttranslator.config.PerformanceConfig;
import com.smarttranslator.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 翻譯管理器
 * 負責協調翻譯API、緩存和配置
 */
public class TranslationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationManager.class);
    
    private final TranslationCache cache;
    private final ExecutorService executorService;
    private final RateLimiter rateLimiter;
    private final ScheduledExecutorService cleanupScheduler;
    private TranslationAPI currentAPI;
    
    // 跳過翻譯的模式（數字、符號等）
    private static final Pattern SKIP_PATTERN = Pattern.compile("^[\\d\\s\\p{Punct}]+$");
    
    public TranslationManager(TranslationCache cache) {
        this.cache = cache;
        this.executorService = Executors.newFixedThreadPool(
            PerformanceConfig.DynamicConfig.getOptimalThreadPoolSize()
        );
        // 初始化速率限制器：每秒最多10個請求，突發容量20
        this.rateLimiter = new RateLimiter(20, 10);
        this.cleanupScheduler = Executors.newScheduledThreadPool(
            PerformanceConfig.CLEANUP_THREAD_POOL_SIZE
        );
        
        // 初始化翻譯API
        String apiType = SmartTranslatorConfig.TRANSLATION_API.get();
        if ("google_ai_studio".equalsIgnoreCase(apiType) || "gemini".equalsIgnoreCase(apiType)) {
            this.currentAPI = new GoogleAIStudioAPI();
        } else {
            this.currentAPI = new GoogleTranslateAPI();
        }
        
        LOGGER.info("翻譯管理器初始化完成，使用API: {}, 線程池大小: {}, 速率限制: 10 req/s", 
            currentAPI.getClass().getSimpleName(),
            PerformanceConfig.DynamicConfig.getOptimalThreadPoolSize());
    }
    
    /**
     * 異步翻譯文本（使用速率限制）
     */
    public CompletableFuture<String> translateAsync(String text) {
        if (!shouldTranslate(text)) {
            return CompletableFuture.completedFuture(text);
        }
        
        // 預處理文本（處理特殊符號和格式化代碼）
        String originalText = text;
        String processedText = MinecraftTextProcessor.preprocessText(text);
        
        // 檢查緩存
        String targetLanguage = SmartTranslatorConfig.TARGET_LANGUAGE.get();
        String cached = cache.getCachedTranslation(processedText, targetLanguage);
        if (cached != null) {
            String result = MinecraftTextProcessor.postprocessText(cached, originalText);
            return CompletableFuture.completedFuture(formatTranslation(result, originalText));
        }
        
        // 使用速率限制器控制翻譯請求
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 嘗試獲取令牌（非阻塞）
                if (!rateLimiter.tryAcquire()) {
                    LOGGER.debug("速率限制觸發，跳過翻譯: {}", text);
                    return text; // 返回原文
                }
                
                String result = currentAPI.translate(processedText, targetLanguage);
                if (result != null && !result.equals(processedText)) {
                    // 後處理翻譯結果
                    String postProcessed = MinecraftTextProcessor.postprocessText(result, originalText);
                    String formatted = formatTranslation(postProcessed, originalText);
                    cache.addToCache(processedText, result, targetLanguage);
                    return formatted;
                }
                return text;
            } catch (Exception e) {
                LOGGER.error("翻譯失敗: {}", text, e);
                return text;
            }
        }, executorService);
    }
    
    /**
     * 內部翻譯方法（由節流器調用）
     */
    CompletableFuture<String> internalTranslateAsync(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String targetLanguage = SmartTranslatorConfig.TARGET_LANGUAGE.get();
                String result = currentAPI.translate(text, targetLanguage);
                if (result != null && !result.equals(text)) {
                    String formatted = formatTranslation(result, text);
                    cache.addToCache(text, result, targetLanguage);
                    return formatted;
                }
                return text;
            } catch (Exception e) {
                LOGGER.error("翻譯失敗: {}", text, e);
                return text;
            }
        }, executorService);
    }
    
    /**
     * 翻譯文字（同步，帶速率限制）
     */
    public String translate(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return originalText;
        }
        
        // 檢查是否需要翻譯
        if (!shouldTranslate(originalText)) {
            return originalText;
        }
        
        // 預處理文本（處理特殊符號和格式化代碼）
        String processedText = MinecraftTextProcessor.preprocessText(originalText);
        
        String targetLanguage = SmartTranslatorConfig.TARGET_LANGUAGE.get();
        
        // 先檢查緩存
        String cachedResult = cache.getCachedTranslation(processedText, targetLanguage);
        if (cachedResult != null) {
            String postProcessed = MinecraftTextProcessor.postprocessText(cachedResult, originalText);
            return formatTranslation(postProcessed, originalText);
        }
        
        // 使用速率限制器（阻塞等待）
        try {
            rateLimiter.acquire(); // 阻塞等待令牌
            
            String translatedText = currentAPI.translate(processedText, targetLanguage);
            
            if (translatedText != null && !translatedText.equals(processedText)) {
                // 後處理翻譯結果
                String postProcessed = MinecraftTextProcessor.postprocessText(translatedText, originalText);
                // 添加到緩存
                cache.addToCache(processedText, translatedText, targetLanguage);
                return formatTranslation(postProcessed, originalText);
            }
        } catch (Exception e) {
            LOGGER.error("API 翻譯失敗: {}", originalText, e);
        }
        
        return originalText;
    }
    
    /**
     * 判斷是否需要翻譯
     */
    private boolean shouldTranslate(String text) {
        // 檢查是否啟用自動翻譯
        if (!SmartTranslatorConfig.AUTO_TRANSLATE_ENABLED.get()) {
            return false;
        }
        
        // 過濾不需要翻譯的內容
        if (SKIP_PATTERN.matcher(text.trim()).matches()) {
            return false;
        }
        
        // 檢查是否已經是目標語言
        String targetLanguage = SmartTranslatorConfig.TARGET_LANGUAGE.get();
        if (isTargetLanguage(text, targetLanguage)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 檢查文字是否已經是目標語言
     */
    private boolean isTargetLanguage(String text, String targetLanguage) {
        if (targetLanguage.startsWith("zh")) {
            // 檢查是否包含中文字符
            return text.matches(".*[\\u4e00-\\u9fff].*");
        }
        return false;
    }
    
    /**
     * 格式化翻譯結果
     */
    private String formatTranslation(String translatedText, String originalText) {
        StringBuilder result = new StringBuilder();
        
        // 檢查是否需要顯示原文
        boolean showOriginal = SmartTranslatorConfig.SHOW_ORIGINAL_TEXT.get();
        boolean showStatus = SmartTranslatorConfig.SHOW_TRANSLATION_STATUS.get();
        
        if (showOriginal && showStatus) {
            // 同時顯示狀態和原文時，使用更清晰的格式
            result.append("§6[原文]§r ").append(originalText)
                  .append(" §a→§r ").append(translatedText);
        } else if (showOriginal) {
            // 只顯示原文對照時，使用簡潔格式
            result.append("§7").append(originalText).append("§r → ").append(translatedText);
        } else {
            // 只顯示翻譯結果 - 改善顏色處理
            if (showStatus) {
                String prefix = SmartTranslatorConfig.TRANSLATION_PREFIX.get();
                // 如果翻譯文本已經包含顏色代碼，不添加前綴顏色
                if (translatedText.matches(".*§[0-9a-fk-or].*")) {
                    result.append(prefix);
                } else {
                    result.append(prefix);
                }
            }
            result.append(translatedText);
            
            // 確保在翻譯結束後重置格式，避免影響後續文本
            if (!translatedText.endsWith("§r")) {
                result.append("§r");
            }
        }
        
        return result.toString();
    }
    
    /**
     * 切換翻譯 API
     */
    public void switchAPI(String apiName) {
        switch (apiName.toLowerCase()) {
            case "google":
                this.currentAPI = new GoogleTranslateAPI();
                LOGGER.info("切換到 Google Translate API");
                break;
            case "google-ai-studio":
            case "gemini":
                this.currentAPI = new GoogleAIStudioAPI();
                LOGGER.info("切換到 Google AI Studio API");
                break;
            default:
                LOGGER.warn("不支援的翻譯 API: {}，使用預設的 Google Translate", apiName);
                this.currentAPI = new GoogleTranslateAPI();
        }
    }
    
    /**
     * 獲取當前 API 名稱
     */
    public String getCurrentAPIName() {
        return currentAPI.getClass().getSimpleName();
    }
    
    /**
     * 關閉翻譯管理器，清理資源
     */
    public void shutdown() {
        LOGGER.info("正在關閉翻譯管理器...");
        
        // 關閉速率限制器
        if (rateLimiter != null) {
            rateLimiter.shutdown();
        }
        
        // 關閉清理調度器
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 關閉執行器
        if (executorService != null && !executorService.isShutdown()) {
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
        
        if (cache != null) {
            cache.saveCache();
        }
        
        LOGGER.info("翻譯管理器已關閉");
    }
    
    /**
     * 直接從緩存中獲取翻譯結果（不觸發新的翻譯請求）
     */
    public String getCachedTranslation(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return null;
        }
        
        if (cache != null) {
            return cache.getCachedTranslation(originalText, "zh");
        }
        
        return null;
    }
    
    /**
     * 檢查緩存中是否存在指定文本的翻譯
     */
    public boolean hasCachedTranslation(String originalText) {
        return getCachedTranslation(originalText) != null;
    }
    
    /**
     * 獲取緩存統計信息和速率限制器狀態
     */
    public String getCacheStats() {
        if (cache != null) {
            var stats = cache.getStats();
            var rateLimiterStats = rateLimiter.getStats();
            return String.format("緩存條目數: %d, 緩存文件: %s, 速率限制器狀態: %s", 
                stats.getTotalEntries(), stats.getCacheFilePath(), rateLimiterStats);
        }
        return "緩存未初始化";
    }
}