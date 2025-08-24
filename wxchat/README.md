# 微信小程序 - 报告查询系统

这是一个基于微信小程序开发的报告查询和浏览系统，参考了现代文档管理界面的设计理念。

## 功能特性

### 🔍 搜索功能
- 支持关键词搜索
- 实时搜索过滤
- 可搜索报告标题、来源、分类、标签等字段
- 搜索栏采用渐变设计，视觉效果佳

### 📊 报告列表
- 展示报告缩略图（使用占位图片）
- 显示报告完整标题和摘要
- 包含报告来源、分类、页数等元信息
- 显示发布时间和标签信息
- 支持点击查看报告详情
- 支持分页加载和上拉加载更多

### 🎨 界面设计
- 现代化卡片式布局
- 响应式设计，适配不同屏幕尺寸
- 渐变色彩搭配，视觉效果丰富
- 阴影和圆角设计，提升用户体验
- 加载状态指示器和分页提示

### 🔌 API集成
- 完整的RESTful API接口定义
- 支持生产环境和开发环境切换
- Mock数据服务，便于开发测试
- 完善的错误处理和用户反馈

## 项目结构

```
wxchat/
├── app.js                 # 小程序主入口文件
├── app.json              # 小程序配置文件
├── app.wxss              # 全局样式文件
├── pages/
│   ├── index/            # 首页
│   │   ├── index.js      # 首页逻辑（集成API调用）
│   │   ├── index.wxml    # 首页模板
│   │   └── index.wxss    # 首页样式
│   └── logs/             # 日志页面
├── utils/
│   ├── util.js           # 工具函数
│   └── api.js            # API服务文件
├── mock/
│   └── index-data.json   # Mock数据文件
├── api/
│   └── report-api.md     # API接口文档
└── project.config.json   # 项目配置文件
```

## API接口规范

### 基础信息
- 基础URL: `https://api.example.com/v1`
- 请求方式: GET/POST
- 数据格式: JSON
- 字符编码: UTF-8

### 主要接口

#### 1. 获取报告列表
- **接口地址**: `GET /reports`
- **功能**: 获取分页报告列表
- **支持参数**: 分页、关键词、分类、来源、日期范围等

#### 2. 搜索报告
- **接口地址**: `POST /reports/search`
- **功能**: 高级搜索，支持多条件过滤
- **支持参数**: 关键词、过滤条件、排序等

#### 3. 获取报告详情
- **接口地址**: `GET /reports/{id}`
- **功能**: 获取单个报告详细信息

### 数据模型

报告对象包含以下字段：
- 基本信息：ID、标题、摘要、缩略图
- 分类信息：来源、分类、标签
- 文件信息：页数、文件大小、价格
- 统计信息：下载次数、浏览次数
- 时间信息：发布日期、更新日期

## 技术特点

- **组件化开发**: 使用微信小程序原生框架
- **响应式布局**: 支持不同设备尺寸
- **搜索算法**: 实现实时搜索过滤功能
- **数据管理**: 使用Page的data管理状态
- **事件处理**: 完善的用户交互事件处理
- **API集成**: 支持RESTful API和Mock数据
- **分页加载**: 支持上拉加载更多和下拉刷新
- **错误处理**: 完善的网络请求错误处理

## PDF预览功能

本小程序已集成新的PDF预览功能，使用分片下载服务替代原有的URL直接预览方式，提供更安全、更稳定的PDF查看体验。

### 🔒 安全性提升
- **消除URL暴露风险**：不再直接返回OSS签名URL，避免AK泄露
- **分片加密传输**：每个PDF分片使用独立的加密密钥
- **临时访问控制**：基于文件ID的访问控制，不暴露内部文件路径

### 🚀 性能优化
- **分片下载**：大文件按1MB分片处理，避免内存溢出
- **断点续传**：支持下载中断后的续传功能
- **并发下载**：支持多个分片并发下载，提升下载速度
- **智能缓存**：自动清理过期缓存，优化内存使用

### 🎯 用户体验
- **实时进度**：显示下载进度和分片信息
- **下载控制**：支持暂停、继续、取消下载操作
- **智能预览**：根据文件大小自动选择预览模式
- **错误处理**：完善的错误提示和重试机制

### 使用方法

#### 1. 预览PDF文件

##### 方式一：通过预览标签页
1. 在报告详情页面，点击"预览"标签
2. 系统自动开始下载PDF文件
3. 显示下载进度和分片信息
4. 下载完成后自动显示PDF预览

##### 方式二：通过下载按钮
1. 在报告详情页面，点击"下载文档"按钮
2. 确认购买后开始下载PDF文件
3. 下载完成后可预览或使用其他应用打开

#### 2. 下载控制

##### 暂停下载
- 在下载过程中，点击"暂停"按钮可暂停下载
- 暂停后下载状态会保存，支持后续恢复

##### 恢复下载
- 暂停后，点击"继续"按钮可恢复下载
- 系统会从上次暂停的位置继续下载

##### 取消下载
- 点击"取消"按钮可取消当前下载任务
- 已下载的分片数据会被清理

### 技术架构

#### 后端服务
- **PdfStreamService**：PDF文件流服务，负责分片加密和流式传输
- **PdfStreamController**：PDF文件流控制器，提供REST API接口
- **CacheCleanupService**：缓存清理定时任务服务

#### 小程序端
- **pdfDownloadService.js**：PDF分片下载服务
- **reportDetail页面**：集成下载服务的报告详情页面
- **config.js**：统一配置文件

#### API接口
```
GET /v1/pdf/stream/{fileId}          # 获取PDF文件流（支持断点续传）
GET /v1/pdf/chunk/{fileId}/{chunkIndex}  # 获取PDF文件分片（加密）
GET /v1/pdf/info/{fileId}            # 获取PDF文件信息
POST /v1/pdf/cache/cleanup           # 清理过期缓存（管理员）
```

## 接口调用规范

### 🎯 核心原则

#### 1. **统一使用封装API**
- 所有接口调用必须使用`utils/api.js`中封装的`reportAPI`方法
- 禁止直接使用`wx.request`
- 保持接口调用的统一性和可维护性

#### 2. **异步处理标准**
- 优先使用`async/await`语法
- 避免使用`.then()/.catch()`链式调用
- 确保代码的可读性和错误处理

#### 3. **返回值判断规范**
- 统一使用`result.code === 200`判断成功
- 通过`result.data`获取实际数据
- 通过`result.message`获取错误信息

#### 4. **错误处理完整**
- 使用`try-catch-finally`结构
- 提供用户友好的错误提示
- 记录详细的错误日志

### 📝 标准写法模板

#### 基础模板
```javascript
async methodName() {
  try {
    // 1. 调用API
    const result = await reportAPI.methodName(params);
    
    // 2. 判断返回值
    if (result.code === 200) {
      // 3. 处理成功情况
      const data = result.data;
      this.setData({ ... });
    } else {
      // 4. 处理业务错误
      wx.showToast({
        title: result.message || '操作失败',
        icon: 'none'
      });
    }
  } catch (error) {
    // 5. 处理网络错误
    console.error('操作失败:', error);
    wx.showToast({
      title: '操作失败，请重试',
      icon: 'none'
    });
  }
}
```

#### 带加载状态模板
```javascript
async methodName() {
  if (this.data.loading) return;
  
  this.setData({ loading: true });
  
  try {
    const result = await reportAPI.methodName(params);
    
    if (result.code === 200) {
      const data = result.data;
      this.setData({ 
        data: data,
        loading: false 
      });
    } else {
      wx.showToast({
        title: result.message || '操作失败',
        icon: 'none'
      });
    }
  } catch (error) {
    console.error('操作失败:', error);
    wx.showToast({
      title: '操作失败，请重试',
      icon: 'none'
    });
  } finally {
    this.setData({ loading: false });
  }
}
```

## 问题解决记录

### 🚨 问题描述

在切换预览按钮时出现以下错误：
```
开始下载失败: TypeError: pdfDownloadService.startDownload is not a function
```

### 🔍 问题分析

经过检查发现以下问题：

1. **模块导出问题**：`pdfDownloadService.js`使用了ES6的`export default`语法，但小程序使用CommonJS的`require`语法
2. **依赖问题**：文件中有未定义的`BASE_URL`引用
3. **配置依赖**：过度依赖复杂的配置文件，增加了出错概率

### ✅ 解决方案

#### 1. 修复模块导出
```javascript
// ❌ 错误的ES6导出
export default { ... };

// ✅ 正确的CommonJS导出
module.exports = { ... };
```

#### 2. 简化依赖关系
```javascript
// ❌ 复杂的配置依赖
const config = require('./config.js');
url: config.getApiUrl(`/pdf/info/${downloadTask.id}`)

// ✅ 简化的直接引用
url: `http://wx.yuesf.cn/v1/pdf/info/${downloadTask.id}`
```

#### 3. 移除未定义的变量
```javascript
// ❌ 未定义的BASE_URL引用
this.BASE_URL = BASE_URL;

// ✅ 移除不必要的引用
// 直接使用硬编码的baseUrl
```

## 使用方法