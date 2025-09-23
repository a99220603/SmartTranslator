package com.smarttranslator.symbols;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 專門測試 [FORMAT] 占位符問題的調試測試
 */
public class FormatPlaceholderDebugTest {
    
    @Test
    public void testFormatPlaceholderProcessing() {
        System.out.println("=== [FORMAT] Placeholder Debug Test ===\n");
        
        // 測試案例 1: 簡單格式化代碼
        testFormatPlaceholder("§cRed Text", "Red Text");
        
        // 測試案例 2: 多個格式化代碼
        testFormatPlaceholder("§4§lBold Red", "Bold Red");
        
        // 測試案例 3: 混合格式和文字
        testFormatPlaceholder("§6Gold §rNormal Text", "Gold Normal Text");
        
        // 測試案例 4: 複雜格式
        testFormatPlaceholder("§a§lGreen Bold §r§cRed", "Green Bold Red");
        
        // 測試案例 5: 格式重置
        testFormatPlaceholder("§lBold §rReset", "Bold Reset");
    }
    
    private void testFormatPlaceholder(String original, String mockTranslation) {
        System.out.println("Original text: " + original);
        
        // 步驟 1: 準備翻譯（提取符號）
        WynncraftSymbolHandler.TextWithSymbols prepared = 
            WynncraftSymbolHandler.prepareForTranslation(original);
        
        System.out.println("Clean text: \"" + prepared.cleanText + "\"");
        System.out.println("Extracted symbols: " + prepared.symbols.size());
        
        // 檢查是否包含 [FORMAT] 占位符
        boolean hasFormatPlaceholder = prepared.cleanText.contains("[FORMAT]");
        System.out.println("Contains [FORMAT] placeholder: " + hasFormatPlaceholder);
        
        // 步驟 2: 模擬翻譯結果
        String translatedText = mockTranslation;
        System.out.println("Translation result: \"" + translatedText + "\"");
        
        // 步驟 3: 恢復符號
        String restored = WynncraftSymbolHandler.restoreSymbols(translatedText, prepared);
        System.out.println("Restored text: " + restored);
        
        // 檢查是否還有未替換的占位符
        boolean hasUnreplacedPlaceholder = restored.contains("[FORMAT]");
        System.out.println("Still has unreplaced [FORMAT]: " + hasUnreplacedPlaceholder);
        
        if (hasUnreplacedPlaceholder) {
            System.out.println("X Problem found: [FORMAT] placeholder not replaced correctly!");
            // 這裡不使用 fail() 以便看到所有測試結果
        } else {
            System.out.println("V Format code processing normal");
        }
        
        System.out.println("-".repeat(60));
    }
}