package com.smarttranslator.translation;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Minecraft 顏色代碼和格式化代碼映射表
 * 基於 Minecraft Wiki 的官方格式化代碼規範
 */
public class MinecraftColorCodes {
    
    // 顏色代碼映射 (§0-§f)
    public static final Map<String, String> COLOR_CODES = new HashMap<>();
    
    // 格式化代碼映射 (§k-§r)
    public static final Map<String, String> FORMAT_CODES = new HashMap<>();
    
    // 所有有效的格式化代碼模式
    public static final Pattern ALL_FORMATTING_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    
    // 僅顏色代碼模式
    public static final Pattern COLOR_ONLY_PATTERN = Pattern.compile("§[0-9a-f]", Pattern.CASE_INSENSITIVE);
    
    // 僅格式代碼模式
    public static final Pattern FORMAT_ONLY_PATTERN = Pattern.compile("§[k-or]", Pattern.CASE_INSENSITIVE);
    
    static {
        // 初始化顏色代碼映射
        COLOR_CODES.put("§0", "黑色");
        COLOR_CODES.put("§1", "深藍色");
        COLOR_CODES.put("§2", "深綠色");
        COLOR_CODES.put("§3", "深青色");
        COLOR_CODES.put("§4", "深紅色");
        COLOR_CODES.put("§5", "深紫色");
        COLOR_CODES.put("§6", "金色");
        COLOR_CODES.put("§7", "灰色");
        COLOR_CODES.put("§8", "深灰色");
        COLOR_CODES.put("§9", "藍色");
        COLOR_CODES.put("§a", "綠色");
        COLOR_CODES.put("§b", "青色");
        COLOR_CODES.put("§c", "紅色");
        COLOR_CODES.put("§d", "粉紅色");
        COLOR_CODES.put("§e", "黃色");
        COLOR_CODES.put("§f", "白色");
        
        // 初始化格式化代碼映射
        FORMAT_CODES.put("§k", "隨機字符");
        FORMAT_CODES.put("§l", "粗體");
        FORMAT_CODES.put("§m", "刪除線");
        FORMAT_CODES.put("§n", "下劃線");
        FORMAT_CODES.put("§o", "斜體");
        FORMAT_CODES.put("§r", "重置");
    }
    
    /**
     * 檢查是否為有效的顏色代碼
     */
    public static boolean isColorCode(String code) {
        return COLOR_CODES.containsKey(code.toLowerCase());
    }
    
    /**
     * 檢查是否為有效的格式代碼
     */
    public static boolean isFormatCode(String code) {
        return FORMAT_CODES.containsKey(code.toLowerCase());
    }
    
    /**
     * 檢查是否為有效的格式化代碼（顏色或格式）
     */
    public static boolean isValidFormattingCode(String code) {
        return isColorCode(code) || isFormatCode(code);
    }
    
    /**
     * 獲取顏色代碼的中文名稱
     */
    public static String getColorName(String code) {
        return COLOR_CODES.get(code.toLowerCase());
    }
    
    /**
     * 獲取格式代碼的中文名稱
     */
    public static String getFormatName(String code) {
        return FORMAT_CODES.get(code.toLowerCase());
    }
    
    /**
     * 獲取格式化代碼的描述
     */
    public static String getFormattingDescription(String code) {
        String lowerCode = code.toLowerCase();
        if (isColorCode(lowerCode)) {
            return getColorName(lowerCode);
        } else if (isFormatCode(lowerCode)) {
            return getFormatName(lowerCode);
        }
        return "未知格式代碼";
    }
    
    /**
     * 將 & 符號轉換為 § 符號（常見的替代格式）
     */
    public static String convertAmpersandCodes(String text) {
        if (text == null) return null;
        return text.replaceAll("&([0-9a-fk-or])", "§$1");
    }
    
    /**
     * 將 § 符號轉換為 & 符號
     */
    public static String convertToAmpersandCodes(String text) {
        if (text == null) return null;
        return text.replaceAll("§([0-9a-fk-or])", "&$1");
    }
}