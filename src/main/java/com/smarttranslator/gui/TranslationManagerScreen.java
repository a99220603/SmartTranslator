package com.smarttranslator.gui;

import com.smarttranslator.SmartTranslator;
import com.smarttranslator.cache.CachedTranslation;
import com.smarttranslator.config.SmartTranslatorConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 翻譯管理介面
 */
public class TranslationManagerScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationManagerScreen.class);
    
    private EditBox inputTextBox;
    private EditBox outputTextBox;
    private Button translateButton;
    private Button clearCacheButton;
    private Button configButton;
    
    private int cacheCount = 0;
    private String lastTranslation = "";
    
    public TranslationManagerScreen() {
        super(Component.literal("Smart Translator 管理介面"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = 40;
        
        // 輸入文字框
        this.inputTextBox = new EditBox(this.font, centerX - 150, startY, 300, 20, 
                Component.literal("輸入要翻譯的文字"));
        this.inputTextBox.setMaxLength(500);
        this.addRenderableWidget(this.inputTextBox);
        
        // 輸出文字框
        this.outputTextBox = new EditBox(this.font, centerX - 150, startY + 40, 300, 20, 
                Component.literal("翻譯結果"));
        this.outputTextBox.setEditable(false);
        this.addRenderableWidget(this.outputTextBox);
        
        // 翻譯按鈕
        this.translateButton = Button.builder(Component.literal("翻譯"), 
                button -> performTranslation())
                .bounds(centerX - 75, startY + 80, 150, 20)
                .build();
        this.addRenderableWidget(this.translateButton);
        
        // 清除緩存按鈕
        this.clearCacheButton = Button.builder(Component.literal("清除緩存"), 
                button -> clearCache())
                .bounds(centerX - 150, startY + 120, 100, 20)
                .build();
        this.addRenderableWidget(this.clearCacheButton);
        
        // 配置按鈕
        this.configButton = Button.builder(Component.literal("設定"), 
                button -> openConfig())
                .bounds(centerX + 50, startY + 120, 100, 20)
                .build();
        this.addRenderableWidget(this.configButton);
        
        // 更新緩存統計
        updateCacheStats();
    }
    
    private void performTranslation() {
        String inputText = this.inputTextBox.getValue().trim();
        if (inputText.isEmpty()) {
            this.outputTextBox.setValue("請輸入要翻譯的文字");
            return;
        }
        
        this.translateButton.active = false;
        this.outputTextBox.setValue("翻譯中...");
        
        SmartTranslator.getInstance().getTranslationManager()
                .translateAsync(inputText)
                .thenAccept(translatedText -> {
                    this.minecraft.execute(() -> {
                        this.outputTextBox.setValue(translatedText);
                        this.lastTranslation = translatedText;
                        this.translateButton.active = true;
                        updateCacheStats();
                    });
                })
                .exceptionally(throwable -> {
                    this.minecraft.execute(() -> {
                        this.outputTextBox.setValue("翻譯失敗: " + throwable.getMessage());
                        this.translateButton.active = true;
                    });
                    LOGGER.error("翻譯失敗", throwable);
                    return null;
                });
    }
    
    private void clearCache() {
        SmartTranslator.getInstance().getTranslationCache().clearCache();
        updateCacheStats();
        this.outputTextBox.setValue("緩存已清除");
    }
    
    private void openConfig() {
        // 打開配置介面
        this.minecraft.setScreen(new SmartTranslatorConfigScreen(this));
    }
    
    private void updateCacheStats() {
        this.cacheCount = SmartTranslator.getInstance().getTranslationCache().getCacheSize();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 繪製標題
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 繪製標籤
        guiGraphics.drawString(this.font, "輸入文字:", this.width / 2 - 150, 30, 0xFFFFFF);
        guiGraphics.drawString(this.font, "翻譯結果:", this.width / 2 - 150, 70, 0xFFFFFF);
        
        // 繪製統計資訊
        String statsText = String.format("緩存項目: %d | 目標語言: %s", 
                this.cacheCount, SmartTranslatorConfig.TARGET_LANGUAGE.get());
        guiGraphics.drawString(this.font, statsText, this.width / 2 - 150, 160, 0xAAAAAA);
        
        // 繪製狀態資訊
        String statusText = SmartTranslatorConfig.ENABLED.get() ? "翻譯已啟用" : "翻譯已停用";
        int statusColor = SmartTranslatorConfig.ENABLED.get() ? 0x00FF00 : 0xFF0000;
        guiGraphics.drawString(this.font, statusText, this.width / 2 - 150, 180, statusColor);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}