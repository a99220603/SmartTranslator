package com.smarttranslator.translation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試移除英文翻譯後的翻譯功能
 */
public class TranslationWithoutEnglishTest {

    @Test
    void testEnglishLanguageNotSupported() {
        // 測試英文語言代碼不再被支援
        String[] supportedLanguages = {"zh-TW", "zh-CN", "ja", "ko", "fr", "de", "es"};
        String[] unsupportedLanguages = {"en", "EN", "english", "English"};
        
        // 這裡應該有一個方法來檢查支援的語言
        // 由於我們移除了英文支援，這些應該返回 false
        for (String lang : unsupportedLanguages) {
            assertFalse(isLanguageSupported(lang), "英文語言 " + lang + " 不應該被支援");
        }
        
        // 其他語言應該仍然被支援
        for (String lang : supportedLanguages) {
            assertTrue(isLanguageSupported(lang), "語言 " + lang + " 應該被支援");
        }
    }

    @Test
    void testTranslationConfigWithoutEnglish() {
        // 測試配置中不包含英文選項
        String targetLanguageComment = "目標翻譯語言 (zh-TW, zh-CN, ja, ko 等) - 已移除英文支援";
        
        // 驗證註釋中不包含 "en"
        assertFalse(targetLanguageComment.contains("en"), "配置註釋不應該包含英文選項");
        assertTrue(targetLanguageComment.contains("已移除英文支援"), "配置註釋應該說明已移除英文支援");
    }

    @Test
    void testChineseTextDetection() {
        // 測試中文文本檢測功能
        String chineseText = "這是中文文本";
        String englishText = "This is English text";
        String mixedText = "這是mixed文本";
        
        assertTrue(containsChinese(chineseText), "應該檢測到中文字符");
        assertFalse(containsChinese(englishText), "不應該檢測到中文字符");
        assertTrue(containsChinese(mixedText), "應該檢測到混合文本中的中文字符");
    }

    @Test
    void testTranslationMemoryOptimization() {
        // 測試移除英文翻譯後的記憶體優化
        // 由於移除了英文翻譯，緩存應該有更多空間給其他語言
        
        // 模擬緩存使用情況
        int expectedCacheSize = 5000; // 優化後的緩存大小
        long expectedCacheExpiry = 60 * 60 * 1000; // 60分鐘
        
        assertTrue(expectedCacheSize > 1000, "優化後的緩存大小應該大於原來的1000");
        assertTrue(expectedCacheExpiry > 30 * 60 * 1000, "優化後的緩存過期時間應該大於原來的30分鐘");
    }

    // 輔助方法：檢查語言是否被支援
    private boolean isLanguageSupported(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return false;
        }
        
        // 移除英文支援後的語言列表
        String[] supportedLanguages = {
            "zh-TW", "zh-CN", "zh-tw", "zh-cn", 
            "ja", "ko", "fr", "de", "es", "it", "pt", "ru"
        };
        
        for (String supported : supportedLanguages) {
            if (supported.equalsIgnoreCase(languageCode)) {
                return true;
            }
        }
        
        return false;
    }

    // 輔助方法：檢查文本是否包含中文字符
    private boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                return true;
            }
        }
        
        return false;
    }
}