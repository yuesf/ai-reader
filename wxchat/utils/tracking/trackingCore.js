/**
 * 埋点核心功能模块
 * 提供埋点数据收集、会话管理、设备信息获取等核心功能
 * 
 * @author AI-Reader Team
 * @since 2025-01-09
 */

const { TRACKING_CONFIG } = require('./trackingConfig.js')
const trackingStorage = require('./trackingStorage.js')
const trackingUpload = require('./trackingUpload.js')

/**
 * 埋点核心类
 */
class TrackingCore {
  constructor() {
    this.isInitialized = false
    this.sessionId = null
    this.userId = null
    this.deviceInfo = null
    this.networkType = null
    this.pageStartTime = null
    this.currentPage = null
  }

  /**
   * 初始化埋点系统
   * @param {Object} config 配置参数
   * @param {string} config.userId 用户ID
   * @param {boolean} config.autoTrackPageView 是否自动追踪页面浏览
   */
  init(config = {}) {
    try {
      console.log('[Tracking] 初始化埋点系统...')
      
      // 生成会话ID
      this.sessionId = this.generateSessionId()
      this.userId = config.userId || this.generateAnonymousUserId()
      
      // 获取设备信息
      this.collectDeviceInfo()
      
      // 获取网络信息
      this.collectNetworkInfo()
      
      // 初始化存储
      trackingStorage.init()
      
      // 初始化上报模块
      trackingUpload.init(this.sessionId, this.userId)
      
      // 监听网络状态变化
      this.listenNetworkChange()
      
      // 监听页面显示/隐藏
      this.listenPageVisibility()
      
      this.isInitialized = true
      console.log('[Tracking] 埋点系统初始化完成', {
        sessionId: this.sessionId,
        userId: this.userId
      })
      
      // 上报初始化事件
      this.trackCustomEvent('tracking_init', {
        version: TRACKING_CONFIG.version,
        config: config
      })
      
    } catch (error) {
      console.error('[Tracking] 初始化失败:', error)
    }
  }

  /**
   * 页面浏览埋点
   * @param {string} pagePath 页面路径
   * @param {string} pageTitle 页面标题
   * @param {Object} extraProps 额外属性
   */
  trackPageView(pagePath, pageTitle = '', extraProps = {}) {
    if (!this.isInitialized) {
      console.warn('[Tracking] 埋点系统未初始化')
      return
    }

    try {
      // 结束上一个页面的访问
      if (this.currentPage && this.pageStartTime) {
        const duration = Date.now() - this.pageStartTime
        this.trackCustomEvent('page_leave', {
          pagePath: this.currentPage,
          duration: duration
        })
      }

      // 开始新页面访问
      this.currentPage = pagePath
      this.pageStartTime = Date.now()

      const eventData = {
        userId: this.userId,
        sessionId: this.sessionId,
        eventType: 'page_view',
        pagePath: pagePath,
        elementId: null,
        elementText: pageTitle,
        properties: {
          pageTitle: pageTitle,
          referrer: this.currentPage,
          ...extraProps
        },
        timestamp: Date.now(),
        deviceInfo: this.deviceInfo,
        networkType: this.networkType
      }

      this.reportEvent(eventData)
      
      console.log('[Tracking] 页面浏览埋点:', pagePath)
    } catch (error) {
      console.error('[Tracking] 页面浏览埋点失败:', error)
    }
  }

  /**
   * 按钮点击埋点
   * @param {string} elementId 元素ID
   * @param {string} elementText 元素文本
   * @param {Object} extraProps 额外属性
   */
  trackButtonClick(elementId, elementText = '', extraProps = {}) {
    if (!this.isInitialized) {
      console.warn('[Tracking] 埋点系统未初始化')
      return
    }

    try {
      const eventData = {
        userId: this.userId,
        sessionId: this.sessionId,
        eventType: 'button_click',
        pagePath: this.currentPage || getCurrentPagePath(),
        elementId: elementId,
        elementText: elementText,
        properties: {
          clickTime: Date.now(),
          ...extraProps
        },
        timestamp: Date.now(),
        deviceInfo: this.deviceInfo,
        networkType: this.networkType
      }

      this.reportEvent(eventData)
      
      console.log('[Tracking] 按钮点击埋点:', elementId, elementText)
    } catch (error) {
      console.error('[Tracking] 按钮点击埋点失败:', error)
    }
  }

  /**
   * 自定义事件埋点
   * @param {string} eventType 事件类型
   * @param {Object} properties 事件属性
   */
  trackCustomEvent(eventType, properties = {}) {
    console.log('[Tracking] 自定义事件埋点:', eventType, properties)
    
    if (!this.isInitialized) {
      console.warn('[Tracking] 埋点系统未初始化')
      return
    }

    try {
      const eventData = {
        userId: this.userId,
        sessionId: this.sessionId,
        eventType: eventType,
        pagePath: this.currentPage || getCurrentPagePath(),
        elementId: properties.elementId || null,
        elementText: properties.elementText || null,
        properties: properties,
        timestamp: Date.now(),
        deviceInfo: this.deviceInfo,
        networkType: this.networkType
      }

      this.reportEvent(eventData)
      
      console.log('[Tracking] 自定义事件埋点:', eventType)
    } catch (error) {
      console.error('[Tracking] 自定义事件埋点失败:', error)
    }
  }

  /**
   * 上报事件
   * @param {Object} eventData 事件数据
   */
  reportEvent(eventData) {
    try {
      // 数据验证
      if (!this.validateEventData(eventData)) {
        console.warn('[Tracking] 事件数据验证失败:', eventData)
        return
      }

      // 存储到本地
      trackingStorage.addEvent(eventData)

      // 根据配置决定上报策略
      if (TRACKING_CONFIG.upload.strategy === 'real-time') {
        // 实时上报
        trackingUpload.uploadSingle(eventData)
      } else {
        // 批量上报
        trackingUpload.checkAndUploadBatch()
      }
    } catch (error) {
      console.error('[Tracking] 上报事件失败:', error)
    }
  }

  /**
   * 获取用户会话信息
   */
  getSessionInfo() {
    return {
      sessionId: this.sessionId,
      userId: this.userId,
      startTime: this.pageStartTime,
      currentPage: this.currentPage
    }
  }

  /**
   * 获取设备信息
   */
  getDeviceInfo() {
    return this.deviceInfo
  }

  /**
   * 结束会话
   */
  endSession() {
    try {
      if (this.currentPage && this.pageStartTime) {
        const duration = Date.now() - this.pageStartTime
        this.trackCustomEvent('page_leave', {
          pagePath: this.currentPage,
          duration: duration
        })
      }

      this.trackCustomEvent('session_end', {
        sessionDuration: Date.now() - this.pageStartTime
      })

      // 强制上报所有缓存的事件
      trackingUpload.forceUploadAll()

      console.log('[Tracking] 会话结束')
    } catch (error) {
      console.error('[Tracking] 结束会话失败:', error)
    }
  }

  /**
   * 生成会话ID
   */
  generateSessionId() {
    const timestamp = Date.now()
    const random = Math.random().toString(36).substr(2, 9)
    return `session_${timestamp}_${random}`
  }

  /**
   * 生成匿名用户ID
   */
  generateAnonymousUserId() {
    let userId = wx.getStorageSync('tracking_anonymous_user_id')
    if (!userId) {
      const timestamp = Date.now()
      const random = Math.random().toString(36).substr(2, 9)
      userId = `anonymous_${timestamp}_${random}`
      wx.setStorageSync('tracking_anonymous_user_id', userId)
    }
    return userId
  }

  /**
   * 收集设备信息
   */
  collectDeviceInfo() {
    try {
      const systemInfo = wx.getSystemInfoSync()
      this.deviceInfo = {
        brand: systemInfo.brand,
        model: systemInfo.model,
        system: systemInfo.system,
        platform: systemInfo.platform,
        version: systemInfo.version,
        SDKVersion: systemInfo.SDKVersion,
        screenWidth: systemInfo.screenWidth,
        screenHeight: systemInfo.screenHeight,
        windowWidth: systemInfo.windowWidth,
        windowHeight: systemInfo.windowHeight,
        pixelRatio: systemInfo.pixelRatio,
        language: systemInfo.language,
        wifiEnabled: systemInfo.wifiEnabled,
        locationEnabled: systemInfo.locationEnabled,
        bluetoothEnabled: systemInfo.bluetoothEnabled,
        cameraAuthorized: systemInfo.cameraAuthorized,
        locationAuthorized: systemInfo.locationAuthorized,
        microphoneAuthorized: systemInfo.microphoneAuthorized,
        notificationAuthorized: systemInfo.notificationAuthorized
      }
    } catch (error) {
      console.error('[Tracking] 获取设备信息失败:', error)
      this.deviceInfo = {}
    }
  }

  /**
   * 收集网络信息
   */
  collectNetworkInfo() {
    try {
      wx.getNetworkType({
        success: (res) => {
          this.networkType = res.networkType
        },
        fail: (error) => {
          console.error('[Tracking] 获取网络信息失败:', error)
          this.networkType = 'unknown'
        }
      })
    } catch (error) {
      console.error('[Tracking] 获取网络信息失败:', error)
      this.networkType = 'unknown'
    }
  }

  /**
   * 监听网络状态变化
   */
  listenNetworkChange() {
    wx.onNetworkStatusChange((res) => {
      this.networkType = res.networkType
      
      // 网络恢复时尝试上报缓存的事件
      if (res.isConnected) {
        trackingUpload.retryFailedUploads()
      }
    })
  }

  /**
   * 监听页面显示/隐藏
   */
  listenPageVisibility() {
    // 小程序进入后台
    wx.onAppHide(() => {
      this.trackCustomEvent('app_hide', {
        hideTime: Date.now()
      })
      trackingUpload.forceUploadAll()
    })

    // 小程序进入前台
    wx.onAppShow(() => {
      this.trackCustomEvent('app_show', {
        showTime: Date.now()
      })
    })
  }

  /**
   * 验证事件数据
   * @param {Object} eventData 事件数据
   */
  validateEventData(eventData) {
    if (!eventData.userId || !eventData.sessionId || !eventData.eventType) {
      return false
    }
    if (!eventData.timestamp || eventData.timestamp <= 0) {
      return false
    }
    return true
  }
}

/**
 * 获取当前页面路径
 */
function getCurrentPagePath() {
  try {
    const pages = getCurrentPages()
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1]
      return currentPage.route || ''
    }
  } catch (error) {
    console.error('[Tracking] 获取当前页面路径失败:', error)
  }
  return ''
}

// 创建全局实例
const trackingCore = new TrackingCore()

module.exports = trackingCore