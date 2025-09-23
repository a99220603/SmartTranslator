package com.smarttranslator.wynncraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Wynnic 和 Gavellian 語言支持類
 * 處理 Wynncraft 中的特殊語言符號和文字
 */
public class WynnicLanguageSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(WynnicLanguageSupport.class);
    
    // Wynnic 語言字符映射 (基於搜索結果中的信息)
    private static final Map<String, String> WYNNIC_CHARACTERS = new HashMap<>();
    static {
        // 基本 Wynnic 字符 (私有使用區域 U+E000-U+F8FF)
        WYNNIC_CHARACTERS.put("\uE000", "A");
        WYNNIC_CHARACTERS.put("\uE001", "B");
        WYNNIC_CHARACTERS.put("\uE002", "C");
        WYNNIC_CHARACTERS.put("\uE003", "D");
        WYNNIC_CHARACTERS.put("\uE004", "E");
        WYNNIC_CHARACTERS.put("\uE005", "F");
        WYNNIC_CHARACTERS.put("\uE006", "G");
        WYNNIC_CHARACTERS.put("\uE007", "H");
        WYNNIC_CHARACTERS.put("\uE008", "I");
        WYNNIC_CHARACTERS.put("\uE009", "J");
        WYNNIC_CHARACTERS.put("\uE00A", "K");
        WYNNIC_CHARACTERS.put("\uE00B", "L");
        WYNNIC_CHARACTERS.put("\uE00C", "M");
        WYNNIC_CHARACTERS.put("\uE00D", "N");
        WYNNIC_CHARACTERS.put("\uE00E", "O");
        WYNNIC_CHARACTERS.put("\uE00F", "P");
        WYNNIC_CHARACTERS.put("\uE010", "Q");
        WYNNIC_CHARACTERS.put("\uE011", "R");
        WYNNIC_CHARACTERS.put("\uE012", "S");
        WYNNIC_CHARACTERS.put("\uE013", "T");
        WYNNIC_CHARACTERS.put("\uE014", "U");
        WYNNIC_CHARACTERS.put("\uE015", "V");
        WYNNIC_CHARACTERS.put("\uE016", "W");
        WYNNIC_CHARACTERS.put("\uE017", "X");
        WYNNIC_CHARACTERS.put("\uE018", "Y");
        WYNNIC_CHARACTERS.put("\uE019", "Z");
        
        // 數字
        WYNNIC_CHARACTERS.put("\uE01A", "0");
        WYNNIC_CHARACTERS.put("\uE01B", "1");
        WYNNIC_CHARACTERS.put("\uE01C", "2");
        WYNNIC_CHARACTERS.put("\uE01D", "3");
        WYNNIC_CHARACTERS.put("\uE01E", "4");
        WYNNIC_CHARACTERS.put("\uE01F", "5");
        WYNNIC_CHARACTERS.put("\uE020", "6");
        WYNNIC_CHARACTERS.put("\uE021", "7");
        WYNNIC_CHARACTERS.put("\uE022", "8");
        WYNNIC_CHARACTERS.put("\uE023", "9");
        
        // 標點符號
        WYNNIC_CHARACTERS.put("\uE024", " ");  // 空格
        WYNNIC_CHARACTERS.put("\uE025", ".");  // 句號
        WYNNIC_CHARACTERS.put("\uE026", ",");  // 逗號
        WYNNIC_CHARACTERS.put("\uE027", "!");  // 驚嘆號
        WYNNIC_CHARACTERS.put("\uE028", "?");  // 問號
        WYNNIC_CHARACTERS.put("\uE029", ":");  // 冒號
        WYNNIC_CHARACTERS.put("\uE02A", ";");  // 分號
    }
    
    // Gavellian 語言字符映射
    private static final Map<String, String> GAVELLIAN_CHARACTERS = new HashMap<>();
    static {
        // Gavellian 字符 (使用不同的私有使用區域範圍)
        GAVELLIAN_CHARACTERS.put("\uE100", "A");
        GAVELLIAN_CHARACTERS.put("\uE101", "B");
        GAVELLIAN_CHARACTERS.put("\uE102", "C");
        GAVELLIAN_CHARACTERS.put("\uE103", "D");
        GAVELLIAN_CHARACTERS.put("\uE104", "E");
        GAVELLIAN_CHARACTERS.put("\uE105", "F");
        GAVELLIAN_CHARACTERS.put("\uE106", "G");
        GAVELLIAN_CHARACTERS.put("\uE107", "H");
        GAVELLIAN_CHARACTERS.put("\uE108", "I");
        GAVELLIAN_CHARACTERS.put("\uE109", "J");
        GAVELLIAN_CHARACTERS.put("\uE10A", "K");
        GAVELLIAN_CHARACTERS.put("\uE10B", "L");
        GAVELLIAN_CHARACTERS.put("\uE10C", "M");
        GAVELLIAN_CHARACTERS.put("\uE10D", "N");
        GAVELLIAN_CHARACTERS.put("\uE10E", "O");
        GAVELLIAN_CHARACTERS.put("\uE10F", "P");
        GAVELLIAN_CHARACTERS.put("\uE110", "Q");
        GAVELLIAN_CHARACTERS.put("\uE111", "R");
        GAVELLIAN_CHARACTERS.put("\uE112", "S");
        GAVELLIAN_CHARACTERS.put("\uE113", "T");
        GAVELLIAN_CHARACTERS.put("\uE114", "U");
        GAVELLIAN_CHARACTERS.put("\uE115", "V");
        GAVELLIAN_CHARACTERS.put("\uE116", "W");
        GAVELLIAN_CHARACTERS.put("\uE117", "X");
        GAVELLIAN_CHARACTERS.put("\uE118", "Y");
        GAVELLIAN_CHARACTERS.put("\uE119", "Z");
        
        // Gavellian 數字和符號
        GAVELLIAN_CHARACTERS.put("\uE11A", "0");
        GAVELLIAN_CHARACTERS.put("\uE11B", "1");
        GAVELLIAN_CHARACTERS.put("\uE11C", "2");
        GAVELLIAN_CHARACTERS.put("\uE11D", "3");
        GAVELLIAN_CHARACTERS.put("\uE11E", "4");
        GAVELLIAN_CHARACTERS.put("\uE11F", "5");
        GAVELLIAN_CHARACTERS.put("\uE120", "6");
        GAVELLIAN_CHARACTERS.put("\uE121", "7");
        GAVELLIAN_CHARACTERS.put("\uE122", "8");
        GAVELLIAN_CHARACTERS.put("\uE123", "9");
    }
    
    // 常見的 Wynnic 短語和翻譯
    private static final Map<String, String> WYNNIC_PHRASES = new HashMap<>();
    static {
        WYNNIC_PHRASES.put("COMING SOON", "即將推出");
        WYNNIC_PHRASES.put("WYNNCRAFT", "溫克拉夫特");
        WYNNIC_PHRASES.put("GAVEL", "加維爾");
        WYNNIC_PHRASES.put("WYNN", "溫恩");
        WYNNIC_PHRASES.put("PROVINCE", "省份");
        WYNNIC_PHRASES.put("QUEST", "任務");
        WYNNIC_PHRASES.put("DUNGEON", "地牢");
        WYNNIC_PHRASES.put("RAID", "團本");
    }
    
    // Wynnic 文本檢測模式
    private static final Pattern WYNNIC_PATTERN = Pattern.compile("[\\uE000-\\uE0FF]+");
    private static final Pattern GAVELLIAN_PATTERN = Pattern.compile("[\\uE100-\\uE1FF]+");
    
    /**
     * 檢測文本是否包含 Wynnic 字符
     */
    public boolean containsWynnicText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return WYNNIC_PATTERN.matcher(text).find();
    }
    
    /**
     * 檢測文本是否包含 Gavellian 字符
     */
    public boolean containsGavellianText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return GAVELLIAN_PATTERN.matcher(text).find();
    }
    
    /**
     * 將 Wynnic 字符轉換為拉丁字符
     */
    public String convertWynnicToLatin(String wynnicText) {
        if (wynnicText == null || wynnicText.isEmpty()) {
            return wynnicText;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < wynnicText.length(); i++) {
            String character = String.valueOf(wynnicText.charAt(i));
            String latinChar = WYNNIC_CHARACTERS.get(character);
            if (latinChar != null) {
                result.append(latinChar);
            } else {
                result.append(character); // 保留非 Wynnic 字符
            }
        }
        
        String converted = result.toString();
        LOGGER.debug("Wynnic 轉換: {} -> {}", wynnicText, converted);
        return converted;
    }
    
    /**
     * 將 Gavellian 字符轉換為拉丁字符
     */
    public String convertGavellianToLatin(String gavellianText) {
        if (gavellianText == null || gavellianText.isEmpty()) {
            return gavellianText;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < gavellianText.length(); i++) {
            String character = String.valueOf(gavellianText.charAt(i));
            String latinChar = GAVELLIAN_CHARACTERS.get(character);
            if (latinChar != null) {
                result.append(latinChar);
            } else {
                result.append(character); // 保留非 Gavellian 字符
            }
        }
        
        String converted = result.toString();
        LOGGER.debug("Gavellian 轉換: {} -> {}", gavellianText, converted);
        return converted;
    }
    
    /**
     * 將拉丁字符轉換回 Wynnic 字符
     */
    public String convertLatinToWynnic(String latinText) {
        if (latinText == null || latinText.isEmpty()) {
            return latinText;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < latinText.length(); i++) {
            String character = String.valueOf(latinText.charAt(i)).toUpperCase();
            String wynnicChar = getWynnicCharacter(character);
            if (wynnicChar != null) {
                result.append(wynnicChar);
            } else {
                result.append(latinText.charAt(i)); // 保留無法轉換的字符
            }
        }
        
        return result.toString();
    }
    
    /**
     * 將拉丁字符轉換回 Gavellian 字符
     */
    public String convertLatinToGavellian(String latinText) {
        if (latinText == null || latinText.isEmpty()) {
            return latinText;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < latinText.length(); i++) {
            String character = String.valueOf(latinText.charAt(i)).toUpperCase();
            String gavellianChar = getGavellianCharacter(character);
            if (gavellianChar != null) {
                result.append(gavellianChar);
            } else {
                result.append(latinText.charAt(i)); // 保留無法轉換的字符
            }
        }
        
        return result.toString();
    }
    
    /**
     * 處理混合語言文本
     */
    public String processMultiLanguageText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        
        // 轉換 Wynnic 字符
        if (containsWynnicText(result)) {
            result = convertWynnicToLatin(result);
        }
        
        // 轉換 Gavellian 字符
        if (containsGavellianText(result)) {
            result = convertGavellianToLatin(result);
        }
        
        // 檢查是否有常見短語需要翻譯
        for (Map.Entry<String, String> entry : WYNNIC_PHRASES.entrySet()) {
            if (result.toUpperCase().contains(entry.getKey())) {
                result = result.replaceAll("(?i)" + Pattern.quote(entry.getKey()), entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * 獲取對應的 Wynnic 字符
     */
    private String getWynnicCharacter(String latinChar) {
        for (Map.Entry<String, String> entry : WYNNIC_CHARACTERS.entrySet()) {
            if (entry.getValue().equals(latinChar)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * 獲取對應的 Gavellian 字符
     */
    private String getGavellianCharacter(String latinChar) {
        for (Map.Entry<String, String> entry : GAVELLIAN_CHARACTERS.entrySet()) {
            if (entry.getValue().equals(latinChar)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * 獲取支持的語言類型
     */
    public enum LanguageType {
        WYNNIC,
        GAVELLIAN,
        MIXED,
        NONE
    }
    
    /**
     * 檢測文本的語言類型
     */
    public LanguageType detectLanguageType(String text) {
        if (text == null || text.isEmpty()) {
            return LanguageType.NONE;
        }
        
        boolean hasWynnic = containsWynnicText(text);
        boolean hasGavellian = containsGavellianText(text);
        
        if (hasWynnic && hasGavellian) {
            return LanguageType.MIXED;
        } else if (hasWynnic) {
            return LanguageType.WYNNIC;
        } else if (hasGavellian) {
            return LanguageType.GAVELLIAN;
        } else {
            return LanguageType.NONE;
        }
    }
}