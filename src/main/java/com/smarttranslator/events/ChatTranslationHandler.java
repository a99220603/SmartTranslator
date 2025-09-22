package com.smarttranslator.events;

import com.smarttranslator.SmartTranslator;
import com.smarttranslator.config.SmartTranslatorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 聊天翻譯事件處理器
 */
public class ChatTranslationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatTranslationHandler.class);
    
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        // 檢查是否啟用聊天翻譯
        if (!SmartTranslatorConfig.TRANSLATE_CHAT.get()) {
            return;
        }
        
        // 獲取聊天訊息
        Component message = event.getMessage();
        String originalText = message.getString();
        
        // 過濾系統訊息和命令
        if (originalText.startsWith("/") || originalText.startsWith("[") || originalText.isEmpty()) {
            return;
        }
        
        // 異步翻譯
        SmartTranslator.getInstance().getTranslationManager()
                .translateAsync(originalText)
                .thenAccept(translatedText -> {
                    if (!translatedText.equals(originalText)) {
                        // 在主線程中更新聊天訊息
                        Minecraft.getInstance().execute(() -> {
                            Component translatedComponent = Component.literal(translatedText);
                            event.setMessage(translatedComponent);
                        });
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.error("翻譯聊天訊息失敗", throwable);
                    return null;
                });
    }
}