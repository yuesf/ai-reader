# 小程序报告图片缓存机制

## 概述

为了优化用户体验，避免每次跳转预览页面时重新生成报告图片，我们实现了一套完整的图片缓存机制。

## 核心组件

### 1. ImageCache (wxchat/utils/imageCache.js)

图片缓存服务的核心类，提供以下功能：

- **缓存管理**: 使用 Map 存储缓存项，支持最大缓存数量限制（默认50个）
- **过期机制**: 缓存项有24小时的过期时间
- **本地文件管理**: 自动管理本地临时文件的创建和删除
- **内存优化**: 当缓存达到上限时，自动清理最旧的缓存项

#### 主要方法

- `setCache(reportId, pageIndex, imagePath)`: 设置缓存
- `getCache(reportId, pageIndex)`: 获取缓存
- `clearReportCache(reportId)`: 清理指定报告的缓存
- `clearAllCache()`: 清理所有缓存
- `getCacheStats()`: 获取缓存统计信息

### 2. PdfImagePreviewService (wxchat/utils/pdfImagePreviewService.js)

PDF图片预览服务，集成了缓存功能：

- **缓存集成**: 在获取图片时优先检查缓存
- **异步下载**: 当缓存未命中时，异步下载并缓存图片
- **本地存储**: 将下载的图片保存到本地文件系统

#### 主要改进

- `getPageImage(pageIndex)`: 优先返回缓存图片，未命中时返回网络URL并异步缓存
- `downloadAndCacheImage()`: 新增方法，负责下载并缓存图片到本地
- `getCachedLocalImagePath()`: 获取已缓存的本地图片路径
- `cacheLocalImagePath()`: 缓存本地图片路径

### 3. PdfPreview页面 (wxchat/pages/pdfPreview/pdfPreview.js)

预览页面集成缓存功能：

- **缓存优先**: 在加载页面时优先使用缓存图片
- **快速显示**: 缓存命中时直接显示，无需网络请求
- **缓存管理**: 提供清理当前报告缓存的功能

#### 主要改进

- `tryAppendPage()`: 优先检查缓存，命中时直接添加到页面列表
- `clearCurrentReportCache()`: 新增方法，用于清理当前报告的缓存

## 缓存策略

### 缓存键生成
```javascript
const cacheKey = `report_${reportId}_page_${pageIndex}`;
```

### 本地文件命名
```javascript
const fileName = `report_${fileId}_page_${pageIndex}.jpg`;
const localPath = `${wx.env.USER_DATA_PATH}/${fileName}`;
```

### 缓存生命周期

1. **创建**: 当用户首次访问某页面时，图片下载完成后自动缓存
2. **使用**: 后续访问时直接从缓存读取，无需重新下载
3. **过期**: 24小时后自动过期，下次访问时重新下载
4. **清理**: 缓存达到上限时，自动清理最旧的缓存项

## 性能优化

### 1. 内存管理
- 最大缓存数量限制：50个图片
- 自动清理最旧缓存项
- 及时删除过期的本地文件

### 2. 网络优化
- 缓存命中时零网络请求
- 异步下载，不阻塞用户界面
- 防重复请求机制

### 3. 用户体验
- 缓存图片即时显示
- 首次加载时显示网络图片，同时后台缓存
- 支持手动清理缓存功能

## 使用示例

### 基本使用
```javascript
// 获取图片（自动处理缓存）
const imageSrc = pdfImagePreviewService.getPageImage(pageIndex);

// 清理指定报告的缓存
pdfImagePreviewService.clearReportCache(reportId);

// 获取缓存统计
const stats = imageCache.getCacheStats();
console.log('缓存统计:', stats);
```

### 在页面中使用
```javascript
// 检查缓存
const cachedPath = pdfImagePreviewService.getCachedLocalImagePath(fileId, pageIndex);
if (cachedPath) {
  // 使用缓存图片
  this.setData({ imageSrc: cachedPath });
} else {
  // 使用网络图片并缓存
  const networkSrc = pdfImagePreviewService.getPageImage(pageIndex);
  this.setData({ imageSrc: networkSrc });
}
```

## 调试功能

### 缓存统计
```javascript
const stats = imageCache.getCacheStats();
console.log('缓存大小:', stats.size);
console.log('最大缓存:', stats.maxSize);
console.log('缓存项目:', stats.items);
```

### 手动清理
```javascript
// 清理当前报告缓存
this.clearCurrentReportCache();

// 清理所有缓存
imageCache.clearAllCache();
```

## 注意事项

1. **存储空间**: 缓存图片会占用本地存储空间，建议定期清理
2. **网络环境**: 首次访问仍需网络下载，后续访问才能享受缓存优势
3. **文件格式**: 目前支持JPG格式的图片缓存
4. **错误处理**: 缓存文件损坏或丢失时会自动重新下载

## 未来优化方向

1. **压缩算法**: 对缓存图片进行压缩，减少存储空间占用
2. **预加载策略**: 智能预加载用户可能访问的页面
3. **网络状态感知**: 根据网络状况调整缓存策略
4. **用户偏好**: 允许用户自定义缓存大小和过期时间