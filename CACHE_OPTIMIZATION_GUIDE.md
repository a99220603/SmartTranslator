# SmartTranslator 緩存優化指南

## 概述

本指南詳細介紹了 SmartTranslator 的緩存優化功能，包括動態緩存大小調整、智能 LRU 算法和預加載機制。

## 核心優化功能

### 1. 動態緩存大小調整

#### 緩存大小等級
- **SMALL (5,000 項目)**: 適合低內存設備或輕度使用
- **MEDIUM (15,000 項目)**: 適合一般日常使用
- **LARGE (30,000 項目)**: 適合高頻翻譯使用
- **XLARGE (50,000 項目)**: 適合專業用戶或服務器環境
- **XXLARGE (100,000 項目)**: 適合企業級部署

#### 自動調整邏輯
系統會根據以下因素自動調整緩存大小：
- **命中率**: 低於 70% 時考慮增大緩存
- **內存使用率**: 超過 85% 時考慮減小緩存
- **系統可用內存**: 動態計算最佳緩存大小

### 2. 智能 LRU 算法

#### 增強功能
- **訪問頻率統計**: 記錄每個緩存項目的訪問次數
- **智能驅逐策略**: 優先保留高頻訪問的項目
- **預加載機制**: 自動預加載常用翻譯
- **動態調整**: 根據使用模式調整緩存策略

#### 統計信息
- 緩存命中率
- 緩存未命中率
- 驅逐項目數量
- 預加載項目數量
- 平均訪問頻率

### 3. 內存管理優化

#### 內存監控
- 實時監控系統內存使用率
- 自動調整緩存大小以避免內存不足
- 提供詳細的內存使用統計

#### 垃圾回收優化
- 智能清理過期緩存項目
- 減少內存碎片
- 優化對象創建和銷毀

## 配置選項

### 基本配置 (SmartTranslatorConfig.java)
```java
// 緩存啟用狀態
public static final ModConfigSpec.BooleanValue CACHE_ENABLED;

// 緩存過期天數
public static final ModConfigSpec.IntValue CACHE_EXPIRE_DAYS;

// 最大緩存項目數
public static final ModConfigSpec.IntValue MAX_CACHE_SIZE;
```

### 高級配置 (AdvancedCacheConfig.java)
```java
// 計算最佳緩存大小
int optimalSize = AdvancedCacheConfig.calculateOptimalCacheSize();

// 獲取推薦緩存大小
int recommendedSize = AdvancedCacheConfig.getRecommendedCacheSize();

// 檢查是否需要調整緩存
boolean shouldAdjust = AdvancedCacheConfig.shouldAdjustCacheSize(
    currentSize, hitRate, memoryUsage);
```

## 性能優化建議

### 1. 緩存大小設置

#### 低內存設備 (< 4GB RAM)
- 推薦緩存大小: 5,000 - 10,000 項目
- 預估內存使用: 1-2 MB
- 適合場景: 個人輕度使用

#### 一般設備 (4-8GB RAM)
- 推薦緩存大小: 15,000 - 25,000 項目
- 預估內存使用: 3-5 MB
- 適合場景: 日常翻譯需求

#### 高性能設備 (> 8GB RAM)
- 推薦緩存大小: 30,000 - 100,000 項目
- 預估內存使用: 6-20 MB
- 適合場景: 專業翻譯或服務器部署

### 2. 優化策略

#### 提升命中率
1. **增大緩存大小**: 在內存允許的情況下適當增大
2. **啟用預加載**: 自動加載常用翻譯
3. **優化清理策略**: 保留高頻訪問的項目

#### 減少內存使用
1. **定期清理**: 自動清理過期和低頻項目
2. **壓縮存儲**: 優化緩存項目的存儲格式
3. **智能驅逐**: 優先驅逐低價值項目

#### 提升響應速度
1. **預加載機制**: 提前加載可能需要的翻譯
2. **批量處理**: 批量加載和保存緩存
3. **異步操作**: 非阻塞的緩存操作

## 監控和調試

### 緩存統計信息
```java
// 獲取增強版統計信息
EnhancedCacheOptimizer.CacheStats stats = cache.getEnhancedStats();

System.out.println("命中率: " + stats.getHitRate());
System.out.println("未命中率: " + stats.getMissRate());
System.out.println("驅逐數量: " + stats.getEvictionCount());
System.out.println("預加載數量: " + stats.getPreloadCount());
```

### 系統信息
```java
// 獲取系統信息
String systemInfo = AdvancedCacheConfig.getSystemInfo();
System.out.println("系統信息: " + systemInfo);

// 獲取內存使用率
double memoryUsage = AdvancedCacheConfig.getMemoryUsage();
System.out.println("內存使用率: " + (memoryUsage * 100) + "%");
```

## 測試和驗證

### 運行測試
```bash
# 編譯測試文件
javac -cp "src/main/java" cache_optimization_test.java

# 運行測試
java -cp "src/main/java:." cache_optimization_test
```

### 測試內容
1. **高級緩存配置測試**: 驗證緩存大小計算邏輯
2. **內存監控測試**: 測試內存使用率監控
3. **性能對比測試**: 比較不同配置下的性能表現
4. **壓力測試**: 模擬高負載情況下的緩存表現

## 故障排除

### 常見問題

#### 1. 內存不足
**症狀**: OutOfMemoryError 或系統響應緩慢
**解決方案**:
- 減小緩存大小設置
- 啟用自動內存管理
- 增加 JVM 堆內存大小

#### 2. 命中率低
**症狀**: 翻譯響應緩慢，頻繁調用 API
**解決方案**:
- 增大緩存大小
- 啟用預加載機制
- 檢查緩存過期設置

#### 3. 緩存文件損壞
**症狀**: 緩存加載失敗或數據丟失
**解決方案**:
- 刪除緩存文件重新生成
- 檢查文件權限
- 驗證磁盤空間

### 調試技巧

#### 啟用詳細日誌
```java
// 在 TranslationCache 中啟用調試模式
private static final boolean DEBUG_MODE = true;

if (DEBUG_MODE) {
    logger.info("緩存操作: " + operation + ", 鍵: " + key);
}
```

#### 監控緩存性能
```java
// 定期輸出緩存統計
Timer timer = new Timer();
timer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        EnhancedCacheOptimizer.CacheStats stats = cache.getEnhancedStats();
        logger.info("緩存統計: " + stats.toString());
    }
}, 0, 60000); // 每分鐘輸出一次
```

## 最佳實踐

### 1. 配置建議
- 根據設備性能和使用場景選擇合適的緩存大小
- 啟用自動調整功能以適應不同的使用模式
- 定期監控緩存性能並調整配置

### 2. 開發建議
- 使用異步操作避免阻塞主線程
- 實現適當的錯誤處理和恢復機制
- 定期備份重要的緩存數據

### 3. 部署建議
- 在生產環境中啟用性能監控
- 設置合理的內存限制和警告閾值
- 實施定期的緩存清理和維護

## 結論

通過實施這些緩存優化策略，SmartTranslator 可以顯著提升翻譯性能和用戶體驗。關鍵是找到緩存大小、內存使用和響應速度之間的最佳平衡點。

建議用戶根據自己的設備配置和使用習慣，選擇合適的緩存配置，並定期監控和調整以獲得最佳性能。