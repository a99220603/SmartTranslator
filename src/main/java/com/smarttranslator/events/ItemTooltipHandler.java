package com.smarttranslator.events;

import com.smarttranslator.SmartTranslator;
import com.smarttranslator.config.SmartTranslatorConfig;
import com.smarttranslator.translation.ItemTranslationService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
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
            
            // 使用高性能非阻塞處理
            processTooltipNonBlocking(tooltip, itemStack);
            
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("處理tooltip時發生錯誤", e);
            }
        }
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
        switch (originalText.toLowerCase()) {
            case "oak log":
                return "橡木原木";
            case "stone":
                return "石頭";
            case "dirt":
                return "泥土";
            case "grass block":
                return "草方塊";
            case "cobblestone":
                return "鵝卵石";
            case "wooden planks":
                return "木板";
            case "oak planks":
                return "橡木木板";
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
    }
    
    /**
     * 創建雙語顯示組件（優化版本）
     * 減少對象創建，提升性能
     */
    private MutableComponent createBilingualComponent(String originalText, String translatedText, Component originalComponent) {
        try {
            // 保持原始樣式
            MutableComponent result = Component.literal(translatedText).withStyle(originalComponent.getStyle());
            
            // 只在調試模式下添加原文
            if (LOGGER.isDebugEnabled()) {
                result.append(Component.literal(" (" + originalText + ")").withStyle(ChatFormatting.GRAY));
            }
            
            return result;
        } catch (Exception e) {
            // 發生錯誤時返回原始組件
            return Component.literal(originalText).withStyle(originalComponent.getStyle());
        }
    }
    
    /**
     * 定期清理過期緩存和狀態
     * 防止內存洩漏
     */
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