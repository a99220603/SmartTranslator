package com.smarttranslator.wynncraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import com.smarttranslator.symbols.WynncraftSymbolHandler;

/**
 * Wynncraft 文本處理器
 * 整合符號處理到翻譯流程中
 */
public class WynncraftTextProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(WynncraftTextProcessor.class);
    
    private final WynncraftSymbolHandler symbolHandler;
    
    // Wynncraft 相關文本模式
    private static final Pattern WYNNCRAFT_PATTERN = Pattern.compile(
        ".*(?:" +
        "[\\uE000-\\uF8FF]|" +  // 私有使用區域字符
        "§[0-9a-fk-or]|" +      // Minecraft 格式代碼
        "[ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ]|" + // 擴展拉丁字符
        "[\\u2600-\\u26FF]|" +  // 雜項符號
        "[\\u2700-\\u27BF]|" +  // 裝飾符號
        "[\\u1F300-\\u1F5FF]|" + // 雜項符號和象形文字
        "[\\u1F600-\\u1F64F]|" + // 表情符號
        "[\\u1F680-\\u1F6FF]|" + // 交通和地圖符號
        "[\\u1F700-\\u1F77F]|" + // 煉金術符號
        "[\\u1F780-\\u1F7FF]|" + // 幾何形狀擴展
        "[\\u1F800-\\u1F8FF]" +  // 補充箭頭-C
        ").*",
        Pattern.CASE_INSENSITIVE
    );
    
    public WynncraftTextProcessor() {
        this.symbolHandler = new WynncraftSymbolHandler();
    }
    
    /**
     * 檢測文本是否包含 Wynncraft 相關內容
     */
    public boolean isWynncraftText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // 檢查是否包含 Wynncraft 特殊符號
        if (symbolHandler.containsWynncraftSymbols(text)) {
            return true;
        }
        
        // 檢查是否匹配 Wynncraft 文本模式
        return WYNNCRAFT_PATTERN.matcher(text).matches();
    }
    
    /**
     * 處理 Wynncraft 文本翻譯
     * 
     * @param originalText 原始文本
     * @param translator 翻譯函數
     * @return 處理後的翻譯結果
     */
    public CompletableFuture<String> processWynncraftTranslation(
            String originalText, 
            java.util.function.Function<String, CompletableFuture<String>> translator) {
        
        if (!isWynncraftText(originalText)) {
            // 不是 Wynncraft 文本，直接翻譯
            return translator.apply(originalText);
        }
        
        LOGGER.debug("處理 Wynncraft 文本: {}", originalText);
        
        // 提取符號並獲取純文本
        WynncraftSymbolHandler.TextWithSymbols symbolData = symbolHandler.prepareForTranslation(originalText);
        String cleanText = symbolData.cleanText;
        
        if (cleanText.trim().isEmpty()) {
            // 只有符號，不需要翻譯
            LOGGER.debug("文本只包含符號，跳過翻譯: {}", originalText);
            return CompletableFuture.completedFuture(originalText);
        }
        
        // 翻譯純文本
        return translator.apply(cleanText)
            .thenApply(translatedText -> {
                // 恢復符號
                String result = symbolHandler.restoreSymbols(translatedText, symbolData);
                LOGGER.debug("Wynncraft 文本處理完成: {} -> {}", originalText, result);
                return result;
            })
            .exceptionally(throwable -> {
                LOGGER.error("Wynncraft 文本翻譯失敗: {}", originalText, throwable);
                return originalText; // 翻譯失敗時返回原文
            });
    }
    
    /**
     * 預處理 Wynncraft 文本
     * 清理和標準化文本格式
     */
    public String preprocessWynncraftText(String text) {
        if (text == null) {
            return null;
        }
        
        // 移除多餘的空白字符
        text = text.trim().replaceAll("\\s+", " ");
        
        // 標準化 Minecraft 格式代碼
        text = text.replaceAll("§([0-9a-fk-or])", "§$1");
        
        return text;
    }
    
    /**
     * 後處理翻譯結果
     * 確保符號和格式正確恢復
     */
    public String postprocessWynncraftText(String translatedText, String originalText) {
        if (translatedText == null) {
            return originalText;
        }
        
        // 如果翻譯結果與原文相同，直接返回
        if (translatedText.equals(originalText)) {
            return translatedText;
        }
        
        // 確保重要的格式代碼被保留
        if (originalText.contains("§") && !translatedText.contains("§")) {
            LOGGER.warn("翻譯結果可能丟失了格式代碼: {} -> {}", originalText, translatedText);
        }
        
        return translatedText;
    }
    
    /**
     * 獲取符號處理器實例
     */
    public WynncraftSymbolHandler getSymbolHandler() {
        return symbolHandler;
    }
}