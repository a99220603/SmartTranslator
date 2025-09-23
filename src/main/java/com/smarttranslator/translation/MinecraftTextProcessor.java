package com.smarttranslator.translation;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;

/**
 * Minecraft 文本處理器
 * 處理 Minecraft 格式化代碼、Unicode 轉義序列和 Wynncraft 特殊符號
 */
public class MinecraftTextProcessor {
    
    // Minecraft 格式化代碼模式
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    
    // Unicode 轉義模式
    private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    
    // Wynncraft 核心特殊符號映射（用戶指定的符號）
    private static final Map<String, String> WYNNCRAFT_CORE_SYMBOLS = new HashMap<>();
    
    // Wynncraft 擴展特殊符號映射（其他符號）
    private static final Map<String, String> WYNNCRAFT_EXTENDED_SYMBOLS = new HashMap<>();
    
    // 符號處理模式
    public enum SymbolProcessingMode {
        TRANSLATE_ALL,      // 翻譯所有符號
        PRESERVE_CORE,      // 保留核心符號，翻譯其他
        PRESERVE_ALL        // 保留所有符號
    }
    
    // 默認處理模式
    private static SymbolProcessingMode processingMode = SymbolProcessingMode.PRESERVE_CORE;
    
    static {
        // 核心 Wynncraft 符號（用戶指定的符號，默認保留）
        WYNNCRAFT_CORE_SYMBOLS.put("⚔", "劍");      // 武器
        WYNNCRAFT_CORE_SYMBOLS.put("❤", "生命");     // 血量
        WYNNCRAFT_CORE_SYMBOLS.put("✦", "法力");     // 魔力
        WYNNCRAFT_CORE_SYMBOLS.put("⬡", "防禦");     // 護甲
        WYNNCRAFT_CORE_SYMBOLS.put("✤", "敏捷");     // 速度
        WYNNCRAFT_CORE_SYMBOLS.put("❋", "智力");     // 智慧
        
        // 擴展符號（其他常見符號）
        WYNNCRAFT_EXTENDED_SYMBOLS.put("⛏", "鎬");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🏹", "弓");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🛡", "盾");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🪓", "斧");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🔱", "三叉戟");
        
        // 職業符號
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🏹", "射手");
        // WYNNCRAFT_EXTENDED_SYMBOLS.put("⚔", "戰士"); // 移除重複映射，⚔ 已在核心符號中
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🔮", "法師");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("💀", "刺客");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🌿", "薩滿");
        
        // 其他屬性符號
        WYNNCRAFT_EXTENDED_SYMBOLS.put("💙", "法力");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("⚡", "雷電");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🔥", "火焰");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("💧", "水");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("🌍", "土");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("💨", "風");
        
        // 其他常見符號
        WYNNCRAFT_EXTENDED_SYMBOLS.put("✧", "空心星");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("◆", "菱形");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("◇", "空心菱形");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("●", "實心圓");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("○", "空心圓");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("■", "實心方塊");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("□", "空心方塊");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("▲", "三角形");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("△", "空心三角形");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("►", "右箭頭");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("◄", "左箭頭");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("▼", "下箭頭");
        
        // 數字符號
        WYNNCRAFT_EXTENDED_SYMBOLS.put("①", "1");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("②", "2");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("③", "3");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("④", "4");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("⑤", "5");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("⑥", "6");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("⑦", "7");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("⑧", "8");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("⑨", "9");
        WYNNCRAFT_EXTENDED_SYMBOLS.put("⑩", "10");
    }
    
    /**
     * 設置符號處理模式
     * @param mode 處理模式
     */
    public static void setSymbolProcessingMode(SymbolProcessingMode mode) {
        processingMode = mode;
    }
    
    /**
     * 獲取當前符號處理模式
     * @return 當前處理模式
     */
    public static SymbolProcessingMode getSymbolProcessingMode() {
        return processingMode;
    }
    
    /**
     * 檢查是否為核心符號
     * @param symbol 符號
     * @return 是否為核心符號
     */
    public static boolean isCoreSymbol(String symbol) {
        return WYNNCRAFT_CORE_SYMBOLS.containsKey(symbol);
    }
    
    /**
     * 檢查是否為擴展符號
     * @param symbol 符號
     * @return 是否為擴展符號
     */
    public static boolean isExtendedSymbol(String symbol) {
        return WYNNCRAFT_EXTENDED_SYMBOLS.containsKey(symbol);
    }
    
    /**
     * 獲取符號翻譯（根據處理模式）
     * @param symbol 原始符號
     * @return 翻譯結果或原符號
     */
    public static String getSymbolTranslation(String symbol) {
        switch (processingMode) {
            case PRESERVE_ALL:
                return symbol; // 保留所有符號
            case PRESERVE_CORE:
                if (WYNNCRAFT_CORE_SYMBOLS.containsKey(symbol)) {
                    return symbol; // 保留核心符號
                } else if (WYNNCRAFT_EXTENDED_SYMBOLS.containsKey(symbol)) {
                    return WYNNCRAFT_EXTENDED_SYMBOLS.get(symbol); // 翻譯擴展符號
                }
                return symbol;
            case TRANSLATE_ALL:
                // 翻譯所有符號
                if (WYNNCRAFT_CORE_SYMBOLS.containsKey(symbol)) {
                    return WYNNCRAFT_CORE_SYMBOLS.get(symbol);
                } else if (WYNNCRAFT_EXTENDED_SYMBOLS.containsKey(symbol)) {
                    return WYNNCRAFT_EXTENDED_SYMBOLS.get(symbol);
                }
                return symbol;
            default:
                return symbol;
        }
    }
    
    /**
     * 預處理文本，移除格式化代碼並處理特殊符號
     */
    public static String preprocessText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // 確保文本使用 UTF-8 編碼
            String processedText = ensureUTF8(text);
            
            // 處理 Unicode 轉義序列
            processedText = processUnicodeEscapes(processedText);
            
            // 保存格式化代碼的位置
            Map<Integer, String> formattingCodes = extractFormattingCodes(processedText);
            
            // 移除格式化代碼以便翻譯
            processedText = removeFormattingCodes(processedText);
            
            // 根據處理模式替換 Wynncraft 特殊符號
            processedText = replaceWynncraftSymbols(processedText);
            
            return processedText;
            
        } catch (Exception e) {
            System.err.println("預處理文本失敗: " + text + ", 錯誤: " + e.getMessage());
            return text;
        }
    }
    
    /**
     * 後處理翻譯結果，恢復格式化代碼
     */
    public static String postprocessText(String translatedText, String originalText) {
        if (translatedText == null || translatedText.isEmpty()) {
            return translatedText;
        }
        
        // 如果原文為null，直接返回翻譯文本
        if (originalText == null) {
            return translatedText;
        }
        
        try {
            // 如果翻譯結果與原文相同，直接返回原文（保持格式）
            String cleanOriginal = removeFormattingCodes(originalText);
            String cleanTranslated = removeFormattingCodes(translatedText);
            
            if (cleanOriginal.equals(cleanTranslated)) {
                return originalText;
            }
            
            // 檢查是否需要顏色保留處理
            if (ColorPreservingTranslator.needsColorPreservation(originalText)) {
                String processedText = ColorPreservingTranslator.translateWithColorPreservation(originalText, translatedText);
                
                // 如果翻譯結果沒有顏色代碼，但原文有，則添加主要顏色
                if (!processedText.contains("§") && originalText.contains("§")) {
                    String mainColor = extractMainColor(originalText);
                    if (mainColor != null) {
                        processedText = mainColor + processedText;
                    }
                }
                
                // 確保格式重置
                if (!processedText.endsWith("§r") && originalText.contains("§")) {
                    processedText += "§r";
                }
                
                return processedText;
            }
            
            return translatedText;
            
        } catch (Exception e) {
            // 降級到基本格式恢復
            return restoreBasicFormatting(translatedText, originalText);
        }
    }
    
    /**
     * 提取文本中的主要顏色代碼
     */
    private static String extractMainColor(String text) {
        if (text == null || !text.contains("§")) {
            return null;
        }
        
        // 查找第一個顏色代碼
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                char colorCode = text.charAt(i + 1);
                // 檢查是否為有效的顏色代碼
                if ("0123456789abcdef".indexOf(Character.toLowerCase(colorCode)) != -1) {
                    return "§" + colorCode;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 確保文本使用 UTF-8 編碼
     */
    private static String ensureUTF8(String text) {
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("UTF-8 編碼轉換失敗: " + text + ", 錯誤: " + e.getMessage());
            return text;
        }
    }
    
    /**
     * 處理 Unicode 轉義序列
     */
    private static String processUnicodeEscapes(String text) {
        Matcher matcher = UNICODE_ESCAPE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            try {
                int codePoint = Integer.parseInt(matcher.group(1), 16);
                String unicodeChar = String.valueOf((char) codePoint);
                matcher.appendReplacement(result, Matcher.quoteReplacement(unicodeChar));
            } catch (NumberFormatException e) {
                System.err.println("無效的 Unicode 轉義序列: " + matcher.group(0));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 提取格式化代碼及其位置
     */
    private static Map<Integer, String> extractFormattingCodes(String text) {
        Map<Integer, String> codes = new HashMap<>();
        Matcher matcher = FORMATTING_CODE_PATTERN.matcher(text);
        
        while (matcher.find()) {
            codes.put(matcher.start(), matcher.group());
        }
        
        return codes;
    }
    
    /**
     * 移除格式化代碼
     */
    private static String removeFormattingCodes(String text) {
        return FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }
    
    /**
     * 替換 Wynncraft 特殊符號（根據處理模式）
     * 已整合到 WynncraftSymbolHandler 中，此方法保持向後兼容
     */
    private static String replaceWynncraftSymbols(String text) {
        String result = text;
        
        // 根據處理模式決定要替換的符號
        switch (processingMode) {
            case PRESERVE_ALL:
                // 不替換任何符號 - 使用新的 WynncraftSymbolHandler
                break;
            case PRESERVE_CORE:
                // 只替換擴展符號，保留核心符號
                for (Map.Entry<String, String> entry : WYNNCRAFT_EXTENDED_SYMBOLS.entrySet()) {
                    result = result.replace(entry.getKey(), entry.getValue());
                }
                break;
            case TRANSLATE_ALL:
                // 替換所有符號
                for (Map.Entry<String, String> entry : WYNNCRAFT_CORE_SYMBOLS.entrySet()) {
                    result = result.replace(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, String> entry : WYNNCRAFT_EXTENDED_SYMBOLS.entrySet()) {
                    result = result.replace(entry.getKey(), entry.getValue());
                }
                break;
        }
        
        return result;
    }
    
    /**
     * 恢復基本格式化代碼
     */
    private static String restoreBasicFormatting(String translatedText, String originalText) {
        // 如果原文以顏色代碼開始，嘗試保持
        Matcher originalMatcher = FORMATTING_CODE_PATTERN.matcher(originalText);
        if (originalMatcher.find() && originalMatcher.start() == 0) {
            String firstCode = originalMatcher.group();
            if (!translatedText.startsWith("§")) {
                return firstCode + translatedText;
            }
        }
        
        return translatedText;
    }
    
    /**
     * 檢查文本是否包含需要特殊處理的字符
     */
    public static boolean needsSpecialProcessing(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 檢查是否包含格式化代碼
        if (FORMATTING_CODE_PATTERN.matcher(text).find()) {
            return true;
        }
        
        // 檢查是否包含 Unicode 轉義
        if (UNICODE_ESCAPE_PATTERN.matcher(text).find()) {
            return true;
        }
        
        // 檢查是否包含 Wynncraft 特殊符號
        for (String symbol : WYNNCRAFT_CORE_SYMBOLS.keySet()) {
            if (text.contains(symbol)) {
                return true;
            }
        }
        for (String symbol : WYNNCRAFT_EXTENDED_SYMBOLS.keySet()) {
            if (text.contains(symbol)) {
                return true;
            }
        }
        
        return false;
    }
}