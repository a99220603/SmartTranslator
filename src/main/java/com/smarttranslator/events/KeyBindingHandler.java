package com.smarttranslator.events;

import com.smarttranslator.config.SmartTranslatorConfig;
import com.smarttranslator.gui.TranslationManagerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

/**
 * 按鍵綁定事件處理器
 */
public class KeyBindingHandler {
    
    // 按鍵綁定定義
    // \ 鍵：開啟/關閉整個翻譯功能
    public static final Lazy<KeyMapping> TOGGLE_TRANSLATION = Lazy.of(() -> 
            new KeyMapping("key.smarttranslator.toggle_translation", GLFW.GLFW_KEY_BACKSLASH, "key.categories.smarttranslator"));
    
    // / 鍵：打開翻譯管理界面
    public static final Lazy<KeyMapping> OPEN_TRANSLATION_GUI = Lazy.of(() -> 
            new KeyMapping("key.smarttranslator.open_gui", GLFW.GLFW_KEY_SLASH, "key.categories.smarttranslator"));
    
    // R 鍵：按住顯示物品tooltip原文
    public static final Lazy<KeyMapping> SHOW_ORIGINAL_TOOLTIP = Lazy.of(() -> 
            new KeyMapping("key.smarttranslator.show_original", GLFW.GLFW_KEY_R, "key.categories.smarttranslator"));
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // T 鍵：切換翻譯功能開關
        if (minecraft.screen == null && TOGGLE_TRANSLATION.get().consumeClick()) {
            boolean currentState = SmartTranslatorConfig.ENABLED.get();
            SmartTranslatorConfig.ENABLED.set(!currentState);
            
            // 顯示狀態訊息
            String statusMessage = !currentState ? "翻譯功能已啟用" : "翻譯功能已停用";
            minecraft.player.displayClientMessage(Component.literal(statusMessage), true);
        }
        
        // O 鍵：打開翻譯管理界面
        if (minecraft.screen == null && OPEN_TRANSLATION_GUI.get().consumeClick()) {
            minecraft.setScreen(new TranslationManagerScreen());
        }
    }
}