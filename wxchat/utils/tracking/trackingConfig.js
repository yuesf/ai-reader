/**
 * 埋点配置文件
 * 定义埋点系统的各项配置参数
 * 
 * @author AI-Reader Team
 * @since 2025-01-09
 */

/**
 * 埋点配置
 */
const TRACKING_CONFIG = {
  // 版本信息
  version: '1.0.0',
  
  // API配置
  api: {
    baseUrl: require('../config.js').getBaseUrl(),
    reportEndpoint: '/v1/tracking/report',
    batchEndpoint: '/v1/tracking/report/batch',
    sessionEndpoint: '/v1/tracking/session/end',
    healthEndpoint: '/v1/tracking/health'
  },
  
  // 上报策略配置
  upload: {
    strategy: 'batch',      // 上报策略: 'real-time' | 'batch' - 改回实时上报
    batchSize: 10,               // 批量上报大小 - 改为10个
    uploadInterval: 10000,       // 上报间隔(毫秒) - 改为10秒
    maxRetries: 2,              // 最大重试次数
    retryDelay: 2000,           // 重试延迟(毫秒) - 改为2秒
    timeout: 10000,             // 请求超时时间(毫秒) - 10秒
    enableRetry: true           // 是否启用重试
  },
  
  // 缓存配置
  cache: {
    maxSize: 100,               // 最大缓存条数
    expireTime: 24 * 60 * 60 * 1000, // 过期时间(毫秒) - 24小时
    storageKey: 'tracking_events',    // 存储键名
    cleanupInterval: 60 * 60 * 1000   // 清理间隔(毫秒) - 1小时
  },
  
  // 埋点开关配置
  switches: {
    enablePageView: true,        // 启用页面浏览埋点
    enableButtonClick: true,     // 启用按钮点击埋点
    enableCustomEvent: true,     // 启用自定义事件埋点
    enableDeviceInfo: true,      // 启用设备信息收集
    enableNetworkInfo: true,     // 启用网络信息收集
    enableErrorTracking: true,   // 启用错误追踪
    enablePerformance: false     // 启用性能监控
  },
  
  // 调试配置
  debug: {
    enabled: true,              // 是否启用调试模式
    logLevel: 'info',           // 日志级别: 'error' | 'warn' | 'info' | 'debug'
    showConsole: true,          // 是否显示控制台日志
    mockUpload: false,          // 是否模拟上报(不实际发送请求)
    logUploadDetails: true,     // 是否记录上报详细信息
    logStorageDetails: true     // 是否记录存储详细信息
  },
  
  // 事件类型定义
  eventTypes: {
    PAGE_VIEW: 'page_view',
    BUTTON_CLICK: 'button_click',
    SEARCH: 'search',
    DOWNLOAD: 'download',
    SHARE: 'share',
    LOGIN: 'login',
    LOGOUT: 'logout',
    ERROR: 'error',
    PERFORMANCE: 'performance',
    CUSTOM: 'custom'
  },
  
  // 页面路径映射
  pageMapping: {
    '/pages/index/index': '首页',
    '/pages/reportDetail/reportDetail': '报告详情页',
    '/pages/pdfPreview/pdfPreview': 'PDF预览页',
    '/pages/login/login': '登录页',
    '/pages/logs/logs': '日志页'
  },
  
  // 元素ID映射
  elementMapping: {
    // 首页元素
    'search_btn': '搜索按钮',
    'search_input': '搜索输入框',
    'report_card': '报告卡片',
    'filter_btn': '筛选按钮',
    'refresh_btn': '刷新按钮',
    
    // 报告详情页元素
    'preview_btn': '预览按钮',
    'download_btn': '下载按钮',
    'share_btn': '分享按钮',
    'back_btn': '返回按钮',
    'tab_intro': '简介标签',
    'tab_preview': '预览标签',
    'tab_download': '下载标签',
    
    // PDF预览页元素
    'zoom_in_btn': '放大按钮',
    'zoom_out_btn': '缩小按钮',
    'zoom_reset_btn': '重置缩放按钮',
    'prev_page_btn': '上一页按钮',
    'next_page_btn': '下一页按钮',
    'download_confirm_btn': '确认下载按钮',
    
    // 登录页元素
    'login_btn': '登录按钮',
    'auth_btn': '授权按钮',
    'skip_btn': '跳过按钮'
  },
  
  // 数据验证规则
  validation: {
    maxEventSize: 10 * 1024,    // 单个事件最大大小(字节) - 10KB
    maxPropertyLength: 1000,     // 属性值最大长度
    maxElementTextLength: 100,   // 元素文本最大长度
    requiredFields: ['userId', 'sessionId', 'eventType', 'pagePath', 'timestamp']
  }
}

/**
 * 获取配置项
 * @param {string} key 配置键路径，支持点分隔符
 * @param {*} defaultValue 默认值
 * @returns {*} 配置值
 */
function getConfig(key, defaultValue = null) {
  try {
    const keys = key.split('.')
    let value = TRACKING_CONFIG
    
    for (const k of keys) {
      if (value && typeof value === 'object' && k in value) {
        value = value[k]
      } else {
        return defaultValue
      }
    }
    
    return value
  } catch (error) {
    console.error('[TrackingConfig] 获取配置失败:', error)
    return defaultValue
  }
}

/**
 * 设置配置项
 * @param {string} key 配置键路径
 * @param {*} value 配置值
 */
function setConfig(key, value) {
  try {
    const keys = key.split('.')
    let target = TRACKING_CONFIG
    
    for (let i = 0; i < keys.length - 1; i++) {
      const k = keys[i]
      if (!target[k] || typeof target[k] !== 'object') {
        target[k] = {}
      }
      target = target[k]
    }
    
    target[keys[keys.length - 1]] = value
  } catch (error) {
    console.error('[TrackingConfig] 设置配置失败:', error)
  }
}

/**
 * 获取元素显示名称
 * @param {string} elementId 元素ID
 * @returns {string} 显示名称
 */
function getElementName(elementId) {
  return TRACKING_CONFIG.elementMapping[elementId] || elementId
}

/**
 * 获取页面显示名称
 * @param {string} pagePath 页面路径
 * @returns {string} 显示名称
 */
function getPageName(pagePath) {
  return TRACKING_CONFIG.pageMapping[pagePath] || pagePath
}

/**
 * 检查功能开关
 * @param {string} switchName 开关名称
 * @returns {boolean} 是否启用
 */
function isEnabled(switchName) {
  return getConfig(`switches.${switchName}`, false)
}

module.exports = {
  TRACKING_CONFIG,
  getConfig,
  setConfig,
  getElementName,
  getPageName,
  isEnabled
}