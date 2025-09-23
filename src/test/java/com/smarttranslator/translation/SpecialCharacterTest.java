package com.smarttranslator.translation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 特殊字符翻譯測試
 */
public class SpecialCharacterTest {
    
    @Test
    public void testBasicFunctionality() {
        // 測試基本功能
        String input = "Hello World";
        String result = MinecraftTextProcessor.preprocessText(input);
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testFormattingCodeRemoval() {
        // 測試格式化代碼移除
        String input = "§cRed Text";
        String result = MinecraftTextProcessor.preprocessText(input);
        assertEquals("Red Text", result);
    }
    
    @Test
    public void testSpecialSymbolReplacement() {
        // 測試特殊符號替換 - 在 PRESERVE_CORE 模式下，核心符號應該被保留
        String input = "⚔ Sword";
        String result = MinecraftTextProcessor.preprocessText(input);
        // 在默認的 PRESERVE_CORE 模式下，核心符號 ⚔ 應該被保留
        assertEquals("⚔ Sword", result, "核心符號應該被保留");
        
        // 測試擴展符號會被替換
        MinecraftTextProcessor.setSymbolProcessingMode(MinecraftTextProcessor.SymbolProcessingMode.TRANSLATE_ALL);
        result = MinecraftTextProcessor.preprocessText(input);
        assertEquals("劍 Sword", result, "在 TRANSLATE_ALL 模式下符號應該被翻譯");
        
        // 恢復默認模式
        MinecraftTextProcessor.setSymbolProcessingMode(MinecraftTextProcessor.SymbolProcessingMode.PRESERVE_CORE);
    }
    
    @Test
    public void testNeedsProcessing() {
        // 測試是否需要特殊處理
        assertTrue(MinecraftTextProcessor.needsSpecialProcessing("§cRed"));
        assertTrue(MinecraftTextProcessor.needsSpecialProcessing("⚔"));
        assertFalse(MinecraftTextProcessor.needsSpecialProcessing("Normal"));
    }
}