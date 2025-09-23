package com.smarttranslator.translation;

import com.smarttranslator.symbols.WynncraftSymbolHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 翻譯流程調試測試
 * 專門追蹤中文 [格式] 占位符的產生原因
 */
public class TranslationFlowDebugTest {
    
    @Test
    public void testCompleteTranslationFlow() {
        System.out.println("=== 完整翻譯流程調試測試 ===\n");
        
        // 測試案例：包含格式化代碼的文本
        String originalText = "§c紅色文字 §a綠色文字";
        System.out.println("原始文本: " + originalText);
        
        // 步驟 1: MinecraftTextProcessor 預處理
        System.out.println("\n--- 步驟 1: MinecraftTextProcessor 預處理 ---");
        String preprocessed = MinecraftTextProcessor.preprocessText(originalText);
        System.out.println("MinecraftTextProcessor 預處理結果: \"" + preprocessed + "\"");
        
        // 步驟 2: WynncraftSymbolHandler 預處理
        System.out.println("\n--- 步驟 2: WynncraftSymbolHandler 預處理 ---");
        WynncraftSymbolHandler.TextWithSymbols wynncraftPrepared = 
            WynncraftSymbolHandler.prepareForTranslation(originalText);
        System.out.println("WynncraftSymbolHandler 清理文本: \"" + wynncraftPrepared.cleanText + "\"");
        System.out.println("WynncraftSymbolHandler 提取符號數量: " + wynncraftPrepared.symbols.size());
        
        // 檢查是否包含 [FORMAT] 占位符
        boolean hasFormatPlaceholder = wynncraftPrepared.cleanText.contains("[FORMAT]");
        System.out.println("包含 [FORMAT] 占位符: " + hasFormatPlaceholder);
        
        // 步驟 3: ColorPreservingTranslator 預處理
        System.out.println("\n--- 步驟 3: ColorPreservingTranslator 預處理 ---");
        ColorPreservingTranslator.TranslationResult colorPreserved = 
            ColorPreservingTranslator.preprocessForTranslation(originalText);
        System.out.println("ColorPreservingTranslator 清理文本: \"" + colorPreserved.translatedText + "\"");
        System.out.println("ColorPreservingTranslator 格式位置數量: " + colorPreserved.formattingPositions.size());
        
        // 步驟 4: 模擬翻譯過程
        System.out.println("\n--- 步驟 4: 模擬翻譯過程 ---");
        String mockTranslation = "Red text Green text";
        System.out.println("模擬翻譯結果: \"" + mockTranslation + "\"");
        
        // 步驟 5: MinecraftTextProcessor 後處理
        System.out.println("\n--- 步驟 5: MinecraftTextProcessor 後處理 ---");
        String postprocessed = MinecraftTextProcessor.postprocessText(mockTranslation, originalText);
        System.out.println("MinecraftTextProcessor 後處理結果: \"" + postprocessed + "\"");
        
        // 步驟 6: WynncraftSymbolHandler 恢復
        System.out.println("\n--- 步驟 6: WynncraftSymbolHandler 恢復 ---");
        String wynncraftRestored = WynncraftSymbolHandler.restoreSymbols(mockTranslation, wynncraftPrepared);
        System.out.println("WynncraftSymbolHandler 恢復結果: \"" + wynncraftRestored + "\"");
        
        // 步驟 7: ColorPreservingTranslator 恢復
        System.out.println("\n--- 步驟 7: ColorPreservingTranslator 恢復 ---");
        String colorRestored = ColorPreservingTranslator.postprocessTranslation(mockTranslation, colorPreserved);
        System.out.println("ColorPreservingTranslator 恢復結果: \"" + colorRestored + "\"");
        
        // 檢查最終結果中是否有未替換的占位符
        System.out.println("\n--- 最終檢查 ---");
        checkForUnreplacedPlaceholders("MinecraftTextProcessor", postprocessed);
        checkForUnreplacedPlaceholders("WynncraftSymbolHandler", wynncraftRestored);
        checkForUnreplacedPlaceholders("ColorPreservingTranslator", colorRestored);
        
        System.out.println("\n=== 測試完成 ===");
    }
    
    @Test
    public void testTranslationManagerFlow() {
        System.out.println("=== TranslationManager 流程測試 ===\n");
        
        String originalText = "§c錯誤: §f找不到文件";
        System.out.println("原始文本: " + originalText);
        
        // 模擬 TranslationManager 的處理流程
        System.out.println("\n--- TranslationManager 預處理 ---");
        String processedText = MinecraftTextProcessor.preprocessText(originalText);
        System.out.println("預處理結果: \"" + processedText + "\"");
        
        // 模擬翻譯 API 返回結果
        String translatedText = "Error: File not found";
        System.out.println("翻譯結果: \"" + translatedText + "\"");
        
        // 後處理
        System.out.println("\n--- TranslationManager 後處理 ---");
        String postProcessed = MinecraftTextProcessor.postprocessText(translatedText, originalText);
        System.out.println("後處理結果: \"" + postProcessed + "\"");
        
        // 檢查是否有中文占位符
        checkForChinesePlaceholders(postProcessed);
        
        System.out.println("\n=== TranslationManager 測試完成 ===");
    }
    
    @Test
    public void testChinesePlaceholderGeneration() {
        System.out.println("=== 中文占位符生成測試 ===\n");
        
        // 測試各種可能產生中文占位符的場景
        String[] testCases = {
            "§c[FORMAT]紅色",
            "[FORMAT] 文字",
            "文字 [FORMAT]",
            "§a綠色 [FORMAT] 文字",
            "[格式] 中文占位符"
        };
        
        for (String testCase : testCases) {
            System.out.println("測試案例: \"" + testCase + "\"");
            
            // WynncraftSymbolHandler 處理
            WynncraftSymbolHandler.TextWithSymbols prepared = 
                WynncraftSymbolHandler.prepareForTranslation(testCase);
            System.out.println("  清理後: \"" + prepared.cleanText + "\"");
            
            // 模擬翻譯 - 不翻譯FORMAT占位符
            String mockTranslation = prepared.cleanText.replace("Red", "紅色")
                                                      .replace("Green", "綠色")
                                                      .replace("text", "文字");
            System.out.println("  模擬翻譯: \"" + mockTranslation + "\"");
            
            // 恢復符號
            String restored = WynncraftSymbolHandler.restoreSymbols(mockTranslation, prepared);
            System.out.println("  恢復結果: \"" + restored + "\"");
            
            // 檢查中文占位符
            if (restored.contains("[格式]")) {
                System.out.println("  ❌ 發現中文占位符 [格式]！");
            } else {
                System.out.println("  ✅ 無中文占位符");
            }
            
            System.out.println();
        }
        
        System.out.println("=== 中文占位符測試完成 ===");
    }
    
    private void checkForUnreplacedPlaceholders(String processorName, String text) {
        boolean hasFormat = text.contains("[FORMAT]");
        boolean hasChineseFormat = text.contains("[格式]");
        
        System.out.println(processorName + " 結果檢查:");
        System.out.println("  包含 [FORMAT]: " + hasFormat);
        System.out.println("  包含 [格式]: " + hasChineseFormat);
        
        if (hasFormat || hasChineseFormat) {
            System.out.println("  ❌ 發現未替換的占位符！");
        } else {
            System.out.println("  ✅ 無未替換占位符");
        }
    }
    
    private void checkForChinesePlaceholders(String text) {
        boolean hasChineseFormat = text.contains("[格式]");
        System.out.println("中文占位符檢查: " + (hasChineseFormat ? "❌ 發現 [格式]" : "✅ 無中文占位符"));
    }
}