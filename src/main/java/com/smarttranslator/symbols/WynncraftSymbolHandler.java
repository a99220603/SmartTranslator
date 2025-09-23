package com.smarttranslator.symbols;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Wynncraft 特殊符號處理器
 * 用於識別、保留和處理 Wynncraft 遊戲中的特殊符號和字符
 */
public class WynncraftSymbolHandler {
    
    // Wynncraft 翡翠符號映射
    private static final Map<String, String> EMERALD_SYMBOLS = new HashMap<>();
    
    // Wynnic 語言字符映射 (Parenthesized Latin Small letters)
    private static final Map<Character, Character> WYNNIC_MAPPING = new HashMap<>();
    
    // Gavellian 語言字符映射 (Circled Latin letters)
    private static final Map<Character, Character> GAVELLIAN_MAPPING = new HashMap<>();
    
    // Minecraft 格式化代碼
    private static final Pattern FORMATTING_CODES = Pattern.compile("§[0-9a-fk-or]");
    
    // Wynncraft 特殊字符模式
    private static final Pattern WYNNCRAFT_SYMBOLS = Pattern.compile("[½¼²ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ]");
    
    // Wynnic 字符模式 (Parenthesized Latin Small letters U+249C-U+24B5)
    private static final Pattern WYNNIC_PATTERN = Pattern.compile("[⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵]");
    
    // Gavellian 字符模式 (Circled Latin letters U+24D0-U+24E9)
    private static final Pattern GAVELLIAN_PATTERN = Pattern.compile("[ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ]");
    
    static {
        // 初始化翡翠符號映射
        EMERALD_SYMBOLS.put("½", "EB"); // Emerald Block
        EMERALD_SYMBOLS.put("¼", "EL"); // Emerald (Liquid)
        EMERALD_SYMBOLS.put("²", "E");  // Emerald
        
        // 初始化 Wynnic 字符映射 (⒜-⒵ 對應 a-z)
        String wynnicChars = "⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵";
        for (int i = 0; i < wynnicChars.length(); i++) {
            WYNNIC_MAPPING.put(wynnicChars.charAt(i), (char)('a' + i));
        }
        
        // 初始化 Gavellian 字符映射 (ⓐ-ⓩ 對應 a-z)
        String gavellianChars = "ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ";
        for (int i = 0; i < gavellianChars.length(); i++) {
            GAVELLIAN_MAPPING.put(gavellianChars.charAt(i), (char)('a' + i));
        }
    }
    
    /**
     * 檢測文本中是否包含 Wynncraft 特殊符號
     */
    public static boolean containsWynncraftSymbols(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        return WYNNCRAFT_SYMBOLS.matcher(text).find() ||
               WYNNIC_PATTERN.matcher(text).find() ||
               GAVELLIAN_PATTERN.matcher(text).find() ||
               FORMATTING_CODES.matcher(text).find();
    }
    
    /**
     * 提取文本中的所有特殊符號和格式化代碼
     */
    public static List<SymbolInfo> extractSymbols(String text) {
        List<SymbolInfo> symbols = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return symbols;
        }
        
        // 提取格式化代碼
        Matcher formattingMatcher = FORMATTING_CODES.matcher(text);
        while (formattingMatcher.find()) {
            symbols.add(new SymbolInfo(
                formattingMatcher.group(),
                formattingMatcher.start(),
                formattingMatcher.end(),
                SymbolType.FORMATTING_CODE
            ));
        }
        
        // 提取 Wynncraft 特殊符號
        Matcher symbolMatcher = WYNNCRAFT_SYMBOLS.matcher(text);
        while (symbolMatcher.find()) {
            symbols.add(new SymbolInfo(
                symbolMatcher.group(),
                symbolMatcher.start(),
                symbolMatcher.end(),
                SymbolType.WYNNCRAFT_SYMBOL
            ));
        }
        
        // 提取 Wynnic 字符
        Matcher wynnicMatcher = WYNNIC_PATTERN.matcher(text);
        while (wynnicMatcher.find()) {
            symbols.add(new SymbolInfo(
                wynnicMatcher.group(),
                wynnicMatcher.start(),
                wynnicMatcher.end(),
                SymbolType.WYNNIC
            ));
        }
        
        // 提取 Gavellian 字符
        Matcher gavellianMatcher = GAVELLIAN_PATTERN.matcher(text);
        while (gavellianMatcher.find()) {
            symbols.add(new SymbolInfo(
                gavellianMatcher.group(),
                gavellianMatcher.start(),
                gavellianMatcher.end(),
                SymbolType.GAVELLIAN
            ));
        }
        
        // 按位置排序
        symbols.sort(Comparator.comparingInt(s -> s.startIndex));
        
        return symbols;
    }
    
    /**
     * 準備翻譯文本 - 移除特殊符號但保留位置信息
     */
    public static TextWithSymbols prepareForTranslation(String originalText) {
        List<SymbolInfo> symbols = extractSymbols(originalText);
        
        if (symbols.isEmpty()) {
            return new TextWithSymbols(originalText, originalText, symbols);
        }
        
        StringBuilder cleanText = new StringBuilder();
        int lastIndex = 0;
        
        for (SymbolInfo symbol : symbols) {
            // 添加符號前的文本
            cleanText.append(originalText, lastIndex, symbol.startIndex);
            
            // 根據符號類型處理
            switch (symbol.type) {
                case FORMATTING_CODE:
                    // 格式化代碼用佔位符替換
                    cleanText.append(" [FORMAT] ");
                    break;
                case WYNNCRAFT_SYMBOL:
                    // 翡翠符號轉換為可讀文本
                    String readable = EMERALD_SYMBOLS.get(symbol.symbol);
                    if (readable != null) {
                        cleanText.append(" ").append(readable).append(" ");
                    } else {
                        cleanText.append(" [SYMBOL] ");
                    }
                    break;
                case WYNNIC:
                    // Wynnic 字符轉換為英文
                    char wynnicChar = symbol.symbol.charAt(0);
                    Character englishChar = WYNNIC_MAPPING.get(wynnicChar);
                    if (englishChar != null) {
                        cleanText.append(englishChar);
                    } else {
                        cleanText.append(" [WYNNIC] ");
                    }
                    break;
                case GAVELLIAN:
                    // Gavellian 字符轉換為英文
                    char gavellianChar = symbol.symbol.charAt(0);
                    Character englishChar2 = GAVELLIAN_MAPPING.get(gavellianChar);
                    if (englishChar2 != null) {
                        cleanText.append(englishChar2);
                    } else {
                        cleanText.append(" [GAVELLIAN] ");
                    }
                    break;
            }
            
            lastIndex = symbol.endIndex;
        }
        
        // 添加剩餘文本
        cleanText.append(originalText.substring(lastIndex));
        
        return new TextWithSymbols(originalText, cleanText.toString(), symbols);
    }
    
    /**
     * 恢復翻譯後的文本 - 將特殊符號重新插入翻譯結果
     */
    public static String restoreSymbols(String translatedText, TextWithSymbols originalData) {
        if (originalData.symbols.isEmpty()) {
            return translatedText;
        }
        
        String result = translatedText;
        
        // 創建替換映射表
        Map<String, String> replacements = new HashMap<>();
        
        for (SymbolInfo symbol : originalData.symbols) {
            switch (symbol.type) {
                case FORMATTING_CODE:
                    replacements.put(" [FORMAT] ", symbol.symbol);
                    break;
                case WYNNCRAFT_SYMBOL:
                    String readable = EMERALD_SYMBOLS.get(symbol.symbol);
                    if (readable != null) {
                        replacements.put(" " + readable + " ", symbol.symbol);
                    } else {
                        replacements.put(" [SYMBOL] ", symbol.symbol);
                    }
                    break;
                case WYNNIC:
                    char wynnicChar = symbol.symbol.charAt(0);
                    Character englishChar = WYNNIC_MAPPING.get(wynnicChar);
                    if (englishChar != null) {
                        replacements.put(englishChar.toString(), symbol.symbol);
                    }
                    break;
                case GAVELLIAN:
                    char gavellianChar = symbol.symbol.charAt(0);
                    Character englishChar2 = GAVELLIAN_MAPPING.get(gavellianChar);
                    if (englishChar2 != null) {
                        replacements.put(englishChar2.toString(), symbol.symbol);
                    }
                    break;
            }
        }
        
        // 按照佔位符長度從長到短排序，避免短的佔位符先被替換導致問題
        List<Map.Entry<String, String>> sortedReplacements = replacements.entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
            .toList();
        
        // 執行替換
        for (Map.Entry<String, String> entry : sortedReplacements) {
            result = replaceFirstOccurrence(result, entry.getKey(), entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 替換字符串中第一次出現的目標文本
     */
    private static String replaceFirstOccurrence(String text, String target, String replacement) {
        int index = text.indexOf(target);
        if (index != -1) {
            return text.substring(0, index) + replacement + text.substring(index + target.length());
        }
        // 如果找不到目標文本，保持原樣
        return text;
    }
    
    /**
     * 符號信息類
     */
    public static class SymbolInfo {
        public final String symbol;
        public final int startIndex;
        public final int endIndex;
        public final SymbolType type;
        
        public SymbolInfo(String symbol, int startIndex, int endIndex, SymbolType type) {
            this.symbol = symbol;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.type = type;
        }
        
        @Override
        public String toString() {
            return String.format("SymbolInfo{symbol='%s', start=%d, end=%d, type=%s}", 
                               symbol, startIndex, endIndex, type);
        }
    }
    
    /**
     * 帶符號信息的文本類
     */
    public static class TextWithSymbols {
        public final String originalText;
        public final String cleanText;
        public final List<SymbolInfo> symbols;
        
        public TextWithSymbols(String originalText, String cleanText, List<SymbolInfo> symbols) {
            this.originalText = originalText;
            this.cleanText = cleanText;
            this.symbols = symbols;
        }
    }
    
    /**
     * 符號類型枚舉
     */
    public enum SymbolType {
        FORMATTING_CODE,    // Minecraft 格式化代碼 (§)
        WYNNCRAFT_SYMBOL,   // Wynncraft 特殊符號 (翡翠等)
        WYNNIC,            // Wynnic 語言字符
        GAVELLIAN          // Gavellian 語言字符
    }
}