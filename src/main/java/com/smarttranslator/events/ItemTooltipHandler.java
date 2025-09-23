package com.smarttranslator.events;

import com.smarttranslator.SmartTranslator;
import com.smarttranslator.config.SmartTranslatorConfig;
import com.smarttranslator.translation.TranslationManager;
import com.smarttranslator.events.KeyBindingHandler;
import com.smarttranslator.translation.ItemTranslationService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 高性能物品 Tooltip 翻譯事件處理器
 * 基於 NeoForge 1.21.4 的 ItemTooltipEvent 實現物品名稱和描述的翻譯功能
 * 
 * 性能優化特性：
 * - 非阻塞主線程設計
 * - 智能緩存預加載
 * - 高效的重複請求過濾
 * - 最小化日誌輸出
 * - 優化的線程池管理
 */
public class ItemTooltipHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemTooltipHandler.class);
    private ItemTranslationService itemTranslationService;
    
    // 性能優化配置
    private static final int THREAD_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
    private static final long CACHE_CHECK_INTERVAL = 50; // 50ms緩存檢查間隔
    private static final int MAX_CONCURRENT_TRANSLATIONS = 10; // 最大並發翻譯數
    
    // 高性能緩存和狀態管理
    private final Map<String, String> fastCache = new ConcurrentHashMap<>();
    private final Map<String, String> originalTextCache = new ConcurrentHashMap<>(); // 存儲原文
    private final Set<String> processingItems = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> recentlyProcessed = new ConcurrentHashMap<>(); // 最近處理過的項目
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    private final Semaphore translationSemaphore = new Semaphore(MAX_CONCURRENT_TRANSLATIONS);
    
    public ItemTooltipHandler() {
        // 延遲初始化，確保 SmartTranslator 實例已經創建
        // 啟動定期清理任務
        scheduler.scheduleAtFixedRate(this::cleanupExpiredData, 30, 30, TimeUnit.SECONDS);
    }
    
    private ItemTranslationService getItemTranslationService() {
        if (itemTranslationService == null) {
            itemTranslationService = new ItemTranslationService(
                SmartTranslator.getInstance().getTranslationManager()
            );
        }
        return itemTranslationService;
    }
    
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        try {
            ItemStack itemStack = event.getItemStack();
            List<Component> tooltip = event.getToolTip();
            
            if (itemStack.isEmpty() || tooltip.isEmpty()) {
                return;
            }
            
            // 檢查是否為書本物品
            boolean isBook = isBookItem(itemStack);
            
            // 如果是書本但書本翻譯功能被禁用，則跳過處理
            if (isBook && !SmartTranslatorConfig.TRANSLATE_BOOKS.get()) {
                return;
            }
            
            // 檢查是否按住R鍵顯示原文
            boolean showOriginalOnly = isShowOriginalKeyPressed();
            
            if (showOriginalOnly) {
                // 顯示原文，恢復未翻譯狀態
                processTooltipShowOriginal(tooltip);
            } else {
                // 根據物品類型選擇處理方式
                if (isBook) {
                    // 書本特殊處理
                    processBookTooltip(tooltip, itemStack);
                } else {
                    // 正常翻譯處理
                    processTooltipNonBlocking(tooltip, itemStack);
                }
            }
            
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("處理tooltip時發生錯誤", e);
            }
        }
    }
    
    /**
     * 檢查是否按住R鍵
     */
    private boolean isShowOriginalKeyPressed() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            
            // 確保在客戶端線程中執行
            if (!minecraft.isSameThread()) {
                return false;
            }
            
            // 檢查按鍵狀態，無論是否在GUI界面中
            return KeyBindingHandler.SHOW_ORIGINAL_TOOLTIP.get().isDown();
            
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("檢查R鍵狀態時發生錯誤", e);
            }
            return false;
        }
    }
    
    /**
     * 處理顯示原文的tooltip
     */
    private void processTooltipShowOriginal(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            Component component = tooltip.get(i);
            String currentText = component.getString();
            
            // 嘗試從原文緩存中獲取原文
            // 首先檢查當前文字是否就是翻譯文字（以翻譯文字為鍵查找原文）
            String originalText = originalTextCache.get(currentText);
            
            // 如果沒找到，可能當前文字包含格式化內容，嘗試解析
            if (originalText == null) {
                // 檢查是否是雙語格式 "翻譯文字 [原文]" 或 "翻譯文字  [原文]"
                if (currentText.contains(" [") && currentText.endsWith("]")) {
                    int bracketStart = currentText.lastIndexOf(" [");
                    if (bracketStart > 0) {
                        String translatedPart = currentText.substring(0, bracketStart).trim();
                        String originalPart = currentText.substring(bracketStart + 2, currentText.length() - 1);
                        
                        // 使用提取的原文
                        originalText = originalPart;
                        
                        // 同時更新緩存
                        originalTextCache.put(translatedPart, originalPart);
                    }
                } else {
                    // 嘗試反向查找：遍歷緩存找到以當前文字為值的條目
                    for (Map.Entry<String, String> entry : originalTextCache.entrySet()) {
                        if (entry.getValue().equals(currentText)) {
                            originalText = currentText; // 當前文字就是原文
                            break;
                        }
                    }
                }
            }
            
            if (originalText != null && !originalText.equals(currentText)) {
                // 恢復原文，保持原有樣式
                MutableComponent originalComponent = Component.literal(originalText);
                Style originalStyle = component.getStyle();
                if (originalStyle != null) {
                    originalComponent = originalComponent.withStyle(originalStyle);
                }
                tooltip.set(i, originalComponent);
            }
        }
    }

    /**
     * 調整過亮的顏色為較柔和的顏色
     * 將常見的亮色調整為較暗的版本，提供更好的視覺體驗
     */
    private net.minecraft.network.chat.Style adjustBrightColors(net.minecraft.network.chat.Style originalStyle) {
        if (originalStyle == null) {
            return null;
        }
        
        var color = originalStyle.getColor();
        if (color == null) {
            return originalStyle;
        }
        
        // 獲取顏色值
        int colorValue = color.getValue();
        
        // 調整常見的亮色
        int adjustedColor = switch (colorValue) {
            case 0xFFFF55 -> 0xCCCC44; // 亮黃色 -> 較暗的黃色
            case 0x55FFFF -> 0x44CCCC; // 亮青色 -> 較暗的青色  
            case 0x55FF55 -> 0x44CC44; // 亮綠色 -> 較暗的綠色
            case 0xFF5555 -> 0xCC4444; // 亮紅色 -> 較暗的紅色
            case 0xFF55FF -> 0xCC44CC; // 亮紫色 -> 較暗的紫色
            case 0x5555FF -> 0x4444CC; // 亮藍色 -> 較暗的藍色
            case 0xFFFFFF -> 0xE0E0E0; // 純白色 -> 淺灰色
            case 0xFFFF00 -> 0xCCCC00; // 純黃色 -> 較暗的黃色
            case 0x00FFFF -> 0x00CCCC; // 純青色 -> 較暗的青色
            case 0x00FF00 -> 0x00CC00; // 純綠色 -> 較暗的綠色
            case 0xFF0000 -> 0xCC0000; // 純紅色 -> 較暗的紅色
            case 0xFF00FF -> 0xCC00CC; // 純紫色 -> 較暗的紫色
            case 0x0000FF -> 0x0000CC; // 純藍色 -> 較暗的藍色
            default -> {
                // 對於其他顏色，檢查是否過亮並適當調暗
                int r = (colorValue >> 16) & 0xFF;
                int g = (colorValue >> 8) & 0xFF;
                int b = colorValue & 0xFF;
                
                // 計算亮度 (使用標準亮度公式)
                double brightness = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
                
                // 如果亮度超過 0.8，則調暗 20%
                if (brightness > 0.8) {
                    r = (int) (r * 0.8);
                    g = (int) (g * 0.8);
                    b = (int) (b * 0.8);
                    yield (r << 16) | (g << 8) | b;
                } else {
                    yield colorValue;
                }
            }
        };
        
        // 如果顏色有變化，創建新的樣式
        if (adjustedColor != colorValue) {
            return originalStyle.withColor(net.minecraft.network.chat.TextColor.fromRgb(adjustedColor));
        }
        
        return originalStyle;
    }

    /**
     * 高性能非阻塞tooltip處理（簡化版本）
     * 主要優化：
     * 1. 立即返回，不阻塞主線程
     * 2. 優先使用快速緩存
     * 3. 避免重複處理
     * 4. 智能批量處理
     */
    private void processTooltipNonBlocking(List<Component> tooltip, ItemStack itemStack) {
        // 第一階段：快速緩存檢查和應用（主線程，極快）
        for (int i = 0; i < tooltip.size(); i++) {
            Component component = tooltip.get(i);
            String originalText = component.getString();
            
            if (originalText == null || originalText.trim().isEmpty()) {
                continue;
            }
            
            // 檢查快速緩存
            String cachedTranslation = fastCache.get(originalText);
            if (cachedTranslation != null && !cachedTranslation.equals(originalText)) {
                // 立即應用緩存的翻譯
                MutableComponent translatedComponent = createBilingualComponent(originalText, cachedTranslation, component);
                tooltip.set(i, translatedComponent);
            } else if (!processingItems.contains(originalText)) {
                // 標記為需要異步處理
                scheduleAsyncTranslation(originalText, i == 0);
            }
        }
    }
    
    /**
     * 高性能異步翻譯調度
     * 優化特性：
     * 1. 信號量控制並發數
     * 2. 避免重複翻譯請求
     * 3. 智能緩存更新
     * 4. 最小化資源使用
     */
    private void scheduleAsyncTranslation(String originalText, boolean isItemName) {
        // 避免重複處理
        if (!processingItems.add(originalText)) {
            return;
        }
        
        // 異步執行翻譯，不阻塞主線程
        CompletableFuture.runAsync(() -> {
            try {
                // 控制並發翻譯數量
                if (translationSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    try {
                        performTranslation(originalText, isItemName);
                    } finally {
                        translationSemaphore.release();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                processingItems.remove(originalText);
            }
        }, scheduler);
    }
    
    /**
     * 執行實際翻譯操作
     */
    private void performTranslation(String originalText, boolean isItemName) {
        try {
            ItemTranslationService service = getItemTranslationService();
            
            // 檢查是否需要翻譯
            if (!service.shouldTranslateText(originalText, isItemName)) {
                return;
            }
            
            // 檢查服務緩存
            String cachedTranslation = service.getCachedTranslation(originalText);
            if (cachedTranslation != null) {
                // 更新快速緩存
                fastCache.put(originalText, cachedTranslation);
                return;
            }
            
            // 執行翻譯
            service.translateText(originalText, isItemName)
                .thenAccept(translatedText -> {
                    if (translatedText != null && !translatedText.equals(originalText)) {
                        // 更新快速緩存
                        fastCache.put(originalText, translatedText);
                        // 存儲原文到緩存，以翻譯文本為鍵，原文為值
                        originalTextCache.put(translatedText, originalText);
                        
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("翻譯完成並緩存: '{}' -> '{}'", originalText, translatedText);
                        }
                    }
                })
                .exceptionally(throwable -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("翻譯失敗: {}", originalText, throwable);
                    }
                    return null;
                });
                
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("翻譯過程中發生錯誤: {}", originalText, e);
            }
        }
    }
    
    /**
     * 簡單的翻譯邏輯（用於測試）
     */
    private String getSimpleTranslation(String originalText) {
        // 暫時使用簡單的映射來測試功能
        String translation = null;
        switch (originalText.toLowerCase()) {
            case "oak log":
                translation = "橡木原木";
                break;
            case "stone":
                translation = "石頭";
                break;
            case "dirt":
                translation = "泥土";
                break;
            case "grass block":
                translation = "草方塊";
                break;
            case "cobblestone":
                translation = "鵝卵石";
                break;
            case "wooden planks":
                translation = "木板";
                break;
            case "oak planks":
                translation = "橡木木板";
                break;
            default:
                // 如果沒有預設翻譯，嘗試使用翻譯服務
                try {
                    return getItemTranslationService().translateText(originalText, false)
                            .get(java.util.concurrent.TimeUnit.SECONDS.toMillis(1), 
                                 java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    LOGGER.warn("翻譯失敗: {}", originalText, e);
                    return originalText;
                }
        }
        
        // 如果有翻譯結果，存儲到原文緩存
        if (translation != null && !translation.equals(originalText)) {
            originalTextCache.put(translation, originalText);
        }
        
        return translation;
    }
    
    /**
     * 創建雙語顯示組件，格式為：繁體中文  [英文原文]
     * 繁體中文保持原始顏色，英文原文使用灰色以便區分
     * 在繁體中文和英文原文之間添加適當空格以提高可讀性
     * 所有文字均使用粗體顯示以提高視覺效果
     * 添加智能文字長度檢查和換行功能
     * 根據配置決定是否顯示原文
     */
    private MutableComponent createBilingualComponent(String originalText, String translatedText, Component originalComponent) {
        try {
            // 獲取原始組件的樣式，確保保留所有格式信息
            Style originalStyle = originalComponent.getStyle();
            
            // 檢查是否應該顯示原文
            boolean showOriginal = SmartTranslatorConfig.SHOW_ORIGINAL_TEXT.get();
            
            // 如果不顯示原文，直接返回翻譯文字
            if (!showOriginal) {
                return createTranslationOnlyComponent(translatedText, originalStyle);
            }
            
            // 計算文字顯示寬度（中文字符按2個單位計算，英文按1個單位）
            int translatedWidth = calculateDisplayWidth(translatedText);
            int originalWidth = calculateDisplayWidth(originalText);
            int separatorWidth = 4; // "  [" + "]" 的寬度
            int totalWidth = translatedWidth + separatorWidth + originalWidth;
            
            // 調整寬度限制以提高可讀性（大幅增加文字顯示空間）
            final int MAX_TOOLTIP_WIDTH = 50; // 大幅增加到50個顯示單位，提供更多空間
            final int MAX_SINGLE_LINE_WIDTH = 45; // 單行文字最大寬度大幅增加到45
            
            // 如果文字過長，使用簡化格式或換行
            if (totalWidth > MAX_TOOLTIP_WIDTH) {
                // 檢查是否可以通過縮短分隔符來解決
                int shortTotalWidth = translatedWidth + 3 + originalWidth; // " [" + "]"
                if (shortTotalWidth <= MAX_TOOLTIP_WIDTH) {
                    // 使用較短的分隔符
                    return createShortBilingualComponent(originalText, translatedText, originalStyle);
                } else if (translatedWidth <= MAX_SINGLE_LINE_WIDTH) {
                    // 如果翻譯文字本身不太長，將原文放到下一行
                    return createMultilineBilingualComponent(originalText, translatedText, originalStyle);
                } else {
                    // 如果翻譯文字也很長，只顯示翻譯文字
                    return createTranslationOnlyComponent(translatedText, originalStyle);
                }
            }
            
            // 文字長度適中，使用標準格式
            return createStandardBilingualComponent(originalText, translatedText, originalStyle);
        } catch (Exception e) {
            LOGGER.warn("創建雙語組件時發生錯誤: {}", e.getMessage());
            // 發生錯誤時返回原始組件，保持原始樣式並添加粗體
            if (originalComponent instanceof MutableComponent) {
                MutableComponent mutableComp = (MutableComponent) originalComponent;
                Style currentStyle = mutableComp.getStyle();
                if (currentStyle != null && !currentStyle.isEmpty()) {
                    return mutableComp.withStyle(currentStyle.withBold(true));
                } else {
                    return mutableComp.withStyle(ChatFormatting.BOLD);
                }
            } else {
                Style style = originalComponent.getStyle();
                MutableComponent fallback = Component.literal(originalText);
                if (style != null && !style.isEmpty()) {
                    return fallback.withStyle(style.withBold(true));
                } else {
                    return fallback.withStyle(ChatFormatting.BOLD);
                }
            }
        }
    }
    
    /**
     * 計算文字的顯示寬度（中文字符按2個單位計算，英文按1個單位）
     */
    private int calculateDisplayWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int width = 0;
        for (char c : text.toCharArray()) {
            // 判斷是否為中文字符（包括繁體中文、簡體中文、日文漢字等）
            if (isCJKCharacter(c)) {
                width += 2; // 中文字符佔2個顯示單位
            } else {
                width += 1; // 英文字符佔1個顯示單位
            }
        }
        return width;
    }
    
    /**
     * 判斷字符是否為CJK（中日韓）字符
     */
    private boolean isCJKCharacter(char c) {
        // Unicode範圍包含中文、日文漢字、韓文等
        return (c >= 0x4E00 && c <= 0x9FFF) ||    // CJK統一漢字
               (c >= 0x3400 && c <= 0x4DBF) ||    // CJK擴展A
               (c >= 0x20000 && c <= 0x2A6DF) ||  // CJK擴展B
               (c >= 0x2A700 && c <= 0x2B73F) ||  // CJK擴展C
               (c >= 0x2B740 && c <= 0x2B81F) ||  // CJK擴展D
               (c >= 0x2B820 && c <= 0x2CEAF) ||  // CJK擴展E
               (c >= 0x3000 && c <= 0x303F) ||    // CJK符號和標點
               (c >= 0xFF00 && c <= 0xFFEF);      // 全角字符
    }
    
    /**
     * 創建標準格式的雙語組件：繁體中文  [英文原文]
     */
    private MutableComponent createStandardBilingualComponent(String originalText, String translatedText, Style originalStyle) {
        // 創建翻譯文字部分，保持原有樣式但不添加額外格式
        MutableComponent result = Component.literal(translatedText);
        if (originalStyle != null && !originalStyle.isEmpty()) {
            // 保持原有樣式，但移除可能造成混亂的格式
            Style cleanStyle = Style.EMPTY
                .withColor(originalStyle.getColor())
                .withBold(false); // 移除粗體避免重疊
            result = result.withStyle(cleanStyle);
        }
        
        // 添加英文對照，使用簡潔的深灰色
        result.append(Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY));
        result.append(Component.literal(originalText).withStyle(ChatFormatting.DARK_GRAY));
        result.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
        
        return result;
    }
    
    /**
     * 創建短分隔符格式的雙語組件：繁體中文 [英文原文]
     */
    private MutableComponent createShortBilingualComponent(String originalText, String translatedText, Style originalStyle) {
        // 創建翻譯文字部分，保持原有樣式但不添加額外格式
        MutableComponent result = Component.literal(translatedText);
        if (originalStyle != null && !originalStyle.isEmpty()) {
            // 保持原有樣式，但移除可能造成混亂的格式
            Style cleanStyle = Style.EMPTY
                .withColor(originalStyle.getColor())
                .withBold(false); // 移除粗體避免重疊
            result = result.withStyle(cleanStyle);
        }
        
        // 添加英文對照，使用簡潔的深灰色和短分隔符
        result.append(Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY));
        result.append(Component.literal(originalText).withStyle(ChatFormatting.DARK_GRAY));
        result.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
        
        return result;
    }
    
    /**
     * 創建多行格式的雙語組件：
     * 繁體中文
     * [英文原文]
     */
    private MutableComponent createMultilineBilingualComponent(String originalText, String translatedText, Style originalStyle) {
        // 創建翻譯文字部分，保持原有樣式但不添加額外格式
        MutableComponent result = Component.literal(translatedText);
        if (originalStyle != null && !originalStyle.isEmpty()) {
            // 保持原有樣式，但移除可能造成混亂的格式
            Style cleanStyle = Style.EMPTY
                .withColor(originalStyle.getColor())
                .withBold(false); // 移除粗體避免重疊
            result = result.withStyle(cleanStyle);
        }
        
        // 檢查原文長度，如果太長則截斷
        String displayOriginalText = originalText;
        if (calculateDisplayWidth(originalText) > 40) { // 原文行最大40個顯示單位，大幅增加可讀性
            displayOriginalText = truncateText(originalText, 37) + "..."; // 大幅增加截斷長度到37
        }
        
        // 添加換行符和原文（使用統一的深灰色樣式）
        result.append(Component.literal("\n[").withStyle(ChatFormatting.DARK_GRAY));
        result.append(Component.literal(displayOriginalText).withStyle(ChatFormatting.DARK_GRAY));
        result.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
        
        return result;
    }
    
    /**
     * 截斷文字到指定的顯示寬度
     */
    private String truncateText(String text, int maxDisplayWidth) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        int currentWidth = 0;
        
        for (char c : text.toCharArray()) {
            int charWidth = isCJKCharacter(c) ? 2 : 1;
            if (currentWidth + charWidth > maxDisplayWidth) {
                break;
            }
            result.append(c);
            currentWidth += charWidth;
        }
        
        return result.toString();
    }
    
    /**
     * 創建僅翻譯文字的組件（當文字過長時使用）
     */
    private MutableComponent createTranslationOnlyComponent(String translatedText, Style originalStyle) {
        // 檢查翻譯文字長度，如果太長則截斷
        String displayTranslatedText = translatedText;
        if (calculateDisplayWidth(translatedText) > 45) { // 單行最大45個顯示單位，大幅增加可讀性
            displayTranslatedText = truncateText(translatedText, 42) + "..."; // 大幅增加截斷長度到42
        }
        
        MutableComponent result = Component.literal(displayTranslatedText);
        if (originalStyle != null && !originalStyle.isEmpty()) {
            // 保持原有樣式，但移除可能造成混亂的格式
            Style cleanStyle = Style.EMPTY
                .withColor(originalStyle.getColor())
                .withBold(false); // 移除粗體避免重疊
            result = result.withStyle(cleanStyle);
        }
        
        return result;
    }
    
    /**
     * 檢查物品是否為書本類型
     */
    private boolean isBookItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        
        // 檢查是否為書本或寫過的書本
        return itemStack.getItem() instanceof WrittenBookItem ||
               itemStack.is(Items.BOOK) ||
               itemStack.is(Items.WRITABLE_BOOK) ||
               itemStack.is(Items.WRITTEN_BOOK);
    }
    
    /**
     * 處理書本tooltip的特殊邏輯
     */
    private void processBookTooltip(List<Component> tooltip, ItemStack itemStack) {
        try {
            // 對於書本，我們需要特別處理內容翻譯
            for (int i = 0; i < tooltip.size(); i++) {
                Component component = tooltip.get(i);
                String text = component.getString();
                
                // 跳過空文本或過短的文本
                if (text == null || text.trim().isEmpty() || text.length() < 2) {
                    continue;
                }
                
                // 檢查是否已經翻譯過（包含[翻譯]標記）
                if (text.contains("[翻譯]")) {
                    continue;
                }
                
                // 檢查快速緩存
                String cacheKey = "book_" + text.hashCode();
                String cachedTranslation = fastCache.get(cacheKey);
                
                if (cachedTranslation != null) {
                    // 使用緩存的翻譯
                    MutableComponent translatedComponent = createBilingualComponent(text, cachedTranslation, component);
                    tooltip.set(i, translatedComponent);
                } else {
                    // 異步翻譯書本內容
                    scheduleBookTranslation(tooltip, i, component, text, cacheKey);
                }
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("處理書本tooltip時發生錯誤", e);
            }
        }
    }
    
    /**
     * 調度書本內容的異步翻譯
     */
    private void scheduleBookTranslation(List<Component> tooltip, int index, Component component, String text, String cacheKey) {
        // 檢查是否已在處理中
        if (processingItems.contains(cacheKey)) {
            return;
        }
        
        processingItems.add(cacheKey);
        
        scheduler.execute(() -> {
            try {
                // 執行翻譯
                String translatedText = performBookTranslation(text);
                
                if (translatedText != null && !translatedText.equals(text)) {
                    // 更新緩存
                    fastCache.put(cacheKey, translatedText);
                    originalTextCache.put(cacheKey, text);
                    
                    // 在主線程中更新tooltip
                    Minecraft.getInstance().execute(() -> {
                        try {
                            if (index < tooltip.size()) {
                                MutableComponent translatedComponent = createBilingualComponent(text, translatedText, component);
                                tooltip.set(index, translatedComponent);
                            }
                        } catch (Exception e) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("更新書本tooltip時發生錯誤", e);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("翻譯書本內容時發生錯誤", e);
                }
            } finally {
                processingItems.remove(cacheKey);
            }
        });
    }
    
    /**
     * 執行書本內容的翻譯
     */
    private String performBookTranslation(String text) {
        try {
            // 使用TranslationManager進行翻譯
            TranslationManager translationManager = SmartTranslator.getInstance().getTranslationManager();
            if (translationManager != null) {
                return translationManager.translate(text);
            }
            
            // 如果TranslationManager不可用，使用簡單翻譯邏輯
            return getSimpleTranslation(text);
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("執行書本翻譯時發生錯誤", e);
            }
            return null;
        }
    }
    private void cleanupExpiredData() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // 清理最近處理記錄（超過5秒的記錄）
            recentlyProcessed.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > 5000);
            
            // 清理快速緩存（保留最近1000個條目）
            if (fastCache.size() > 1000) {
                // 簡單的LRU清理：移除一半條目
                Iterator<String> iterator = fastCache.keySet().iterator();
                int toRemove = fastCache.size() / 2;
                while (iterator.hasNext() && toRemove > 0) {
                    iterator.next();
                    iterator.remove();
                    toRemove--;
                }
            }
            
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("清理緩存時發生錯誤", e);
            }
        }
    }
    
    /**
     * 移除舊的方法，已被新的高性能系統替代
     */
    // scheduleTranslation, startAsyncTranslation, getCachedTranslation 方法已被新系統替代
    
    /**
     * 關閉資源，防止內存洩漏（優化版本）
     */
    public void shutdown() {
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            }
            
            // 清理所有緩存
            fastCache.clear();
            processingItems.clear();
            recentlyProcessed.clear();
            
        } catch (Exception e) {
            LOGGER.error("關閉ItemTooltipHandler時發生錯誤", e);
        }
    }
}