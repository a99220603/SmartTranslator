# Wynncraft 符號處理指南

## 概述

SmartTranslator 現在支持完整的 Wynncraft 符號映射處理，提供三種不同的處理模式，讓用戶可以根據需要選擇是否翻譯特殊符號。

## 符號分類

### 核心符號（用戶指定）
這些是用戶特別指定的核心 Wynncraft 符號：

- ⚔ → 劍 (武器)
- ❤ → 生命 (血量)
- ✦ → 法力 (魔力)
- ⬡ → 防禦 (護甲)
- ✤ → 敏捷 (速度)
- ❋ → 智力 (智慧)

### 擴展符號（其他常見符號）
包括其他 Wynncraft 常見符號：

- 武器工具：⛏ 🏹 🛡 🪓 🔱
- 職業符號：🔮 💀 🌿
- 屬性符號：💙 ⚡ 🔥 💧 🌍 💨
- 幾何符號：✧ ◆ ◇ ● ○ ■ □ ▲ △ ► ◄ ▼
- 數字符號：① ② ③ ④ ⑤ ⑥ ⑦ ⑧ ⑨ ⑩

## 處理模式

### 1. PRESERVE_CORE（默認模式）
- **描述**：保留核心符號，翻譯其他符號
- **行為**：
  - 核心符號（⚔❤✦⬡✤❋）保持原樣
  - 擴展符號會被翻譯為中文
- **適用場景**：大多數用戶的首選，保持重要符號的視覺效果

### 2. TRANSLATE_ALL
- **描述**：翻譯所有符號
- **行為**：
  - 所有 Wynncraft 符號都會被翻譯為中文
  - 提供完全中文化的體驗
- **適用場景**：希望完全中文化界面的用戶

### 3. PRESERVE_ALL
- **描述**：保留所有符號
- **行為**：
  - 所有 Wynncraft 符號都保持原樣
  - 不進行任何符號翻譯
- **適用場景**：希望保持原始遊戲視覺效果的用戶

## 使用方法

### 程序化設置
```java
import com.smarttranslator.translation.MinecraftTextProcessor;
import com.smarttranslator.config.SymbolConfig;

// 設置處理模式
MinecraftTextProcessor.setSymbolProcessingMode(
    MinecraftTextProcessor.SymbolProcessingMode.PRESERVE_CORE
);

// 獲取當前模式
MinecraftTextProcessor.SymbolProcessingMode currentMode = 
    MinecraftTextProcessor.getSymbolProcessingMode();

// 檢查符號類型
boolean isCoreSymbol = MinecraftTextProcessor.isCoreSymbol("⚔");
boolean isExtendedSymbol = MinecraftTextProcessor.isExtendedSymbol("🏹");

// 獲取符號翻譯
String translation = MinecraftTextProcessor.getSymbolTranslation("⚔");
```

### 配置管理
```java
import com.smarttranslator.config.SymbolConfig;

// 從字符串獲取模式
SymbolConfig.SymbolProcessingMode mode = 
    SymbolConfig.getModeFromString("PRESERVE_CORE");

// 獲取模式描述
String description = SymbolConfig.getModeDescription(mode);

// 驗證模式有效性
boolean isValid = SymbolConfig.isValidMode("TRANSLATE_ALL");
```

## 測試結果示例

### 測試文本
```
§6武器: ⚔ 傷害 §c❤ 生命 §b✦ 法力 §7⬡ 防禦 §a✤ 敏捷 §d❋ 智力 §e🏹 弓箭 §f①②③
```

### 各模式處理結果

**TRANSLATE_ALL 模式：**
```
武器: 劍 傷害 生命 生命 法力 法力 防禦 防禦 敏捷 敏捷 智力 智力 弓 弓箭 123
```

**PRESERVE_CORE 模式（默認）：**
```
武器: ⚔ 傷害 ❤ 生命 ✦ 法力 ⬡ 防禦 ✤ 敏捷 ❋ 智力 弓 弓箭 123
```

**PRESERVE_ALL 模式：**
```
武器: ⚔ 傷害 ❤ 生命 ✦ 法力 ⬡ 防禦 ✤ 敏捷 ❋ 智力 🏹 弓箭 ①②③
```

## 實現特點

1. **靈活配置**：三種模式滿足不同用戶需求
2. **符號分類**：區分核心符號和擴展符號
3. **實時切換**：可以動態更改處理模式
4. **向後兼容**：默認模式保持良好的用戶體驗
5. **錯誤處理**：無效配置會回退到默認模式

## 配置建議

- **新用戶**：建議使用 `PRESERVE_CORE` 模式
- **完全中文化**：使用 `TRANSLATE_ALL` 模式
- **保持原味**：使用 `PRESERVE_ALL` 模式

## 注意事項

1. 符號處理在文本預處理階段進行
2. 格式化代碼會被正確保留和恢復
3. Unicode 轉義序列會被正確處理
4. 處理模式的更改會立即生效
5. 無效的處理模式會自動回退到默認模式

## 測試文件

- `symbol_mode_test.java`：符號模式測試
- `SymbolConfig.java`：配置管理類
- `MinecraftTextProcessor.java`：核心處理邏輯

這個功能確保了 SmartTranslator 能夠根據用戶偏好靈活處理 Wynncraft 特殊符號，提供最佳的遊戲體驗。