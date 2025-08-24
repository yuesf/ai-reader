/**
 * PDF.js 配置文件
 * 用于优化PDF预览性能和用户体验
 */

// PDF.js CDN配置
export const PDF_JS_CONFIG = {
  // 主要CDN链接
  CDN_URLS: {
    // Cloudflare CDN (推荐，速度快)
    CLOUDFLARE: 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.4.120',
    // jsDelivr CDN (备选)
    JSDELIVR: 'https://cdn.jsdelivr.net/npm/pdfjs-dist@3.4.120',
    // unpkg CDN (备选)
    UNPKG: 'https://unpkg.com/pdfjs-dist@3.4.120'
  },
  
  // 当前使用的CDN
  CURRENT_CDN: 'CLOUDFLARE',
  
  // 获取完整的CDN基础URL
  getBaseUrl() {
    return this.CDN_URLS[this.CURRENT_CDN];
  },
  
  // 获取viewer.html的完整URL
  getViewerUrl() {
    return `${this.getBaseUrl()}/web/viewer.html`;
  },
  
  // 获取worker.js的完整URL
  getWorkerUrl() {
    return `${this.getBaseUrl()}/build/pdf.worker.min.js`;
  }
};

// 快速预览模式配置
export const FAST_PREVIEW_CONFIG = {
  // 隐藏不必要的UI元素，提升性能
  HIDDEN_ELEMENTS: {
    scrollbar: '0',        // 隐藏滚动条
    toolbar: '0',          // 隐藏工具栏
    navpanes: '0',         // 隐藏导航面板
    statusbar: '0',        // 隐藏状态栏
    messages: '0',         // 隐藏消息
    print: '0',            // 隐藏打印按钮
    download: '0',         // 隐藏下载按钮
    secondaryToolbar: '0', // 隐藏次要工具栏
    findbar: '0',          // 隐藏查找栏
    sidebar: '0'           // 隐藏侧边栏
  },
  
  // 性能优化参数
  PERFORMANCE: {
    zoom: 'page-width',    // 页面宽度适配
    enableScripting: '0',  // 禁用脚本执行
    enableXfa: '0',        // 禁用XFA表单
    disableAutoFetch: '1', // 禁用自动获取
    disableStream: '1',    // 禁用流式加载
    disableRange: '1'      // 禁用范围请求
  }
};

// 完整预览模式配置
export const FULL_PREVIEW_CONFIG = {
  // 显示所有功能
  VISIBLE_ELEMENTS: {
    scrollbar: '1',
    toolbar: '1',
    navpanes: '1',
    statusbar: '1',
    messages: '1',
    print: '1',
    download: '1',
    secondaryToolbar: '1',
    findbar: '1',
    sidebar: '1'
  },
  
  // 功能完整参数
  FEATURES: {
    zoom: 'auto',          // 自动缩配
    enableScripting: '1',  // 启用脚本执行
    enableXfa: '1',        // 启用XFA表单
    disableAutoFetch: '0', // 启用自动获取
    disableStream: '0',    // 启用流式加载
    disableRange: '0'      // 启用范围请求
  }
};

// 构建预览URL的工具函数
export const buildPreviewUrl = (pdfUrl, mode = 'fast') => {
  // 对于本地文件，直接返回文件路径，不使用PDF.js viewer
  if (pdfUrl.startsWith(wx.env.USER_DATA_PATH)) {
    return pdfUrl;
  }
  
  const baseUrl = PDF_JS_CONFIG.getViewerUrl();
  const config = mode === 'fast' ? FAST_PREVIEW_CONFIG : FULL_PREVIEW_CONFIG;
  
  // 合并配置参数
  const params = {};
  params.file = pdfUrl;
  
  // 合并性能参数
  Object.keys(config.PERFORMANCE).forEach(key => {
    params[key] = config.PERFORMANCE[key];
  });
  
  // 合并UI参数
  if (mode === 'fast') {
    Object.keys(config.HIDDEN_ELEMENTS).forEach(key => {
      params[key] = config.HIDDEN_ELEMENTS[key];
    });
  } else {
    Object.keys(config.VISIBLE_ELEMENTS).forEach(key => {
      params[key] = config.VISIBLE_ELEMENTS[key];
    });
  }
  
  // 手动拼接查询参数
  const queryString = Object.entries(params)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');
  
  return `${baseUrl}?${queryString}`;
};

// 性能监控配置
export const PERFORMANCE_CONFIG = {
  // 加载超时时间 (毫秒)
  LOAD_TIMEOUT: 30000,
  
  // 重试次数
  MAX_RETRIES: 3,
  
  // 重试间隔 (毫秒)
  RETRY_INTERVAL: 2000,
  
  // 文件大小阈值 (字节)
  SIZE_THRESHOLDS: {
    SMALL: 1024 * 1024,      // 1MB
    MEDIUM: 10 * 1024 * 1024, // 10MB
    LARGE: 50 * 1024 * 1024   // 50MB
  }
};

// 错误处理配置
export const ERROR_CONFIG = {
  // 错误消息映射
  MESSAGES: {
    NETWORK_ERROR: '网络连接失败，请检查网络设置',
    TIMEOUT_ERROR: '加载超时，请稍后重试',
    FILE_ERROR: '文件加载失败，请检查文件是否损坏',
    CDN_ERROR: 'CDN服务异常，正在切换到备用服务',
    UNKNOWN_ERROR: '未知错误，请稍后重试'
  },
  
  // 错误重试策略
  RETRY_STRATEGY: {
    NETWORK_ERROR: true,    // 网络错误可重试
    TIMEOUT_ERROR: true,    // 超时错误可重试
    FILE_ERROR: false,      // 文件错误不重试
    CDN_ERROR: true,        // CDN错误可重试
    UNKNOWN_ERROR: true     // 未知错误可重试
  }
};

export default {
  PDF_JS_CONFIG,
  FAST_PREVIEW_CONFIG,
  FULL_PREVIEW_CONFIG,
  buildPreviewUrl,
  PERFORMANCE_CONFIG,
  ERROR_CONFIG
};