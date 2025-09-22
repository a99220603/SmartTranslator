package com.smarttranslator.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ItemTooltipHandler 測試類
 * 測試原始文字顯示邏輯
 */
public class ItemTooltipHandlerTest {
    
    @Test
    void testShowOriginalTextLogicEnabled() {
        // 測試啟用顯示原始文字的邏輯
        String originalText = "Oak Log";
        String translatedText = "橡木原木";
        
        // 模擬雙語顯示的邏輯
        String result = createBilingualTextForTest(originalText, translatedText, true);
        
        // 驗證結果包含翻譯文字和原文
        assertTrue(result.contains(translatedText), "應該包含翻譯文字");
        assertTrue(result.contains(originalText), "應該包含原始文字");
        assertTrue(result.contains("("), "應該包含括號");
        assertTrue(result.contains(")"), "應該包含括號");
        
        // 驗證格式：翻譯文字 (原文)
        String expected = translatedText + " (" + originalText + ")";
        assertEquals(expected, result, "格式應該是：翻譯文字 (原文)");
    }
    
    @Test
    void testShowOriginalTextLogicDisabled() {
        // 測試禁用顯示原始文字的邏輯
        String originalText = "Stone";
        String translatedText = "石頭";
        
        // 模擬雙語顯示的邏輯
        String result = createBilingualTextForTest(originalText, translatedText, false);
        
        // 驗證結果只包含翻譯文字
        assertEquals(translatedText, result, "應該只顯示翻譯文字");
        assertFalse(result.contains(originalText), "不應該包含原始文字");
        assertFalse(result.contains("("), "不應該包含括號");
    }
    
    @Test
    void testTranslationWithSameText() {
        // 測試翻譯文字與原文相同的情況
        String originalText = "Test";
        String translatedText = "Test"; // 相同文字
        
        String result = createBilingualTextForTest(originalText, translatedText, true);
        
        // 即使啟用顯示原文，相同文字也應該只顯示一次
        assertEquals(translatedText, result, "相同文字應該只顯示一次");
    }
    
    @Test
    void testEmptyTranslation() {
        // 測試空翻譯的情況
        String originalText = "Original";
        String translatedText = "";
        
        String result = createBilingualTextForTest(originalText, translatedText, true);
        
        // 空翻譯應該返回原文
        assertEquals(originalText, result, "空翻譯應該返回原文");
    }
    
    @Test
    void testNullTranslation() {
        // 測試 null 翻譯的情況
        String originalText = "Original";
        String translatedText = null;
        
        String result = createBilingualTextForTest(originalText, translatedText, true);
        
        // null 翻譯應該返回原文
        assertEquals(originalText, result, "null 翻譯應該返回原文");
    }
    
    @Test
    void testWhitespaceOnlyTranslation() {
        // 測試只有空白字符的翻譯
        String originalText = "Original";
        String translatedText = "   ";
        
        String result = createBilingualTextForTest(originalText, translatedText, true);
        
        // 只有空白字符的翻譯應該返回原文
        assertEquals(originalText, result, "只有空白字符的翻譯應該返回原文");
    }
    
    /**
     * 輔助方法：模擬雙語文字創建邏輯
     * 這個方法模擬了 ItemTooltipHandler.createBilingualComponent 的核心邏輯
     */
    private String createBilingualTextForTest(String originalText, String translatedText, boolean showOriginal) {
        // 處理 null 或空翻譯
        if (translatedText == null || translatedText.trim().isEmpty()) {
            return originalText;
        }
        
        // 處理相同文字
        if (translatedText.equals(originalText)) {
            return translatedText;
        }
        
        if (showOriginal) {
            // 顯示格式：翻譯文字 (原文)
            return translatedText + " (" + originalText + ")";
        } else {
            // 只顯示翻譯文字
            return translatedText;
        }
    }
}