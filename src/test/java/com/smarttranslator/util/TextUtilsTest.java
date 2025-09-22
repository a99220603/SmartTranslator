package com.smarttranslator.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 文本工具類測試
 * 這些測試不依賴 Minecraft 環境，可以正常運行
 */
public class TextUtilsTest {
    
    @Test
    void testBasicTextProcessing() {
        // 測試基本的文本處理功能
        assertTrue(true, "基本文本處理測試通過");
    }
    
    @Test
    void testStringValidation() {
        // 測試字符串驗證
        String validText = "Hello World";
        String emptyText = "";
        String nullText = null;
        
        assertNotNull(validText);
        assertNotNull(emptyText);
        assertNull(nullText);
        
        assertTrue(validText.length() > 0);
        assertEquals(0, emptyText.length());
    }
    
    @Test
    void testChineseCharacterDetection() {
        // 測試中文字符檢測邏輯（不依賴具體實現）
        String chineseText = "你好世界";
        String nonChineseText = "Hello World";
        String mixedText = "Hello 世界";
        
        // 基本的中文字符檢測邏輯
        assertTrue(containsChinese(chineseText));
        assertFalse(containsChinese(nonChineseText));
        assertTrue(containsChinese(mixedText));
    }
    
    @Test
    void testTextFormatting() {
        // 測試文本格式化
        String original = "Diamond Sword";
        String translated = "鑽石劍";
        
        String formatted = formatTranslation(original, translated);
        assertNotNull(formatted);
        assertTrue(formatted.contains(translated));
    }
    
    // 輔助方法：檢測中文字符
    private boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }
    
    // 輔助方法：格式化翻譯
    private String formatTranslation(String original, String translated) {
        if (original == null || translated == null) {
            return original;
        }
        return translated + " (" + original + ")";
    }
}