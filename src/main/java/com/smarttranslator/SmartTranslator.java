package com.smarttranslator;

import com.smarttranslator.cache.TranslationCache;
import com.smarttranslator.client.SmartTranslatorModMenuIntegration;
import com.smarttranslator.config.SmartTranslatorConfig;
import com.smarttranslator.events.ChatTranslationHandler;
import com.smarttranslator.events.ItemTooltipHandler;
import com.smarttranslator.events.KeyBindingHandler;
import com.smarttranslator.translation.TranslationManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SmartTranslator.MODID)
public class SmartTranslator {
    public static final String MODID = "smarttranslator";
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartTranslator.class);
    
    private static SmartTranslator instance;
    private TranslationCache translationCache;
    private TranslationManager translationManager;
    
    public SmartTranslator(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        
        // 註冊配置
        modContainer.registerConfig(ModConfig.Type.CLIENT, SmartTranslatorConfig.SPEC);
        
        // 註冊事件監聽器
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeyMappings);
        
        LOGGER.info("Smart Translator MOD 初始化完成");
    }
    
    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 初始化翻譯緩存
        this.translationCache = new TranslationCache();
        
        // 初始化翻譯管理器
        this.translationManager = new TranslationManager(this.translationCache);
        
        LOGGER.info("Smart Translator 組件初始化完成");
    }
    
    private void onClientSetup(FMLClientSetupEvent event) {
        // 註冊客戶端事件處理器
        NeoForge.EVENT_BUS.register(new ChatTranslationHandler());
        NeoForge.EVENT_BUS.register(new KeyBindingHandler());
        NeoForge.EVENT_BUS.register(new ItemTooltipHandler());
        
        // 註冊配置螢幕
        event.enqueueWork(() -> {
            SmartTranslatorModMenuIntegration.registerConfigScreen();
        });
        
        LOGGER.info("Smart Translator 客戶端設置完成");
    }
    
    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindingHandler.TOGGLE_TRANSLATION.get());
        event.register(KeyBindingHandler.OPEN_TRANSLATION_GUI.get());
    }
    
    public static SmartTranslator getInstance() {
        return instance;
    }
    
    public TranslationCache getTranslationCache() {
        return translationCache;
    }
    
    public TranslationManager getTranslationManager() {
        return translationManager;
    }
}