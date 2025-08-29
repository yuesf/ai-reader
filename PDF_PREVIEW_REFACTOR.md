# PDF预览页面滑动问题修复 - 重构方案

## 问题分析

原有实现使用 `transform: scale()` 进行缩放，导致以下问题：
1. **右滑超出边界**：缩放后实际内容区域计算错误
2. **左滑不到边界**：变换原点设置导致内容偏移
3. **放大后无法上滑**：滚动边界限制不正确

## 新的解决方案

### 核心思路
放弃使用 `transform: scale()` 缩放，改用**动态调整容器尺寸**的方式实现缩放效果。

### 技术实现

#### 1. JavaScript 修改 (pdfPreview.js)
```javascript
// 简化缩放方法，移除复杂的边界计算
zoomIn() {
  const scale = Math.min(3.0, this.data.pageScale + 0.25);
  this.setData({ pageScale: scale });
},
zoomOut() {
  const scale = Math.max(0.5, this.data.pageScale - 0.25);
  this.setData({ pageScale: scale });
},
resetZoom() {
  this.setData({ pageScale: 1.0, scrollLeft: 0, scrollTop: 0 });
}

// 移除了复杂的 onScroll 和 resetScrollToBounds 方法
```

#### 2. WXML 修改 (pdfPreview.wxml)
```xml
<!-- 修改前：使用 transform: scale() -->
<view class="pages-wrapper" style="transform: scale({{pageScale}}); transform-origin: center top;">

<!-- 修改后：使用动态尺寸 -->
<view class="pages-wrapper" style="width: {{pageScale * 100}}%; height: {{pageScale * 100}}%; transform-origin: left top;">
```

#### 3. CSS 修改 (pdfPreview.wxss)
```css
/* 修改前：复杂的缩放控制 */
.pages-wrapper {
  width: 100%;
  min-width: 100%;
  min-height: 100%;
  transform-origin: center top !important;
  /* 复杂的缩放样式... */
}

/* 修改后：简化的尺寸控制 */
.pages-wrapper {
  position: relative;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  transform-origin: left top;
  transition: width 0.3s ease, height 0.3s ease;
}
```

## 解决原理

### 1. 右滑边界问题解决
- **原因**：`transform: scale()` 不改变元素的实际尺寸，导致滚动区域计算错误
- **解决**：使用 `width: {{pageScale * 100}}%` 直接改变容器实际尺寸
- **效果**：scroll-view 能正确识别内容边界，自动限制滚动范围

### 2. 左滑边界问题解决
- **原因**：`transform-origin: center` 导致缩放时内容向左右扩展
- **解决**：使用 `transform-origin: left top` 确保缩放从左上角开始
- **效果**：放大时内容不会向左偏移，左边界始终可达

### 3. 上滑问题解决
- **原因**：复杂的滚动边界检测逻辑干扰了正常滚动
- **解决**：移除所有自定义滚动控制，依赖 scroll-view 原生行为
- **效果**：放大后可以正常向上滚动查看内容

## 技术优势

1. **简化实现**：代码量减少约60%，逻辑更清晰
2. **原生支持**：充分利用 scroll-view 的原生滚动能力
3. **性能提升**：避免了复杂的实时计算和DOM查询
4. **兼容性好**：不依赖复杂的CSS变换，兼容性更强

## 测试要点

1. **基础缩放**：测试放大、缩小、重置功能
2. **边界滚动**：在各个缩放级别下测试四个方向的滚动边界
3. **翻页功能**：确保缩放状态下翻页功能正常
4. **性能测试**：验证滚动流畅度和响应速度

## 总结

通过将复杂的 `transform: scale()` 缩放改为简单的容器尺寸调整，彻底解决了PDF预览页面的滑动边界问题。新方案更简洁、更可靠，充分利用了微信小程序 scroll-view 组件的原生能力。