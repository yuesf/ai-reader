  # 微信小程序 - AI Reader 技术架构文档

## 技术架构概述

### 整体架构
AI Reader 微信小程序采用前后端分离的架构模式，前端使用微信小程序原生框架，后端提供 RESTful API 服务，通过 HTTP/HTTPS 协议进行数据交互。

### 技术栈
- **前端框架**：微信小程序原生框架
- **开发语言**：JavaScript (ES6+)
- **样式方案**：WXSS (类似CSS)
- **模板引擎**：WXML (类似HTML)
- **状态管理**：Page 内置 data 管理
- **网络请求**：wx.request API
- **文件处理**：wx.downloadFile API
- **存储方案**：微信小程序本地存储

---

## 系统架构设计

### 1. 分层架构

```
┌─────────────────────────────────────┐
│           表现层 (UI Layer)          │
│  ┌─────────────┐ ┌─────────────┐   │
│  │   首页      │ │   详情页    │   │
│  │  (index)   │ │(reportDetail)│   │
│  └─────────────┘ └─────────────┘   │
├─────────────────────────────────────┤
│           业务层 (Business Layer)    │
│  ┌─────────────┐ ┌─────────────┐   │
│  │   API服务   │ │  工具函数   │   │
│  │   (api.js) │ │  (util.js)  │   │
│  └─────────────┘ └─────────────┘   │
├─────────────────────────────────────┤
│           数据层 (Data Layer)       │
│  ┌─────────────┐ ┌─────────────┐   │
│  │   Mock数据  │ │   网络请求  │   │
│  │ (mock/*.json)│ │ (wx.request) │   │
│  └─────────────┘ └─────────────┘   │
└─────────────────────────────────────┘
```

### 2. 模块化设计

#### 2.1 核心模块
- **app.js**：小程序入口，全局配置和生命周期管理
- **pages/**：页面模块，每个页面独立管理
- **utils/**：工具模块，提供通用功能
- **api/**：接口模块，定义API规范
- **mock/**：模拟数据，开发测试使用

#### 2.2 页面模块结构
```
pages/
├── index/              # 首页模块
│   ├── index.js       # 业务逻辑
│   ├── index.wxml     # 页面结构
│   └── index.wxss     # 页面样式
├── reportDetail/       # 报告详情模块
│   ├── reportDetail.js # 业务逻辑
│   ├── reportDetail.wxml # 页面结构
│   └── reportDetail.wxss # 页面样式
├── pdfViewer/          # PDF查看器模块
└── logs/               # 日志页面模块
```

---

## 核心技术实现

### 1. 网络请求架构

#### 1.1 请求封装
```javascript
// utils/api.js - 网络请求封装
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
```

#### 1.2 API服务层
```javascript
// 报告相关API服务
const reportAPI = {
  // 获取报告列表
  getReports: (params = {}) => {
    return post('/v1/mini/reports', params)
  },
  
  // 搜索报告
  searchReports: (searchParams) => {
    return post('/v1/mini/reports', searchParams)
  },
  
  // 获取报告详情
  getReportDetail: (id) => {
    return get(`/v1/mini/reports/${id}`)
  }
}
```

### 2. 数据管理架构

#### 2.1 页面数据管理
```javascript
// 页面数据结构设计
Page({
  data: {
    // 搜索相关
    searchKeyword: '',
    
    // 报告数据
    reports: [],
    filteredReports: [],
    
    // 分页相关
    page: 1,
    pageSize: 20,
    total: 0,
    hasMore: true,
    
    // 加载状态
    loading: false
  }
})
```

#### 2.2 数据流管理
```
用户操作 → 事件处理 → API调用 → 数据更新 → 界面刷新
    ↓           ↓         ↓         ↓         ↓
  搜索输入    onSearch   searchAPI  setData   重新渲染
  点击报告    onReportTap getDetail  setData   页面跳转
  下拉刷新    onPullDown  loadReports setData   列表更新
```

### 3. PDF预览架构

#### 3.1 分片下载服务
```javascript
// PDF分片下载核心逻辑
const pdfDownloadService = {
  // 开始下载
  startDownload: (fileId, filename, onProgress, onComplete, onError) => {
    // 1. 获取文件信息
    // 2. 创建下载任务
    // 3. 分片下载
    // 4. 进度回调
    // 5. 完成回调
  },
  
  // 暂停下载
  pauseDownload: (taskId) => {
    // 暂停下载任务
  },
  
  // 恢复下载
  resumeDownload: (taskId) => {
    // 恢复下载任务
  }
}
```

#### 3.2 预览模式管理
```javascript
// 预览模式配置
const PREVIEW_CONFIG = {
  fast: {
    chunkSize: 1024 * 1024,    // 1MB分片
    maxChunks: 5,              // 最大分片数
    timeout: 10000             // 超时时间
  },
  full: {
    chunkSize: 1024 * 1024,    // 1MB分片
    maxChunks: 50,             // 最大分片数
    timeout: 30000             // 超时时间
  }
}
```

### 3. PDF预览技术实现

#### 3.1 功能特性

##### 🔒 安全性提升
- **消除URL暴露风险**：不再直接返回OSS签名URL，避免AK泄露
- **分片加密传输**：每个PDF分片使用独立的加密密钥
- **临时访问控制**：基于文件ID的访问控制，不暴露内部文件路径

##### 🚀 性能优化
- **分片下载**：大文件按1MB分片处理，避免内存溢出
- **断点续传**：支持下载中断后的续传功能
- **并发下载**：支持多个分片并发下载，提升下载速度
- **智能缓存**：自动清理过期缓存，优化内存使用

##### 🎯 用户体验
- **实时进度**：显示下载进度和分片信息
- **下载控制**：支持暂停、继续、取消下载操作
- **智能预览**：根据文件大小自动选择预览模式
- **错误处理**：完善的错误提示和重试机制

#### 3.2 小程序内PDF预览功能

##### 🎯 核心特性
- **无需跳转**：PDF文件在小程序内直接预览，保持用户上下文
- **Canvas渲染**：使用微信小程序Canvas API进行PDF内容渲染
- **交互控制**：支持缩放、翻页、触摸手势等操作
- **实时状态**：显示加载进度、页码信息、预览状态

##### 🏗️ 技术架构
```
┌─────────────────────────────────────┐
│           报告详情页面               │
├─────────────────────────────────────┤
│        小程序内PDF预览区域           │
│  ┌─────────────┐ ┌─────────────┐   │
│  │   Canvas    │ │   控制面板  │   │
│  │  (PDF渲染)  │ │ (缩放/导航) │   │
│  └─────────────┘ └─────────────┘   │
├─────────────────────────────────────┤
│           页面导航控制              │
│  ┌─────────────┐ ┌─────────────┐   │
│  │   上一页    │ │   下一页    │   │
│  └─────────────┘ └─────────────┘   │
└─────────────────────────────────────┘
```

##### 🔧 核心组件

###### PDF预览服务 (`pdfPreviewService.js`)
- **文件加载**：检查文件存在性，获取文件信息和页面数量
- **页面渲染**：使用Canvas API渲染PDF页面内容
- **缓存管理**：页面数据缓存，提升切换性能
- **状态管理**：当前页面、缩放级别、渲染状态等

###### 预览界面组件
- **Canvas画布**：显示PDF页面内容
- **控制面板**：缩放控制（放大、缩小、重置）
- **页面导航**：上一页、下一页切换
- **状态提示**：显示当前预览状态和页码信息

###### 交互处理
- **触摸事件**：支持左右滑动翻页
- **按钮事件**：处理各种控制按钮点击
- **状态同步**：保持界面状态与数据一致

##### 📱 交互功能

###### 缩放控制
- **放大**：点击"放大"按钮，支持0.5x到3.0x缩放
- **缩小**：点击"缩小"按钮，按步长调整显示大小
- **重置**：点击"重置"按钮，恢复原始1.0x缩放

###### 页面导航
- **上一页**：点击"上一页"按钮或向右滑动
- **下一页**：点击"下一页"按钮或向左滑动
- **页码显示**：实时显示当前页码和总页数

###### 触摸手势
- **左右滑动**：检测滑动方向和距离，实现快速翻页
- **手势识别**：支持触摸开始、移动、结束事件处理

##### 🚀 性能优化

###### 渲染优化
- **页面缓存**：已渲染的页面数据会被缓存
- **渲染队列**：避免并发渲染冲突，提升稳定性
- **内存管理**：及时清理不需要的缓存数据

###### 文件处理
- **文件检查**：验证PDF文件存在性和完整性
- **信息获取**：智能估算页面数量（基于文件大小）
- **错误处理**：完善的异常处理和用户提示

#### 3.3 技术架构

##### 后端服务
- **PdfStreamService**：PDF文件流服务，负责分片加密和流式传输
- **PdfStreamController**：PDF文件流控制器，提供REST API接口
- **CacheCleanupService**：缓存清理定时任务服务

##### 小程序端
- **pdfDownloadService.js**：PDF分片下载服务
- **pdfPreviewService.js**：PDF预览服务（新增）
- **reportDetail页面**：集成下载和预览服务的报告详情页面
- **config.js**：统一配置文件

##### API接口
```
GET /v1/pdf/stream/{fileId}          # 获取PDF文件流（支持断点续传）
GET /v1/pdf/chunk/{fileId}/{chunkIndex}  # 获取PDF文件分片（加密）
GET /v1/pdf/info/{fileId}            # 获取PDF文件信息
POST /v1/pdf/cache/cleanup           # 清理过期缓存（管理员）
```

#### 3.4 配置说明

##### 环境配置
在 `utils/config.js` 中配置不同环境的API地址：

```javascript
const ENV = {
  DEV: {
    BASE_URL: 'https://yuesf.cn',    // 开发环境
    API_VERSION: 'v1',
    DEBUG: true
  },
  TEST: {
    BASE_URL: 'https://yuesf.cn',     // 测试环境
    API_VERSION: 'v1',
    DEBUG: true
  },
  PROD: {
    BASE_URL: 'https://yuesf.cn',      // 生产环境
    API_VERSION: 'v1',
    DEBUG: false
  }
};

// 修改当前环境
const CURRENT_ENV = 'DEV'; // 改为 'TEST' 或 'PROD'
```

##### 下载配置
在 `utils/pdfDownloadService.js` 中配置下载参数：

```javascript
const CONFIG = {
  CHUNK_SIZE: 1024 * 1024,           // 分片大小：1MB
  MAX_CONCURRENT_DOWNLOADS: 3,        // 最大并发数
  MAX_RETRIES: 3,                     // 最大重试次数
  MEMORY_THRESHOLD: 10 * 1024 * 1024, // 内存阈值：10MB
  CLEANUP_INTERVAL: 5 * 60 * 1000     // 清理间隔：5分钟
};
```

#### 3.5 安全注意事项

##### 密钥管理
- 加密密钥不持久化存储
- 每次请求生成新的密钥
- 定期清理过期的密钥

##### 访问控制
- 基于JWT的身份验证
- 文件访问权限验证
- 接口访问频率限制

##### 数据保护
- 不暴露内部文件路径
- 使用临时访问令牌
- 自动过期机制

#### 3.6 性能优化建议

##### 分片大小优化
- 小文件（<5MB）：使用512KB分片
- 中等文件（5-50MB）：使用1MB分片
- 大文件（>50MB）：使用2MB分片

##### 并发数调整
- 网络环境好：可增加到5个并发
- 网络环境一般：保持3个并发
- 网络环境差：减少到2个并发

##### 缓存策略
- 启用智能缓存清理
- 设置合理的缓存过期时间
- 监控内存使用情况

---

## 性能优化策略

### 1. 网络性能优化

#### 1.1 请求优化
- **请求合并**：批量请求减少网络开销
- **缓存策略**：合理使用本地缓存
- **错误重试**：网络异常时自动重试
- **超时控制**：设置合理的请求超时时间

#### 1.2 数据传输优化
- **数据压缩**：减少传输数据量
- **分页加载**：避免一次性加载大量数据
- **懒加载**：按需加载图片和内容

### 2. 内存性能优化

#### 2.1 内存管理
- **及时释放**：页面卸载时清理资源
- **避免泄漏**：防止事件监听器泄漏
- **图片优化**：合理控制图片大小和质量

#### 2.2 数据处理优化
- **分片处理**：大文件分片处理避免内存溢出
- **流式处理**：支持流式数据读取
- **垃圾回收**：及时清理不需要的数据

### 3. 渲染性能优化

#### 3.1 界面渲染
- **虚拟列表**：大量数据时使用虚拟滚动
- **条件渲染**：合理使用条件渲染
- **组件复用**：提高组件复用率

#### 3.2 动画性能
- **硬件加速**：使用transform3d启用硬件加速
- **动画优化**：避免频繁的DOM操作
- **帧率控制**：控制动画帧率

---

## 安全架构设计

### 1. 数据安全

#### 1.1 传输安全
- **HTTPS协议**：所有网络请求使用HTTPS
- **数据加密**：敏感数据加密传输
- **签名验证**：API请求签名验证

#### 1.2 存储安全
- **本地加密**：敏感数据本地加密存储
- **权限控制**：基于用户权限的数据访问控制
- **数据清理**：定期清理过期数据

### 2. 访问控制

#### 2.1 用户认证
- **微信授权**：基于微信的用户身份认证
- **Token管理**：JWT Token的生成和验证
- **会话管理**：用户会话状态管理

#### 2.2 权限管理
- **角色权限**：基于角色的访问控制
- **资源权限**：基于资源的访问控制
- **操作权限**：基于操作的权限控制

### 3. 文件安全

#### 3.1 文件访问控制
- **分片加密**：PDF文件分片加密传输
- **临时访问**：基于文件ID的临时访问控制
- **下载限制**：防止恶意下载和传播

#### 3.2 内容保护
- **水印技术**：文档水印保护
- **版权声明**：明确版权归属和使用限制
- **使用追踪**：记录文件使用情况

---

## 错误处理机制

### 1. 网络错误处理

#### 1.1 错误分类
```javascript
// 错误类型定义
const ERROR_TYPES = {
  NETWORK_ERROR: '网络连接失败',
  TIMEOUT_ERROR: '请求超时',
  SERVER_ERROR: '服务器错误',
  AUTH_ERROR: '认证失败',
  PERMISSION_ERROR: '权限不足'
}
```

#### 1.2 错误处理策略
```javascript
// 错误处理模板
async function apiCall() {
  try {
    const result = await reportAPI.methodName(params)
    
    if (result.code === 200) {
      // 处理成功情况
      return result.data
    } else {
      // 处理业务错误
      throw new Error(result.message)
    }
  } catch (error) {
    // 处理网络错误
    console.error('API调用失败:', error)
    
    // 根据错误类型提供不同处理
    if (error.message.includes('网络')) {
      // 网络错误处理
      this.handleNetworkError()
    } else if (error.message.includes('认证')) {
      // 认证错误处理
      this.handleAuthError()
    } else {
      // 其他错误处理
      this.handleGeneralError(error)
    }
  }
}
```

### 2. 用户错误提示

#### 2.1 提示策略
- **即时反馈**：操作后立即提供反馈
- **友好提示**：使用用户易懂的错误描述
- **解决建议**：提供具体的解决建议
- **重试机制**：支持用户重试操作

#### 2.2 错误恢复
- **自动重试**：网络错误时自动重试
- **降级处理**：功能异常时提供降级方案
- **状态恢复**：错误后恢复到正常状态

---

## 监控与日志

### 1. 性能监控

#### 1.1 关键指标
- **页面加载时间**：页面从加载到渲染完成的时间
- **API响应时间**：网络请求的响应时间
- **内存使用情况**：应用内存占用情况
- **错误率统计**：各类错误的出现频率

#### 1.2 监控实现
```javascript
// 性能监控示例
const performanceMonitor = {
  // 页面加载监控
  pageLoadMonitor: {
    startTime: Date.now(),
    endTime: null,
    
    start() {
      this.startTime = Date.now()
    },
    
    end() {
      this.endTime = Date.now()
      const loadTime = this.endTime - this.startTime
      console.log(`页面加载时间: ${loadTime}ms`)
      
      // 上报性能数据
      this.reportPerformance('page_load', loadTime)
    }
  },
  
  // API性能监控
  apiMonitor: {
    startTime: null,
    
    beforeRequest() {
      this.startTime = Date.now()
    },
    
    afterResponse() {
      const responseTime = Date.now() - this.startTime
      console.log(`API响应时间: ${responseTime}ms`)
      
      // 上报性能数据
      this.reportPerformance('api_response', responseTime)
    }
  }
}
```

### 2. 错误日志

#### 2.1 日志记录
- **错误信息**：详细的错误描述和堆栈信息
- **上下文信息**：错误发生时的上下文信息
- **用户行为**：用户操作路径和行为轨迹
- **系统状态**：系统运行状态和环境信息

#### 2.2 日志上报
```javascript
// 日志上报服务
const logService = {
  // 错误日志上报
  reportError(error, context = {}) {
    const logData = {
      type: 'error',
      message: error.message,
      stack: error.stack,
      context: context,
      timestamp: Date.now(),
      userAgent: wx.getSystemInfoSync()
    }
    
    // 上报到日志服务器
    this.uploadLog(logData)
  },
  
  // 性能日志上报
  reportPerformance(type, value) {
    const logData = {
      type: 'performance',
      metric: type,
      value: value,
      timestamp: Date.now()
    }
    
    // 上报到日志服务器
    this.uploadLog(logData)
  }
}
```

---

## 部署与运维

### 1. 构建部署

#### 1.1 构建流程
- **代码检查**：ESLint代码质量检查
- **资源优化**：图片压缩、代码压缩
- **版本管理**：版本号管理和发布记录
- **环境配置**：开发、测试、生产环境配置

#### 1.2 发布策略
- **灰度发布**：逐步扩大用户范围
- **回滚机制**：问题出现时快速回滚
- **监控告警**：发布后实时监控系统状态

### 2. 运维监控

#### 2.1 系统监控
- **服务可用性**：API服务可用性监控
- **性能监控**：系统性能指标监控
- **错误告警**：错误率超阈值时告警

#### 2.2 用户监控
- **用户行为**：用户使用行为分析
- **功能使用**：各功能模块使用情况
- **用户反馈**：用户反馈和问题收集

---

## 技术债务与优化方向

### 1. 当前技术债务

#### 1.1 代码质量
- **模块化程度**：部分功能耦合度较高
- **错误处理**：错误处理机制不够完善
- **测试覆盖**：单元测试覆盖率较低

#### 1.2 性能问题
- **首屏加载**：首屏加载时间较长
- **内存管理**：内存使用优化空间较大
- **网络请求**：网络请求优化不够充分

### 2. 优化方向

#### 2.1 短期优化
- **代码重构**：重构耦合度高的模块
- **性能优化**：优化首屏加载和内存使用
- **错误处理**：完善错误处理机制

#### 2.2 长期规划
- **架构升级**：考虑引入状态管理框架
- **组件化**：提高组件复用率
- **自动化**：自动化测试和部署流程
