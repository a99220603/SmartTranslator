package com.smarttranslator.translation.api;

/**
 * 翻譯 API 介面
 */
public interface TranslationAPI {
    
    /**
     * 翻譯文字
     * 
     * @param text 要翻譯的文字
     * @param targetLanguage 目標語言代碼
     * @return 翻譯結果
     * @throws Exception 翻譯失敗時拋出異常
     */
    String translate(String text, String targetLanguage) throws Exception;
    
    /**
     * 獲取 API 名稱
     * 
     * @return API 名稱
     */
    String getApiName();
    
    /**
     * 檢查 API 是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}