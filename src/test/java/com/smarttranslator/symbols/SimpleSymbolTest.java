package com.smarttranslator.symbols;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 簡單符號測試
 */
public class SimpleSymbolTest {
    
    @Test
    public void testBasicSymbolHandling() {
        // 使用基本的翡翠符號測試
        String testText = "獲得 \u00B2 翡翠"; // 使用 Unicode 轉義序列
        System.out.println("原始文本: " + testText);
        System.out.println("字符詳情:");
        for (int i = 0; i < testText.length(); i++) {
            char c = testText.charAt(i);
            System.out.println("  位置 " + i + ": '" + c + "' (U+" + String.format("%04X", (int)c) + ")");
        }
        
        // 檢測符號
        boolean hasSymbols = WynncraftSymbolHandler.containsWynncraftSymbols(testText);
        System.out.println("包含特殊符號: " + hasSymbols);
        
        // 準備翻譯
        WynncraftSymbolHandler.TextWithSymbols prepared = WynncraftSymbolHandler.prepareForTranslation(testText);
        System.out.println("清理後文本: '" + prepared.cleanText + "'");
        System.out.println("符號數量: " + prepared.symbols.size());
        
        for (WynncraftSymbolHandler.SymbolInfo symbol : prepared.symbols) {
            System.out.println("符號詳情: " + symbol);
            System.out.println("  符號字符: '" + symbol.symbol + "' (U+" + String.format("%04X", (int)symbol.symbol.charAt(0)) + ")");
        }
        
        // 驗證結果
        assertTrue(hasSymbols, "應該檢測到特殊符號");
        assertEquals(1, prepared.symbols.size(), "應該有一個符號");
        
        // 測試恢復
        String mockTranslation = "Get  E  Emerald";
        String restored = WynncraftSymbolHandler.restoreSymbols(mockTranslation, prepared);
        System.out.println("恢復後文本: '" + restored + "'");
        
        assertTrue(restored.contains("\u00B2"), "應該包含原始翡翠符號");
    }
}