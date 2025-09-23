# Minecraft 顏色代碼保留功能使用指南

## 概述

SmartTranslator 現在支持在翻譯過程中保留 Minecraft 的顏色代碼和格式化代碼，確保翻譯後的文本保持原有的視覺效果。

## 支持的格式代碼

### 顏色代碼
- `§0` - 黑色 (Black)
- `§1` - 深藍色 (Dark Blue)
- `§2` - 深綠色 (Dark Green)
- `§3` - 深青色 (Dark Aqua)
- `§4` - 深紅色 (Dark Red)
- `§5` - 深紫色 (Dark Purple)
- `§6` - 金色 (Gold)
- `§7` - 灰色 (Gray)
- `§8` - 深灰色 (Dark Gray)
- `§9` - 藍色 (Blue)
- `§a` - 綠色 (Green)
- `§b` - 青色 (Aqua)
- `§c` - 紅色 (Red)
- `§d` - 淺紫色 (Light Purple)
- `§e` - 黃色 (Yellow)
- `§f` - 白色 (White)

### 格式代碼
- `§k` - 隨機字符 (Obfuscated)
- `§l` - 粗體 (Bold)
- `§m` - 刪除線 (Strikethrough)
- `§n` - 下劃線 (Underline)
- `§o` - 斜體 (Italic)
- `§r` - 重置 (Reset)

## 功能特性

### 1. 智能顏色檢測
系統會自動檢測文本中的 Minecraft 格式化代碼，並在翻譯過程中保留這些代碼。

### 2. 格式恢復
翻譯完成後，系統會智能地將格式化代碼應用到翻譯後的文本中，保持原有的視覺效果。

### 3. 多種處理模式
- **完全保留模式**: 保留所有檢測到的格式化代碼
- **智能映射模式**: 根據文本內容智能地映射格式代碼位置
- **降級處理模式**: 當智能處理失敗時，使用基本的格式恢復

## 使用方法

### 基本使用

```java
// 使用 MinecraftTextProcessor 進行預處理和後處理
String originalText = "§aHello §bWorld!";
String preprocessed = MinecraftTextProcessor.preprocessText(originalText);
// 進行翻譯...
String translated = "你好 世界!";
String result = MinecraftTextProcessor.postprocessText(translated, originalText);
// 結果: "§a你好 §b世界!"
```

### 直接使用顏色保留翻譯器

```java
// 直接使用 ColorPreservingTranslator
String original = "§cError: §fFile not found";
String translated = "錯誤: 找不到文件";
String result = ColorPreservingTranslator.translateWithColorPreservation(original, translated);
// 結果: "§c錯誤: §f找不到文件"
```

### 檢查是否需要特殊處理

```java
String text = "§aGreen text";
if (MinecraftTextProcessor.needsSpecialProcessing(text)) {
    // 需要特殊處理
    String processed = MinecraftTextProcessor.preprocessText(text);
    // 進行翻譯...
}
```

## 高級功能

### 1. 預處理結果檢查

```java
ColorPreservingTranslator.TranslationResult result = 
    ColorPreservingTranslator.preprocessForTranslation("§aHello §bWorld");
    
System.out.println("清理後的文本: " + result.cleanText);
System.out.println("格式化代碼: " + result.formattingCodes);
System.out.println("主要顏色: " + result.primaryColor);
```

### 2. 自定義格式恢復

```java
// 使用高級格式恢復
String original = "§aShort";
ColorPreservingTranslator.TranslationResult preprocessed = 
    ColorPreservingTranslator.preprocessForTranslation(original);
String translated = "這是一個很長的翻譯文本";
String result = ColorPreservingTranslator.advancedRestoreFormatting(translated, preprocessed);
```

## 最佳實踐

### 1. 處理流程
1. 使用 `needsSpecialProcessing()` 檢查文本是否包含格式化代碼
2. 使用 `preprocessText()` 進行預處理
3. 進行翻譯
4. 使用 `postprocessText()` 恢復格式

### 2. 錯誤處理
系統內建了錯誤處理機制，當智能處理失敗時會自動降級到基本處理模式。

### 3. 性能考慮
- 對於不包含格式化代碼的文本，系統會跳過特殊處理
- 使用緩存機制提高重複文本的處理效率

## 測試用例

項目包含完整的測試套件，涵蓋以下場景：
- 基本顏色代碼保留
- 格式代碼處理
- 混合顏色和格式代碼
- 錯誤處理
- 邊界情況

運行測試：
```bash
./gradlew test
```

## 故障排除

### 常見問題

1. **格式代碼丟失**
   - 確保使用了正確的處理流程
   - 檢查原始文本是否包含有效的格式化代碼

2. **翻譯結果不正確**
   - 檢查翻譯文本的長度是否與原文差異過大
   - 使用調試模式查看處理過程

3. **性能問題**
   - 對於大量文本，考慮批量處理
   - 使用 `needsSpecialProcessing()` 預先篩選

### 調試模式

系統會在控制台輸出處理過程的詳細信息，幫助診斷問題：

```
後處理文本: Hello World -> §aHello §bWorld
```

## 更新日誌

- **v1.0.0**: 初始版本，支持基本的顏色代碼保留
- **v1.1.0**: 添加格式代碼支持和智能映射
- **v1.2.0**: 改進錯誤處理和性能優化

## 貢獻

歡迎提交 Issue 和 Pull Request 來改進這個功能。請確保：
1. 添加適當的測試用例
2. 更新相關文檔
3. 遵循現有的代碼風格