# 微信小程序 - AI Reader 开发规范

## 代码规范概述

本规范基于微信小程序开发最佳实践，结合项目实际情况制定，旨在提高代码质量、可维护性和团队协作效率。

### 规范原则
- **一致性**：保持代码风格和结构的一致性
- **可读性**：代码应该清晰易懂，便于理解和维护
- **可维护性**：代码结构合理，便于后续修改和扩展
- **性能性**：在保证功能的前提下，注重性能优化

---

## 文件命名规范

### 1. 页面文件命名

#### 1.1 页面目录结构
```
pages/
├── index/                    # 首页
│   ├── index.js            # 页面逻辑
│   ├── index.wxml          # 页面结构
│   ├── index.wxss          # 页面样式
│   └── index.json          # 页面配置
├── reportDetail/            # 报告详情页
│   ├── reportDetail.js     # 页面逻辑
│   ├── reportDetail.wxml   # 页面结构
│   ├── reportDetail.wxss   # 页面样式
│   └── reportDetail.json   # 页面配置
└── pdfViewer/              # PDF查看器
    ├── pdfViewer.js        # 页面逻辑
    ├── pdfViewer.wxml      # 页面结构
    ├── pdfViewer.wxss      # 页面样式
    └── pdfViewer.json      # 页面配置
```

#### 1.2 命名规则
- **目录名**：使用小驼峰命名法（camelCase）
- **文件名**：与目录名保持一致
- **扩展名**：严格按照微信小程序规范使用

### 2. 工具文件命名

#### 2.1 工具文件结构
```
utils/
├── api.js                  # API服务文件
├── util.js                 # 通用工具函数
├── config.js               # 配置文件
├── pdfConfig.js            # PDF配置
└── pdfDownloadService.js   # PDF下载服务
```

#### 2.2 命名规则
- **功能模块**：使用描述性的英文名称
- **服务文件**：以 `Service` 结尾
- **配置文件**：以 `Config` 结尾
- **工具文件**：以 `util` 或具体功能命名

---

## JavaScript 代码规范

### 1. 基础语法规范

#### 1.1 变量声明
```javascript
// ✅ 推荐：使用 const 和 let
const BASE_URL = 'http://api.yuesf.cn'
const reportAPI = require('../../utils/api.js')

let currentPage = 1
let isLoading = false

// ❌ 避免：使用 var
var baseUrl = 'http://api.yuesf.cn'
```

#### 1.2 函数声明
```javascript
// ✅ 推荐：使用箭头函数和 async/await
const loadReports = async () => {
  try {
    const result = await reportAPI.getReports(params)
    return result
  } catch (error) {
    console.error('加载失败:', error)
    throw error
  }
}

// ✅ 推荐：普通函数声明
function handleSearch(e) {
  const keyword = e.detail.value
  performSearch(keyword)
}
```

### 2. 页面结构规范

#### 2.1 页面基础结构
```javascript
// pages/index/index.js
const { reportAPI, BASE_URL } = require('../../utils/api.js')

Page({
  // 1. 页面数据
  data: {
    searchKeyword: '',
    reports: [],
    filteredReports: [],
    page: 1,
    pageSize: 20,
    total: 0,
    hasMore: true,
    loading: false
  },

  // 2. 生命周期函数
  onLoad() {
    this.loadReports()
  },

  // 3. 事件处理函数
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ searchKeyword: keyword })
    this.performSearch(keyword)
  },

  // 4. 业务逻辑函数
  async loadReports() {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      const result = await reportAPI.getReports({
        page: this.data.page,
        pageSize: this.data.pageSize,
        keyword: this.data.searchKeyword
      })
      
      if (result.code === 200) {
        this.handleReportsSuccess(result.data)
      } else {
        this.handleReportsError(result.message)
      }
    } catch (error) {
      this.handleNetworkError(error)
    } finally {
      this.setData({ loading: false })
    }
  }
})
```

#### 2.2 数据管理规范
```javascript
// ✅ 推荐：统一使用 setData 更新数据
this.setData({
  reports: newReports,
  page: nextPage,
  hasMore: hasMoreData
})

// ✅ 推荐：批量更新，减少 setData 调用次数
this.setData({
  loading: false,
  reports: list,
  total: total
})
```

### 3. API 调用规范

#### 3.1 API 服务封装
```javascript
// utils/api.js
const BASE_URL = 'http://api.yuesf.cn'

/**
 * 通用请求方法
 * @param {string} url - 请求地址
 * @param {object} options - 请求配置
 * @returns {Promise} 请求结果
 */
const request = (url, options = {}) => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}${url}`,
      ...options,
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data)
          } else {
            reject(new Error(res.data.message || '请求失败'))
          }
        } else {
          reject(new Error(`HTTP错误: ${res.statusCode}`))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '网络请求失败'))
      }
    })
  })
}

// 报告相关API服务
const reportAPI = {
  getReports: (params = {}) => {
    return post('/v1/mini/reports', params)
  },
  
  searchReports: (searchParams) => {
    return post('/v1/mini/reports', searchParams)
  },
  
  getReportDetail: (id) => {
    return get(`/v1/mini/reports/${id}`)
  }
}

module.exports = {
  reportAPI,
  request,
  get,
  post,
  BASE_URL
}
```

#### 3.2 API 调用规范
```javascript
// ✅ 推荐：使用 async/await 和统一的错误处理
async loadReports() {
  if (this.data.loading) return
  
  this.setData({ loading: true })
  
  try {
    const result = await reportAPI.getReports({
      page: this.data.page,
      pageSize: this.data.pageSize,
      keyword: this.data.searchKeyword
    })
    
    if (result.code === 200) {
      this.handleSuccess(result.data)
    } else {
      this.handleBusinessError(result.message)
    }
  } catch (error) {
    this.handleNetworkError(error)
  } finally {
    this.setData({ loading: false })
  }
}
```

### 4. 错误处理规范

#### 4.1 错误处理模板
```javascript
// 统一的错误处理模板
const errorHandler = {
  // 网络错误处理
  handleNetworkError(error) {
    console.error('网络请求失败:', error)
    
    let message = '网络异常，请重试'
    if (error.message.includes('超时')) {
      message = '请求超时，请检查网络'
    } else if (error.message.includes('404')) {
      message = '请求的资源不存在'
    }
    
    wx.showToast({
      title: message,
      icon: 'none',
      duration: 3000
    })
  },

  // 业务错误处理
  handleBusinessError(message) {
    wx.showToast({
      title: message || '操作失败',
      icon: 'none',
      duration: 2000
    })
  }
}
```

---

## WXML 模板规范

### 1. 基础结构规范

#### 1.1 页面结构
```xml
<!-- pages/index/index.wxml -->
<view class="page">
  <!-- 搜索区域 -->
  <view class="search-section">
    <view class="search-box">
      <input 
        class="search-input" 
        placeholder="搜索报告..." 
        value="{{searchKeyword}}"
        bindinput="onSearchInput"
        bindconfirm="onSearch"
      />
      <button class="search-btn" bindtap="onSearch">搜索</button>
    </view>
  </view>

  <!-- 报告列表 -->
  <view class="reports-section">
    <view class="reports-list">
      <view 
        class="report-item" 
        wx:for="{{filteredReports}}" 
        wx:key="id"
        bindtap="onReportTap"
        data-id="{{item.id}}"
      >
        <image 
          class="report-thumbnail" 
          src="{{item.thumbnailFull}}" 
          mode="aspectFill"
          lazy-load
        />
        <view class="report-info">
          <text class="report-title">{{item.title}}</text>
          <text class="report-summary">{{item.summary}}</text>
          <view class="report-meta">
            <text class="report-source">{{item.source}}</text>
            <text class="report-category">{{item.category}}</text>
            <text class="report-pages">{{item.pages}}页</text>
          </view>
        </view>
      </view>
    </view>
  </view>

  <!-- 加载状态 -->
  <view class="loading-section" wx:if="{{loading}}">
    <text class="loading-text">加载中...</text>
  </view>
</view>
```

#### 1.2 条件渲染规范
```xml
<!-- ✅ 推荐：使用 wx:if 进行条件渲染 -->
<view class="report-item" wx:if="{{item.isFree}}">
  <text class="free-tag">免费</text>
</view>

<view class="report-item" wx:elif="{{item.price > 0}}">
  <text class="price-tag">¥{{item.price}}</text>
</view>

<!-- ✅ 推荐：使用 wx:for 进行列表渲染 -->
<view 
  class="report-item" 
  wx:for="{{reports}}" 
  wx:key="id"
  wx:for-item="report"
  wx:for-index="index"
>
  <text class="report-title">{{report.title}}</text>
  <text class="report-index">{{index + 1}}</text>
</view>
```

### 2. 事件绑定规范

#### 2.1 事件命名
```xml
<!-- ✅ 推荐：使用描述性的事件名称 -->
<button bindtap="onSearch">搜索</button>
<input bindinput="onSearchInput" />
<view bindtap="onReportTap" data-id="{{item.id}}">点击查看</view>
```

#### 2.2 数据传递
```xml
<!-- ✅ 推荐：使用 data-* 属性传递数据 -->
<view 
  class="report-item" 
  bindtap="onReportTap" 
  data-id="{{item.id}}"
  data-title="{{item.title}}"
  data-category="{{item.category}}"
>
  <text class="report-title">{{item.title}}</text>
</view>

<!-- 在 JS 中获取数据 -->
onReportTap(e) {
  const { id, title, category } = e.currentTarget.dataset
  console.log('报告信息:', { id, title, category })
  
  // 跳转到详情页
  wx.navigateTo({
    url: `../reportDetail/reportDetail?id=${id}`
  })
}
```

---

## WXSS 样式规范

### 1. 样式命名规范

#### 1.1 类名命名
```css
/* ✅ 推荐：使用 BEM 命名规范 */
.page {
  padding: 16px;
}

.search-section {
  margin-bottom: 16px;
}

.search-box {
  display: flex;
  align-items: center;
}

.search-input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.search-btn {
  margin-left: 8px;
  height: 40px;
  padding: 0 16px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
}
```

#### 1.2 样式组织
```css
/* ✅ 推荐：按功能模块组织样式 */
/* 页面基础样式 */
.page {
  padding: 16px;
  background-color: #f5f5f5;
}

/* 搜索区域样式 */
.search-section {
  margin-bottom: 16px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
}

/* 报告列表样式 */
.reports-section {
  margin-bottom: 16px;
}

.reports-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-item {
  display: flex;
  padding: 16px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
```

### 2. 响应式设计规范

#### 2.1 弹性布局
```css
/* ✅ 推荐：使用 flexbox 进行响应式布局 */
.page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.search-section {
  flex-shrink: 0;
  padding: 16px;
}

.reports-section {
  flex: 1;
  padding: 0 16px;
}

.report-item {
  display: flex;
  gap: 12px;
}

.report-thumbnail {
  flex-shrink: 0;
  width: 80px;
  height: 120px;
}

.report-info {
  flex: 1;
  min-width: 0; /* 防止文本溢出 */
}
```

---

## 项目配置规范

### 1. 小程序配置

#### 1.1 app.json 配置
```json
{
  "pages": [
    "pages/index/index",
    "pages/reportDetail/reportDetail",
    "pages/pdfViewer/pdfViewer",
    "pages/logs/logs"
  ],
  "window": {
    "backgroundTextStyle": "light",
    "navigationBarTitleText": "AI Reader",
    "navigationBarBackgroundColor": "#ffffff",
    "navigationBarTextStyle": "black",
    "backgroundColor": "#f5f5f5",
    "enablePullDownRefresh": true,
    "onReachBottomDistance": 50
  },
  "sitemapLocation": "sitemap.json",
  "permission": {
    "scope.userLocation": {
      "desc": "你的位置信息将用于小程序位置接口的效果展示"
    }
  },
  "requiredBackgroundModes": ["audio"],
  "networkTimeout": {
    "request": 10000,
    "downloadFile": 10000
  }
}
```

### 2. 项目配置

#### 2.1 project.config.json
```json
{
  "description": "项目配置文件",
  "packOptions": {
    "ignore": [
      {
        "type": "file",
        "value": ".eslintrc.js"
      }
    ]
  },
  "setting": {
    "urlCheck": false,
    "es6": true,
    "enhance": true,
    "postcss": true,
    "minified": true,
    "newFeature": false,
    "coverView": true,
    "nodeModules": false,
    "autoAudits": false,
    "showShadowRootInWxmlPanel": true,
    "scopeDataCheck": false,
    "uglifyFileName": false,
    "checkInvalidKey": true,
    "checkSiteMap": true,
    "uploadWithSourceMap": true,
    "compileHotReLoad": false,
    "useMultiFrameRuntime": true,
    "useApiHook": true,
    "useApiHostProcess": true,
    "babelSetting": {
      "ignore": [],
      "disablePlugins": [],
      "outputPath": ""
    },
    "enableEngineNative": false,
    "useIsolateContext": false,
    "userConfirmedBundleSwitch": false,
    "packNpmManually": false,
    "packNpmRelationList": [],
    "minifyWXSS": true,
    "disableUseStrict": false,
    "minifyWXML": true,
    "showES6CompileOption": false,
    "useCompilerPlugins": false
  },
  "compileType": "miniprogram",
  "libVersion": "2.19.4",
  "appid": "your-app-id"
}
```

---

## 代码质量规范

### 1. 注释规范

#### 1.1 文件注释
```javascript
/**
 * 报告详情页面
 * @description 展示报告详细信息，支持PDF预览和下载
 * @author 开发团队
 * @date 2024-01-01
 */

const { reportAPI } = require('../../utils/api.js')
```

#### 1.2 函数注释
```javascript
/**
 * 加载报告列表
 * @description 从服务器获取报告列表数据
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码，默认1
 * @param {number} params.pageSize - 每页数量，默认20
 * @param {string} params.keyword - 搜索关键词
 * @returns {Promise<void>}
 */
async loadReports(params = {}) {
  // 函数实现
}
```

#### 1.3 代码注释
```javascript
// 检查是否正在加载，避免重复请求
if (this.data.loading) return

// 设置加载状态
this.setData({ loading: true })

try {
  // 调用API获取报告列表
  const result = await reportAPI.getReports(params)
  
  // 处理成功响应
  if (result.code === 200) {
    this.handleSuccess(result.data)
  } else {
    // 处理业务错误
    this.handleBusinessError(result.message)
  }
} catch (error) {
  // 处理网络错误
  this.handleNetworkError(error)
} finally {
  // 清理加载状态
  this.setData({ loading: false })
}
```

### 2. 代码检查工具

#### 2.1 ESLint 配置
```javascript
// .eslintrc.js
module.exports = {
  env: {
    es6: true,
    browser: true,
    node: true
  },
  extends: [
    'eslint:recommended'
  ],
  parserOptions: {
    ecmaVersion: 2018,
    sourceType: 'module'
  },
  rules: {
    'semi': ['error', 'always'],
    'quotes': ['error', 'single'],
    'indent': ['error', 2],
    'camelcase': 'error',
    'prefer-const': 'error',
    'prefer-arrow-callback': 'error'
  }
}
```

---

## 性能优化规范

### 1. 网络性能优化

#### 1.1 请求优化
```javascript
// ✅ 推荐：使用防抖优化搜索请求
const debounce = (func, wait) => {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

// 防抖搜索
const debouncedSearch = debounce(this.performSearch, 300)

onSearchInput(e) {
  const keyword = e.detail.value
  this.setData({ searchKeyword: keyword })
  debouncedSearch(keyword)
}
```

### 2. 渲染性能优化

#### 2.1 图片优化
```xml
<!-- ✅ 推荐：使用懒加载和占位图 -->
<image 
  class="report-thumbnail" 
  src="{{item.thumbnailFull}}" 
  mode="aspectFill"
  lazy-load
  binderror="onImageError"
  data-index="{{index}}"
/>
```

```javascript
// 图片加载错误处理
onImageError(e) {
  const index = e.currentTarget.dataset.index
  const placeholder = '/images/placeholder.png'
  
  this.setData({
    [`filteredReports[${index}].thumbnailFull`]: placeholder
  })
}
```

---

## 发布规范

### 1. 版本管理

#### 1.1 版本号规范
```json
{
  "version": "1.0.0",
  "description": "AI Reader 小程序 v1.0.0 正式版",
  "changelog": [
    "新增报告搜索功能",
    "优化PDF预览体验",
    "修复已知问题"
  ]
}
```

### 2. 发布检查清单

- [ ] 代码规范检查通过
- [ ] 功能测试完成
- [ ] 性能测试通过
- [ ] 兼容性测试完成
- [ ] 安全扫描通过
- [ ] 文档更新完成
- [ ] 发布计划制定
- [ ] 回滚方案准备

---

## 总结

本开发规范涵盖了微信小程序开发的各个方面，包括代码规范、架构设计、性能优化、测试规范等。遵循这些规范可以：

1. **提高代码质量**：统一的代码风格和结构
2. **增强可维护性**：清晰的模块划分和命名规范
3. **提升开发效率**：标准化的开发流程和工具
4. **保证产品质量**：完善的测试和发布规范

开发团队应该定期回顾和更新这些规范，确保它们与项目发展和技术进步保持同步。
