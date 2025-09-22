package com.smarttranslator.cache;

/**
 * 緩存的翻譯項目
 */
public class CachedTranslation {
    private final String originalText;
    private final String translatedText;
    private final String targetLanguage;
    private final long timestamp;
    
    public CachedTranslation(String originalText, String translatedText, String targetLanguage, long timestamp) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.targetLanguage = targetLanguage;
        this.timestamp = timestamp;
    }
    
    public String getOriginalText() {
        return originalText;
    }
    
    public String getTranslatedText() {
        return translatedText;
    }
    
    public String getTargetLanguage() {
        return targetLanguage;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "CachedTranslation{" +
                "originalText='" + originalText + '\'' +
                ", translatedText='" + translatedText + '\'' +
                ", targetLanguage='" + targetLanguage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}