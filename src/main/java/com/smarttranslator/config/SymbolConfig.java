package com.smarttranslator.config;

import com.smarttranslator.translation.MinecraftTextProcessor;

/**
 * 符號處理配置類
 * 提供符號處理模式的配置和管理
 */
public class SymbolConfig {
    
    // 配置文件中的鍵名
    public static final String SYMBOL_PROCESSING_MODE_KEY = "symbol_processing_mode";
    
    // 默認處理模式
    private static final String DEFAULT_MODE = "PRESERVE_CORE";
    
    /**
     * 從配置字符串獲取處理模式
     * @param modeString 模式字符串
     * @return 處理模式
     */
    public static MinecraftTextProcessor.SymbolProcessingMode getModeFromString(String modeString) {
        if (modeString == null || modeString.trim().isEmpty()) {
            return MinecraftTextProcessor.SymbolProcessingMode.PRESERVE_CORE;
        }
        
        try {
            return MinecraftTextProcessor.SymbolProcessingMode.valueOf(modeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("無效的符號處理模式: " + modeString + "，使用默認模式");
            return MinecraftTextProcessor.SymbolProcessingMode.PRESERVE_CORE;
        }
    }
    
    /**
     * 將處理模式轉換為字符串
     * @param mode 處理模式
     * @return 模式字符串
     */
    public static String getModeString(MinecraftTextProcessor.SymbolProcessingMode mode) {
        return mode.name();
    }
    
    /**
     * 獲取默認處理模式
     * @return 默認處理模式
     */
    public static MinecraftTextProcessor.SymbolProcessingMode getDefaultMode() {
        return getModeFromString(DEFAULT_MODE);
    }
    
    /**
     * 獲取所有可用的處理模式
     * @return 處理模式數組
     */
    public static MinecraftTextProcessor.SymbolProcessingMode[] getAllModes() {
        return MinecraftTextProcessor.SymbolProcessingMode.values();
    }
    
    /**
     * 獲取處理模式的描述
     * @param mode 處理模式
     * @return 模式描述
     */
    public static String getModeDescription(MinecraftTextProcessor.SymbolProcessingMode mode) {
        switch (mode) {
            case TRANSLATE_ALL:
                return "翻譯所有符號 - 將所有 Wynncraft 符號翻譯為中文";
            case PRESERVE_CORE:
                return "保留核心符號 - 保留用戶指定的核心符號（⚔❤✦⬡✤❋），翻譯其他符號";
            case PRESERVE_ALL:
                return "保留所有符號 - 保留所有 Wynncraft 符號不翻譯";
            default:
                return "未知模式";
        }
    }
    
    /**
     * 檢查模式是否有效
     * @param modeString 模式字符串
     * @return 是否有效
     */
    public static boolean isValidMode(String modeString) {
        if (modeString == null || modeString.trim().isEmpty()) {
            return false;
        }
        
        try {
            MinecraftTextProcessor.SymbolProcessingMode.valueOf(modeString.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}