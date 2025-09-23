package com.smarttranslator.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Smart Translator MOD 配置
 */
public class SmartTranslatorConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // 基本設定
    // 翻譯功能開關
    public static final ModConfigSpec.BooleanValue ENABLED;
    
    public static final ModConfigSpec.BooleanValue AUTO_TRANSLATE_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> TARGET_LANGUAGE;
    public static final ModConfigSpec.ConfigValue<String> TRANSLATION_API;
    // Google 翻譯 API 金鑰
    public static final ModConfigSpec.ConfigValue<String> GOOGLE_API_KEY;
    public static final ModConfigSpec.ConfigValue<String> API_KEY;
    
    // 緩存設定
    public static final ModConfigSpec.BooleanValue CACHE_ENABLED;
    public static final ModConfigSpec.IntValue CACHE_EXPIRY_DAYS;
    public static final ModConfigSpec.IntValue MAX_CACHE_SIZE;
    
    // 翻譯設定
    public static final ModConfigSpec.BooleanValue TRANSLATE_CHAT;
    public static final ModConfigSpec.BooleanValue TRANSLATE_SIGNS;
    public static final ModConfigSpec.BooleanValue TRANSLATE_BOOKS;
    public static final ModConfigSpec.IntValue TRANSLATION_DELAY_MS;
    
    // 性能設定
    public static final ModConfigSpec.IntValue MAX_BATCH_SIZE;
    public static final ModConfigSpec.LongValue BATCH_TIMEOUT_MS;
    public static final ModConfigSpec.IntValue MAX_CONCURRENT_TRANSLATIONS;
    public static final ModConfigSpec.IntValue THREAD_POOL_SIZE;
    
    // 顯示設定
    public static final ModConfigSpec.BooleanValue SHOW_ORIGINAL_TEXT;
    public static final ModConfigSpec.BooleanValue SHOW_TRANSLATION_STATUS;
    public static final ModConfigSpec.ConfigValue<String> TRANSLATION_PREFIX;
    
    static {
        BUILDER.comment("Smart Translator 基本設定").push("general");
        
        ENABLED = BUILDER
                .comment("是否啟用翻譯功能")
                .define("enabled", true);
        
        AUTO_TRANSLATE_ENABLED = BUILDER
                .comment("是否啟用自動翻譯")
                .define("autoTranslateEnabled", true);
        
        TARGET_LANGUAGE = BUILDER
                .comment("目標翻譯語言 (zh-TW, zh-CN, ja, ko 等) - 已移除英文支援")
                .define("targetLanguage", "zh-TW");
        
        TRANSLATION_API = BUILDER
                .comment("翻譯 API 服務 (google, baidu, youdao, deepl)")
                .define("translationApi", "google");
        
        GOOGLE_API_KEY = BUILDER
                .comment("Google 翻譯 API 金鑰")
                .define("googleApiKey", "");
        
        API_KEY = BUILDER
                .comment("API 金鑰 (某些服務需要)")
                .define("apiKey", "");
        
        BUILDER.pop();
        
        BUILDER.comment("緩存設定").push("cache");
        
        CACHE_ENABLED = BUILDER
                .comment("是否啟用翻譯緩存")
                .define("cacheEnabled", true);
        
        CACHE_EXPIRY_DAYS = BUILDER
                .comment("緩存過期天數")
                .defineInRange("cacheExpiryDays", 30, 1, 365);
        
        MAX_CACHE_SIZE = BUILDER
                .comment("最大緩存項目數量")
                .defineInRange("maxCacheSize", 10000, 100, 100000);
        
        BUILDER.pop();
        
        BUILDER.comment("翻譯範圍設定").push("translation");
        
        TRANSLATE_CHAT = BUILDER
                .comment("翻譯聊天訊息")
                .define("translateChat", true);
        
        TRANSLATE_SIGNS = BUILDER
                .comment("翻譯告示牌")
                .define("translateSigns", false);
        
        TRANSLATE_BOOKS = BUILDER
                .comment("翻譯書本內容")
                .define("translateBooks", false);
        
        TRANSLATION_DELAY_MS = BUILDER
                .comment("翻譯延遲毫秒數 (避免頻繁請求)")
                .defineInRange("translationDelayMs", 500, 0, 5000);

        BUILDER.pop();
        
        BUILDER.comment("性能設定").push("performance");
        
        MAX_BATCH_SIZE = BUILDER
                .comment("批量翻譯最大批次大小")
                .defineInRange("maxBatchSize", 10, 1, 50);
        
        BATCH_TIMEOUT_MS = BUILDER
                .comment("批量翻譯超時時間（毫秒）")
                .defineInRange("batchTimeoutMs", 5000L, 1000L, 30000L);
        
        MAX_CONCURRENT_TRANSLATIONS = BUILDER
                .comment("最大並發翻譯數量")
                .defineInRange("maxConcurrentTranslations", 3, 1, 10);
        
        THREAD_POOL_SIZE = BUILDER
                .comment("翻譯線程池大小 (影響大文本翻譯性能)")
                .defineInRange("threadPoolSize", 8, 2, 16);

        BUILDER.pop();
        
        BUILDER.comment("顯示設定").push("display");
        
        SHOW_ORIGINAL_TEXT = BUILDER
                .comment("是否顯示原始文字")
                .define("showOriginalText", true);
        
        SHOW_TRANSLATION_STATUS = BUILDER
                .comment("是否顯示翻譯狀態")
                .define("showTranslationStatus", false);
        
        TRANSLATION_PREFIX = BUILDER
                .comment("翻譯文字前綴")
                .define("translationPrefix", "§a[翻譯]§r ");
        
        BUILDER.pop();
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}