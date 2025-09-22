package com.smarttranslator.gui;

import com.smarttranslator.config.SmartTranslatorConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Smart Translator 配置螢幕
 */
public class SmartTranslatorConfigScreen extends Screen {
    private final Screen parentScreen;
    
    // 配置控件
    private CycleButton<Boolean> enabledButton;
    private CycleButton<Boolean> autoTranslateButton;
    private CycleButton<String> apiTypeButton;
    private EditBox targetLanguageBox;
    private EditBox apiKeyBox;
    private CycleButton<Boolean> cacheEnabledButton;
    private CycleButton<Boolean> translateChatButton;
    private CycleButton<Boolean> translateSignsButton;
    private CycleButton<Boolean> translateBooksButton;
    private CycleButton<Boolean> showOriginalButton;
    
    public SmartTranslatorConfigScreen(Screen parentScreen) {
        super(Component.translatable("smarttranslator.config.title"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = 50; // 增加起始Y位置，避免與標題重疊
        int spacing = 30; // 增加間距，避免控件重疊
        int currentY = startY;
        int buttonWidth = 280; // 稍微縮小按鈕寬度
        int buttonHeight = 22; // 稍微增加按鈕高度
        
        // 啟用翻譯功能
        this.enabledButton = CycleButton.onOffBuilder()
                .withInitialValue(SmartTranslatorConfig.ENABLED.get())
                .withTooltip(value -> Tooltip.create(Component.translatable("smarttranslator.config.enabled.tooltip")))
                .create(centerX - buttonWidth/2, currentY, buttonWidth, buttonHeight, 
                        Component.translatable("smarttranslator.config.enabled"),
                        (button, value) -> SmartTranslatorConfig.ENABLED.set(value));
        this.addRenderableWidget(this.enabledButton);
        currentY += spacing;
        
        // 自動翻譯
        this.autoTranslateButton = CycleButton.onOffBuilder()
                .withInitialValue(SmartTranslatorConfig.AUTO_TRANSLATE_ENABLED.get())
                .withTooltip(value -> Tooltip.create(Component.translatable("smarttranslator.config.auto_translate.tooltip")))
                .create(centerX - buttonWidth/2, currentY, buttonWidth, buttonHeight,
                        Component.literal("自動翻譯"),
                        (button, value) -> SmartTranslatorConfig.AUTO_TRANSLATE_ENABLED.set(value));
        this.addRenderableWidget(this.autoTranslateButton);
        currentY += spacing;
        
        // 翻譯API類型
        this.apiTypeButton = CycleButton.<String>builder(value -> Component.literal("翻譯API: " + value))
                .withValues("google", "google-ai-studio")
                .withInitialValue(SmartTranslatorConfig.TRANSLATION_API.get())
                .withTooltip(value -> {
                    String tooltipText = value.equals("google") ? 
                        "使用 Google Translate API (免費但有限制)" : 
                        "使用 Google AI Studio API (需要API金鑰)";
                    return Tooltip.create(Component.literal(tooltipText));
                })
                .displayOnlyValue()
                .create(centerX - buttonWidth/2, currentY, buttonWidth, buttonHeight, Component.literal("翻譯API"),
                        (button, value) -> SmartTranslatorConfig.TRANSLATION_API.set(value));
        this.addRenderableWidget(this.apiTypeButton);
        currentY += spacing;
        
        // 目標語言 - 標籤將在render方法中繪製
        currentY += 15; // 為標籤預留空間
        this.targetLanguageBox = new EditBox(this.font, centerX - 100, currentY, 200, 20,
                Component.translatable("smarttranslator.config.target_language"));
        this.targetLanguageBox.setValue(SmartTranslatorConfig.TARGET_LANGUAGE.get());
        this.targetLanguageBox.setMaxLength(10);
        this.addRenderableWidget(this.targetLanguageBox);
        currentY += spacing;
        
        // API 金鑰 - 標籤將在render方法中繪製
        currentY += 15; // 為標籤預留空間
        this.apiKeyBox = new EditBox(this.font, centerX - 100, currentY, 200, 20,
                Component.literal("Google AI Studio API 金鑰"));
        this.apiKeyBox.setValue(SmartTranslatorConfig.GOOGLE_API_KEY.get());
        this.apiKeyBox.setMaxLength(100);
        this.addRenderableWidget(this.apiKeyBox);
        currentY += spacing;
        
        // 啟用緩存
        this.cacheEnabledButton = CycleButton.onOffBuilder()
                .withInitialValue(SmartTranslatorConfig.CACHE_ENABLED.get())
                .withTooltip(value -> Tooltip.create(Component.translatable("smarttranslator.config.cache_enabled.tooltip")))
                .create(centerX - buttonWidth/2, currentY, buttonWidth, buttonHeight,
                        Component.translatable("smarttranslator.config.cache_enabled"),
                        (button, value) -> SmartTranslatorConfig.CACHE_ENABLED.set(value));
        this.addRenderableWidget(this.cacheEnabledButton);
        currentY += spacing;
        
        // 翻譯聊天
        this.translateChatButton = CycleButton.onOffBuilder()
                .withInitialValue(SmartTranslatorConfig.TRANSLATE_CHAT.get())
                .withTooltip(value -> Tooltip.create(Component.translatable("smarttranslator.config.translate_chat.tooltip")))
                .create(centerX - buttonWidth/2, currentY, buttonWidth, buttonHeight,
                        Component.translatable("smarttranslator.config.translate_chat"),
                        (button, value) -> SmartTranslatorConfig.TRANSLATE_CHAT.set(value));
        this.addRenderableWidget(this.translateChatButton);
        currentY += spacing;
        
        // 翻譯告示牌
        this.translateSignsButton = CycleButton.onOffBuilder()
                .withInitialValue(SmartTranslatorConfig.TRANSLATE_SIGNS.get())
                .withTooltip(value -> Tooltip.create(Component.translatable("smarttranslator.config.translate_signs.tooltip")))
                .create(centerX - buttonWidth/2, currentY, buttonWidth, buttonHeight,
                        Component.translatable("smarttranslator.config.translate_signs"),
                        (button, value) -> SmartTranslatorConfig.TRANSLATE_SIGNS.set(value));
        this.addRenderableWidget(this.translateSignsButton);
        currentY += spacing;
        
        // 翻譯書籍
        this.translateBooksButton = CycleButton.onOffBuilder()
                .withInitialValue(SmartTranslatorConfig.TRANSLATE_BOOKS.get())
                .withTooltip(value -> Tooltip.create(Component.translatable("smarttranslator.config.translate_books.tooltip")))
                .create(centerX - buttonWidth/2, currentY, buttonWidth, buttonHeight,
                        Component.translatable("smarttranslator.config.translate_books"),
                        (button, value) -> SmartTranslatorConfig.TRANSLATE_BOOKS.set(value));
        this.addRenderableWidget(this.translateBooksButton);
        currentY += spacing;
        
        // 顯示原文
        this.showOriginalButton = CycleButton.onOffBuilder()
                .withInitialValue(SmartTranslatorConfig.SHOW_ORIGINAL_TEXT.get())
                .withTooltip(value -> Tooltip.create(Component.translatable("smarttranslator.config.show_original.tooltip")))
                .create(centerX - buttonWidth/2, currentY, buttonWidth, buttonHeight,
                        Component.translatable("smarttranslator.config.show_original"),
                        (button, value) -> SmartTranslatorConfig.SHOW_ORIGINAL_TEXT.set(value));
        this.addRenderableWidget(this.showOriginalButton);
        currentY += spacing + 15; // 增加與按鈕的間距
        
        // 儲存按鈕
        Button saveButton = Button.builder(Component.literal("儲存設定"), button -> saveAndClose())
                .bounds(centerX - 80, currentY, 75, 22)
                .build();
        this.addRenderableWidget(saveButton);
        
        // 取消按鈕
        Button cancelButton = Button.builder(Component.literal("取消"), button -> this.onClose())
                .bounds(centerX + 5, currentY, 75, 22)
                .build();
        this.addRenderableWidget(cancelButton);
    }
    
    private void saveAndClose() {
        // 儲存文字框的值
        SmartTranslatorConfig.TARGET_LANGUAGE.set(this.targetLanguageBox.getValue());
        SmartTranslatorConfig.GOOGLE_API_KEY.set(this.apiKeyBox.getValue());
        
        // 儲存配置
        SmartTranslatorConfig.SPEC.save();
        
        this.onClose();
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 繪製標題
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 繪製輸入框標籤
        int centerX = this.width / 2;
        int buttonWidth = 280;
        int startY = 50;
        int spacing = 30;
        
        // 目標語言標籤 (在第4個位置，前面有3個按鈕)
        int targetLangLabelY = startY + spacing * 3;
        guiGraphics.drawString(this.font, "目標語言:", centerX - buttonWidth/2, targetLangLabelY, 0xFFFFFF);
        
        // API金鑰標籤 (在第5個位置)
        int apiKeyLabelY = startY + spacing * 4 + 15; // +15是因為目標語言輸入框的標籤空間
        guiGraphics.drawString(this.font, "Google AI Studio API 金鑰:", centerX - buttonWidth/2, apiKeyLabelY, 0xFFFFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}