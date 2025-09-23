package com.smarttranslator.translation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 顏色代碼保留翻譯器
 * 在翻譯過程中智能保留 Minecraft 格式化代碼的位置和結構
 */
public class ColorPreservingTranslator {
    
    /**
     * 格式化代碼位置信息
     */
    public static class FormattingPosition {
        public final int position;
        public final String code;
        public final String type; // "color" 或 "format"
        
        public FormattingPosition(int position, String code, String type) {
            this.position = position;
            this.code = code;
            this.type = type;
        }
    }
    
    /**
     * 翻譯結果包含原始格式信息
     */
    public static class TranslationResult {
        public final String translatedText;
        public final String originalText;
        public final List<FormattingPosition> formattingPositions;
        
        public TranslationResult(String translatedText, String originalText, List<FormattingPosition> formattingPositions) {
            this.translatedText = translatedText;
            this.originalText = originalText;
            this.formattingPositions = formattingPositions;
        }
    }
    
    /**
     * 預處理文本，提取格式化代碼信息
     */
    public static TranslationResult preprocessForTranslation(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return new TranslationResult("", originalText, new ArrayList<>());
        }
        
        List<FormattingPosition> positions = new ArrayList<>();
        StringBuilder cleanText = new StringBuilder();
        
        Matcher matcher = MinecraftColorCodes.ALL_FORMATTING_PATTERN.matcher(originalText);
        int lastEnd = 0;
        
        while (matcher.find()) {
            // 添加格式化代碼之前的文本
            cleanText.append(originalText.substring(lastEnd, matcher.start()));
            
            // 記錄格式化代碼位置（相對於清理後的文本）
            String code = matcher.group().toLowerCase();
            String type = MinecraftColorCodes.isColorCode(code) ? "color" : "format";
            positions.add(new FormattingPosition(cleanText.length(), code, type));
            
            lastEnd = matcher.end();
        }
        
        // 添加剩餘文本
        cleanText.append(originalText.substring(lastEnd));
        
        return new TranslationResult(cleanText.toString(), originalText, positions);
    }
    
    /**
     * 後處理翻譯結果，智能恢復格式化代碼
     */
    public static String postprocessTranslation(String translatedText, TranslationResult preprocessResult) {
        if (translatedText == null || translatedText.isEmpty() || preprocessResult.formattingPositions.isEmpty()) {
            return translatedText;
        }
        
        // 如果翻譯結果與原始清理文本相同，直接返回原文
        if (translatedText.equals(preprocessResult.translatedText)) {
            return preprocessResult.originalText;
        }
        
        return restoreFormattingCodes(translatedText, preprocessResult);
    }
    
    /**
     * 智能恢復格式化代碼
     */
    private static String restoreFormattingCodes(String translatedText, TranslationResult preprocessResult) {
        StringBuilder result = new StringBuilder();
        List<FormattingPosition> positions = preprocessResult.formattingPositions;
        
        // 策略1: 保持開頭的顏色代碼
        if (!positions.isEmpty() && positions.get(0).position == 0) {
            FormattingPosition firstCode = positions.get(0);
            if ("color".equals(firstCode.type)) {
                result.append(firstCode.code);
            }
        }
        
        // 策略2: 在句子開始處添加主要顏色代碼
        String mainColorCode = findMainColorCode(positions);
        if (mainColorCode != null && !result.toString().startsWith(mainColorCode)) {
            result.insert(0, mainColorCode);
        }
        
        // 添加翻譯文本
        result.append(translatedText);
        
        // 策略3: 如果原文以重置代碼結尾，保持這個特性
        if (!positions.isEmpty()) {
            FormattingPosition lastCode = positions.get(positions.size() - 1);
            if ("§r".equals(lastCode.code)) {
                result.append("§r");
            }
        }
        
        // 策略4: 如果沒有找到合適的顏色代碼，但原文有格式，使用第一個顏色代碼
        if (result.length() == translatedText.length() && !positions.isEmpty()) {
            for (FormattingPosition pos : positions) {
                if ("color".equals(pos.type)) {
                    result.insert(0, pos.code);
                    break;
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * 找到文本中的主要顏色代碼
     */
    private static String findMainColorCode(List<FormattingPosition> positions) {
        // 找到第一個顏色代碼作為主要顏色
        for (FormattingPosition pos : positions) {
            if ("color".equals(pos.type)) {
                return pos.code;
            }
        }
        return null;
    }
    
    /**
     * 高級格式化代碼恢復（保持相對位置）
     */
    public static String advancedRestoreFormatting(String translatedText, TranslationResult preprocessResult) {
        if (translatedText == null || translatedText.isEmpty() || preprocessResult.formattingPositions.isEmpty()) {
            return translatedText;
        }
        
        String originalClean = preprocessResult.translatedText;
        List<FormattingPosition> positions = preprocessResult.formattingPositions;
        
        // 計算文本長度變化比例
        double lengthRatio = (double) translatedText.length() / originalClean.length();
        
        StringBuilder result = new StringBuilder(translatedText);
        
        // 從後往前插入格式化代碼，避免位置偏移
        for (int i = positions.size() - 1; i >= 0; i--) {
            FormattingPosition pos = positions.get(i);
            int newPosition = (int) Math.round(pos.position * lengthRatio);
            
            // 確保位置在有效範圍內
            newPosition = Math.max(0, Math.min(newPosition, result.length()));
            
            // 插入格式化代碼
            result.insert(newPosition, pos.code);
        }
        
        return result.toString();
    }
    
    /**
     * 檢查文本是否需要顏色代碼保留處理
     */
    public static boolean needsColorPreservation(String text) {
        return text != null && MinecraftColorCodes.ALL_FORMATTING_PATTERN.matcher(text).find();
    }
    
    /**
     * 簡化的顏色保留翻譯方法
     */
    public static String translateWithColorPreservation(String originalText, String translatedText) {
        if (!needsColorPreservation(originalText)) {
            return translatedText;
        }
        
        TranslationResult preprocessed = preprocessForTranslation(originalText);
        return postprocessTranslation(translatedText, preprocessed);
    }
}