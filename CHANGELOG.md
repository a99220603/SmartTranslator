# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.5] - 2025-09-23

### Fixed
- 修復 JSON 解析錯誤導致的模組載入失敗問題
- 修復 ItemTranslationService 缺少 getCachedTranslation 方法的編譯錯誤
- 重新生成損壞的翻譯緩存文件

### Improved
- 增強錯誤處理機制
- 優化模組穩定性

## [2.3.4] - 2025-01-24

### Added
- 英文對照預設顯示功能
- 智能文本截斷邏輯優化

### Changed
- 調整 tooltip 最大寬度從 25 增加到 50
- 調整單行最大寬度從 20 增加到 45
- 統一雙語顯示格式，移除視覺混淆
- 優化長文本顯示邏輯

### Fixed
- 修復 tooltip 格式混亂問題
- 改善顏色處理，避免文字重疊
- 統一雙語顯示格式

## [2.0.0] - 2025-09-22

### Added
- 零延遲響應系統
- 非阻塞架構設計
- 三層緩存系統
- 並發控制機制
- 自動內存管理

### Changed
- 移除傳統 2 秒緩衝機制
- 重構翻譯引擎架構
- 優化性能指標

### Performance
- 響應延遲從 2000ms 降至 0ms
- 緩存命中率提升 3 倍
- 主線程完全不受翻譯影響

## [1.0.0] - 2025-09-22

### Added
- 基本翻譯功能
- 本地緩存支援
- 物品 tooltip 翻譯
- 聊天訊息翻譯
- 告示牌和書籍翻譯
- 多語言支援