package com.smarttranslator.translation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Minecraft 顏色代碼映射表測試
 */
public class MinecraftColorCodesTest {
    
    @Test
    public void testColorCodeValidation() {
        // 測試顏色代碼驗證
        assertTrue(MinecraftColorCodes.isColorCode("§0"));
        assertTrue(MinecraftColorCodes.isColorCode("§f"));
        assertTrue(MinecraftColorCodes.isColorCode("§a"));
        assertFalse(MinecraftColorCodes.isColorCode("§g"));
        assertFalse(MinecraftColorCodes.isColorCode("§z"));
    }
    
    @Test
    public void testFormatCodeValidation() {
        // 測試格式代碼驗證
        assertTrue(MinecraftColorCodes.isFormatCode("§l"));
        assertTrue(MinecraftColorCodes.isFormatCode("§o"));
        assertTrue(MinecraftColorCodes.isFormatCode("§r"));
        assertFalse(MinecraftColorCodes.isFormatCode("§0"));
        assertFalse(MinecraftColorCodes.isFormatCode("§g"));
    }
    
    @Test
    public void testValidFormattingCode() {
        // 測試有效格式化代碼檢查
        assertTrue(MinecraftColorCodes.isValidFormattingCode("§c"));
        assertTrue(MinecraftColorCodes.isValidFormattingCode("§l"));
        assertTrue(MinecraftColorCodes.isValidFormattingCode("§r"));
        assertFalse(MinecraftColorCodes.isValidFormattingCode("§g"));
        assertFalse(MinecraftColorCodes.isValidFormattingCode("§z"));
    }
    
    @Test
    public void testColorNames() {
        // 測試顏色名稱獲取
        assertEquals("紅色", MinecraftColorCodes.getColorName("§c"));
        assertEquals("藍色", MinecraftColorCodes.getColorName("§9"));
        assertEquals("白色", MinecraftColorCodes.getColorName("§f"));
        assertEquals("黑色", MinecraftColorCodes.getColorName("§0"));
        assertNull(MinecraftColorCodes.getColorName("§g"));
    }
    
    @Test
    public void testFormatNames() {
        // 測試格式名稱獲取
        assertEquals("粗體", MinecraftColorCodes.getFormatName("§l"));
        assertEquals("斜體", MinecraftColorCodes.getFormatName("§o"));
        assertEquals("重置", MinecraftColorCodes.getFormatName("§r"));
        assertEquals("刪除線", MinecraftColorCodes.getFormatName("§m"));
        assertNull(MinecraftColorCodes.getFormatName("§c"));
    }
    
    @Test
    public void testFormattingDescription() {
        // 測試格式化描述獲取
        assertEquals("紅色", MinecraftColorCodes.getFormattingDescription("§c"));
        assertEquals("粗體", MinecraftColorCodes.getFormattingDescription("§l"));
        assertEquals("未知格式代碼", MinecraftColorCodes.getFormattingDescription("§g"));
    }
    
    @Test
    public void testCaseInsensitive() {
        // 測試大小寫不敏感
        assertTrue(MinecraftColorCodes.isColorCode("§C"));
        assertTrue(MinecraftColorCodes.isColorCode("§A"));
        assertTrue(MinecraftColorCodes.isFormatCode("§L"));
        assertTrue(MinecraftColorCodes.isFormatCode("§R"));
        assertEquals("紅色", MinecraftColorCodes.getColorName("§C"));
        assertEquals("粗體", MinecraftColorCodes.getFormatName("§L"));
    }
    
    @Test
    public void testAmpersandConversion() {
        // 測試 & 符號轉換
        String input = "&cRed &lBold &rReset";
        String expected = "§cRed §lBold §rReset";
        String result = MinecraftColorCodes.convertAmpersandCodes(input);
        assertEquals(expected, result);
    }
    
    @Test
    public void testSectionToAmpersandConversion() {
        // 測試 § 符號轉換為 &
        String input = "§cRed §lBold §rReset";
        String expected = "&cRed &lBold &rReset";
        String result = MinecraftColorCodes.convertToAmpersandCodes(input);
        assertEquals(expected, result);
    }
    
    @Test
    public void testPatternMatching() {
        // 測試正則表達式模式匹配
        String text = "§cRed §lBold §9Blue Text";
        
        assertTrue(MinecraftColorCodes.ALL_FORMATTING_PATTERN.matcher(text).find());
        assertTrue(MinecraftColorCodes.COLOR_ONLY_PATTERN.matcher(text).find());
        assertTrue(MinecraftColorCodes.FORMAT_ONLY_PATTERN.matcher(text).find());
        
        String noFormatText = "Normal Text";
        assertFalse(MinecraftColorCodes.ALL_FORMATTING_PATTERN.matcher(noFormatText).find());
    }
    
    @Test
    public void testAllColorCodes() {
        // 測試所有顏色代碼都有對應的中文名稱
        String[] colorCodes = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", 
                              "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"};
        
        for (String code : colorCodes) {
            assertTrue(MinecraftColorCodes.isColorCode(code));
            assertNotNull(MinecraftColorCodes.getColorName(code));
            assertFalse(MinecraftColorCodes.getColorName(code).isEmpty());
        }
    }
    
    @Test
    public void testAllFormatCodes() {
        // 測試所有格式代碼都有對應的中文名稱
        String[] formatCodes = {"§k", "§l", "§m", "§n", "§o", "§r"};
        
        for (String code : formatCodes) {
            assertTrue(MinecraftColorCodes.isFormatCode(code));
            assertNotNull(MinecraftColorCodes.getFormatName(code));
            assertFalse(MinecraftColorCodes.getFormatName(code).isEmpty());
        }
    }
    
    @Test
    public void testNullAndEmptyInput() {
        // 測試null和空輸入
        assertNull(MinecraftColorCodes.convertAmpersandCodes(null));
        assertNull(MinecraftColorCodes.convertToAmpersandCodes(null));
        assertEquals("", MinecraftColorCodes.convertAmpersandCodes(""));
        assertEquals("", MinecraftColorCodes.convertToAmpersandCodes(""));
    }
}