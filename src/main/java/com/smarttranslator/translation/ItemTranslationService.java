package com.smarttranslator.translation;

import com.smarttranslator.config.SmartTranslatorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 物品翻譯服務類
 * 專門處理物品名稱和描述的翻譯邏輯
 */
public class ItemTranslationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemTranslationService.class);
    
    private final TranslationManager translationManager;
    
    // 不需要翻譯的物品名稱模式
    private static final Pattern SKIP_ITEM_PATTERN = Pattern.compile(
        "^[\\d\\s\\p{Punct}]*$|" +  // 只包含數字、空格、標點符號
        "^[a-zA-Z0-9\\s\\p{Punct}]{1,3}$|" +  // 短英文單詞
        "^\\[.*\\]$|" +  // 方括號內容
        "^<.*>$|" +  // 尖括號內容
        "^minecraft:|^forge:|^neoforge:"  // MOD ID 前綴
    );
    
    // 常見的不需要翻譯的關鍵詞
    private static final Set<String> SKIP_KEYWORDS = new HashSet<>();
    static {
        SKIP_KEYWORDS.add("Minecraft");
        SKIP_KEYWORDS.add("Forge");
        SKIP_KEYWORDS.add("NeoForge");
        SKIP_KEYWORDS.add("JEI");
        SKIP_KEYWORDS.add("REI");
        SKIP_KEYWORDS.add("EMI");
        SKIP_KEYWORDS.add("Waila");
        SKIP_KEYWORDS.add("HWYLA");
        SKIP_KEYWORDS.add("TOP");
    }
    
    public ItemTranslationService(TranslationManager translationManager) {
        this.translationManager = translationManager;
    }
    
    /**
     * 翻譯物品名稱
     */
    public CompletableFuture<String> translateItemName(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return CompletableFuture.completedFuture("");
        }
        
        String itemName = itemStack.getHoverName().getString();
        return translateText(itemName, true);
    }
    
    /**
     * 翻譯物品描述
     */
    public CompletableFuture<String> translateItemDescription(Component description) {
        if (description == null) {
            return CompletableFuture.completedFuture("");
        }
        
        String descriptionText = description.getString();
        return translateText(descriptionText, false);
    }
    
    /**
     * 翻譯文字
     */
    public CompletableFuture<String> translateText(String originalText, boolean isItemName) {
        if (!shouldTranslateText(originalText, isItemName)) {
            return CompletableFuture.completedFuture(originalText);
        }
        
        return translationManager.translateAsync(originalText);
    }
    
    /**
     * 判斷是否需要翻譯文字
     */
    public boolean shouldTranslateText(String text, boolean isItemName) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // 檢查是否啟用翻譯
        if (!SmartTranslatorConfig.ENABLED.get()) {
            return false;
        }
        
        // 檢查模式匹配
        if (SKIP_ITEM_PATTERN.matcher(text).matches()) {
            return false;
        }
        
        // 檢查關鍵詞
        for (String keyword : SKIP_KEYWORDS) {
            if (text.contains(keyword)) {
                return false;
            }
        }
        
        // 物品名稱的特殊檢查
        if (isItemName) {
            // 跳過已經是中文的物品名稱
            if (containsChinese(text)) {
                return false;
            }
            
            // 跳過純英文且長度小於等於 2 的單詞
            if (text.matches("^[a-zA-Z]{1,2}$")) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 檢查文字是否包含中文字符
     */
    private boolean containsChinese(String text) {
        return text.matches(".*[\\u4e00-\\u9fff].*");
    }
    
    /**
     * 格式化翻譯結果
     */
    public String formatTranslationResult(String originalText, String translatedText) {
        if (translatedText == null || translatedText.equals(originalText)) {
            return originalText;
        }
        
        if (SmartTranslatorConfig.SHOW_ORIGINAL_TEXT.get()) {
            return translatedText + " (" + originalText + ")";
        } else {
            return translatedText;
        }
    }
    
    /**
     * 檢查物品是否應該被翻譯
     */
    public boolean shouldTranslateItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        
        // 檢查配置
        if (!SmartTranslatorConfig.ENABLED.get()) {
            return false;
        }
        
        // 可以根據物品類型、MOD 來源等進行更細緻的控制
        String itemName = itemStack.getItem().toString();
        
        // 跳過原版 Minecraft 的某些物品（如果需要的話）
        if (itemName.startsWith("minecraft:") && !SmartTranslatorConfig.AUTO_TRANSLATE_ENABLED.get()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 獲取緩存的翻譯結果
     */
    public String getCachedTranslation(String originalText) {
        return translationManager.getCachedTranslation(originalText);
    }
    
    /**
     * 檢查是否有緩存的翻譯
     */
    public boolean hasCachedTranslation(String originalText) {
        return translationManager.hasCachedTranslation(originalText);
    }
}