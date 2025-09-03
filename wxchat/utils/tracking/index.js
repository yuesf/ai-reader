/**
 * 埋点系统入口文件
 * 统一导出埋点相关的所有模块和工具函数
 * 
 * @author AI-Reader Team
 * @since 2025-01-09
 */

const trackingCore = require('./trackingCore.js')
const { TRACKING_CONFIG, getConfig, setConfig, getElementName, getPageName, isEnabled } = require('./trackingConfig.js')
const trackingStorage = require('./trackingStorage.js')
const trackingUpload = require('./trackingUpload.js')

/**
 * 埋点系统统一接口 - 直接导出核心功能
 */
module.exports = {
  // 核心功能 - 直接暴露trackingCore的方法
  init: trackingCore.init.bind(trackingCore),
  trackPageView: trackingCore.trackPageView.bind(trackingCore),
  trackButtonClick: trackingCore.trackButtonClick.bind(trackingCore),
  trackCustomEvent: trackingCore.trackCustomEvent.bind(trackingCore),
  getSessionInfo: trackingCore.getSessionInfo.bind(trackingCore),
  getDeviceInfo: trackingCore.getDeviceInfo.bind(trackingCore),
  endSession: trackingCore.endSession.bind(trackingCore),
  
  // 核心对象引用
  core: trackingCore,
  
  // 配置管理
  config: {
    get: getConfig,
    set: setConfig,
    getElementName: getElementName,
    getPageName: getPageName,
    isEnabled: isEnabled,
    TRACKING_CONFIG: TRACKING_CONFIG
  },
  
  // 存储管理
  storage: trackingStorage,
  
  // 上报管理
  upload: trackingUpload,
  
  /**
   * 获取系统统计信息
   */
  getStats() {
    return {
      session: trackingCore.getSessionInfo(),
      storage: trackingStorage.getStorageStats(),
      upload: trackingUpload.getUploadStats()
    }
  },
  
  // 便捷函数
  trackPage(pagePath, pageTitle) {
    return trackingCore.trackPageView(pagePath, pageTitle)
  },
  
  trackButton(buttonId, buttonText) {
    return trackingCore.trackButtonClick(buttonId, buttonText)
  },
  
  trackSearch(keyword, extraProps = {}) {
    return trackingCore.trackCustomEvent('search', {
      keyword: keyword,
      ...extraProps
    })
  },
  
  trackDownload(fileId, fileName, extraProps = {}) {
    return trackingCore.trackCustomEvent('download', {
      fileId: fileId,
      fileName: fileName,
      ...extraProps
    })
  },
  
  trackShare(shareType, shareTarget, extraProps = {}) {
    return trackingCore.trackCustomEvent('share', {
      shareType: shareType,
      shareTarget: shareTarget,
      ...extraProps
    })
  },
  
  trackLogin(loginType, success, extraProps = {}) {
    return trackingCore.trackCustomEvent('login', {
      loginType: loginType,
      success: success,
      ...extraProps
    })
  },
  
  trackError(errorType, errorMessage, extraProps = {}) {
    return trackingCore.trackCustomEvent('error', {
      errorType: errorType,
      errorMessage: errorMessage,
      ...extraProps
    })
  }
}