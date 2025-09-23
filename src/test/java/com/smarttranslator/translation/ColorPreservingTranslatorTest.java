package com.smarttranslator.translation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 顏色保留翻譯器測試
 */
public class ColorPreservingTranslatorTest {
    
    @Test
    public void testPreprocessForTranslation() {
        // 測試預處理提取格式化代碼
        String input = "§cRed §lBold Text";
        ColorPreservingTranslator.TranslationResult result = 
            ColorPreservingTranslator.preprocessForTranslation(input);
        
        assertEquals("Red Bold Text", result.translatedText);
        assertEquals(2, result.formattingPositions.size());
        assertEquals("§c", result.formattingPositions.get(0).code);
        assertEquals("color", result.formattingPositions.get(0).type);
        assertEquals("§l", result.formattingPositions.get(1).code);
        assertEquals("format", result.formattingPositions.get(1).type);
    }
    
    @Test
    public void testSimpleColorPreservation() {
        // 測試簡單顏色代碼保留
        String original = "§cHello World";
        String translated = "你好世界";
        String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
        
        assertTrue(result.startsWith("§c"));
        assertTrue(result.contains("你好世界"));
    }
    
    @Test
    public void testMultipleColorCodes() {
        // 測試多個顏色代碼
        String original = "§cRed §aGreen §bBlue";
        String translated = "紅色 綠色 藍色";
        String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
        
        assertTrue(result.startsWith("§c"));
        assertTrue(result.contains("紅色 綠色 藍色"));
    }
    
    @Test
    public void testFormatCodes() {
        // 測試格式代碼（粗體、斜體等）
        String original = "§lBold §oItalic Text";
        String translated = "粗體 斜體 文字";
        String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
        
        // 由於§l是格式代碼而不是顏色代碼，主要顏色代碼查找會返回null
        // 但仍應該包含翻譯文本
        assertTrue(result.contains("粗體 斜體 文字"));
    }
    
    @Test
    public void testMixedColorAndFormat() {
        // 測試混合顏色和格式代碼
        String original = "§c§lRed Bold Text";
        String translated = "紅色粗體文字";
        String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
        
        assertTrue(result.startsWith("§c"));
        assertTrue(result.contains("紅色粗體文字"));
    }
    
    @Test
    public void testResetCode() {
        // 測試重置代碼
        String original = "§cRed Text§r Normal";
        String translated = "紅色文字 正常";
        String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
        
        assertTrue(result.startsWith("§c"));
        assertTrue(result.endsWith("§r"));
        assertTrue(result.contains("紅色文字 正常"));
    }
    
    @Test
    public void testNoColorCodes() {
        // 測試沒有顏色代碼的文本
        String original = "Normal Text";
        String translated = "正常文字";
        String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
        
        assertEquals("正常文字", result);
    }
    
    @Test
    public void testEmptyAndNull() {
        // 測試空值和null
        assertEquals("test", ColorPreservingTranslator.translateWithColorPreservation(null, "test"));
        assertEquals("", ColorPreservingTranslator.translateWithColorPreservation("", ""));
        assertEquals("test", ColorPreservingTranslator.translateWithColorPreservation("normal", "test"));
    }
    
    @Test
    public void testNeedsColorPreservation() {
        // 測試是否需要顏色保留處理
        assertTrue(ColorPreservingTranslator.needsColorPreservation("§cRed Text"));
        assertTrue(ColorPreservingTranslator.needsColorPreservation("§lBold Text"));
        assertFalse(ColorPreservingTranslator.needsColorPreservation("Normal Text"));
        assertFalse(ColorPreservingTranslator.needsColorPreservation(null));
    }
    
    @Test
    public void testAdvancedRestoreFormatting() {
        // 測試高級格式恢復
        String original = "§aGreen §bBlue §rReset";
        String translated = "綠色 藍色 重置";
        String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
        
        // 檢查是否包含翻譯文本
        assertTrue(result.contains("綠色 藍色 重置"));
        // 檢查是否保留了某些格式化代碼
        assertTrue(result.contains("§"));
    }
    
    @Test
    public void testSameTextPreservation() {
        // 測試相同文本的格式保留
        String original = "§cSame Text";
        ColorPreservingTranslator.TranslationResult preprocessed = 
            ColorPreservingTranslator.preprocessForTranslation(original);
        String translated = "Same Text"; // 與清理後的原文相同
        
        String result = ColorPreservingTranslator.postprocessTranslation(translated, preprocessed);
        assertEquals(original, result); // 應該返回完整的原文
    }
    
    @Test
    public void testComplexFormattingPattern() {
        // 測試複雜的格式化模式
        String original = "§0§1§2§3§4§5§6§7§8§9§a§b§c§d§e§f Rainbow Text";
        String translated = "彩虹文字";
        String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
        
        assertTrue(result.startsWith("§0")); // 應該保留第一個顏色代碼
        assertTrue(result.contains("彩虹文字"));
    }
}