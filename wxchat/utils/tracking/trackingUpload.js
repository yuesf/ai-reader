/**
 * 埋点数据上报模块
 * 负责埋点数据的网络上报、重试机制和上报策略管理
 * 
 * @author AI-Reader Team
 * @since 2025-01-09
 */

const { TRACKING_CONFIG, getConfig, isEnabled } = require('./trackingConfig.js')
const trackingStorage = require('./trackingStorage.js')

/**
 * 埋点上报管理类
 */
class TrackingUpload {
  constructor() {
    this.isInitialized = false
    this.sessionId = null
    this.userId = null
    this.uploadTimer = null
    this.retryTimer = null
    this.isUploading = false
    this.uploadQueue = []
  }

  /**
   * 初始化上报模块
   * @param {string} sessionId 会话ID
   * @param {string} userId 用户ID
   */
  init(sessionId, userId) {
    try {
      this.sessionId = sessionId
      this.userId = userId
      
      // 启动定时上报
      if (getConfig('upload.strategy') === 'batch') {
        this.startBatchUpload()
      }
      
      // 启动重试机制
      if (getConfig('upload.enableRetry')) {
        this.startRetryMechanism()
      }
      
      this.isInitialized = true
      console.log('[TrackingUpload] 上报模块初始化完成')
    } catch (error) {
      console.error('[TrackingUpload] 上报模块初始化失败:', error)
    }
  }

  /**
   * 单个事件实时上报
   * @param {Object} eventData 事件数据
   */
  uploadSingle(eventData) {
    if (!this.isInitialized) {
      console.warn('[TrackingUpload] 上报模块未初始化')
      return
    }

    if (getConfig('debug.mockUpload')) {
      console.log('[TrackingUpload] 模拟上报模式，跳过实际上报:', eventData.eventType)
      return
    }

    this.doUpload([eventData])
      .then(success => {
        if (success) {
          console.log('[TrackingUpload] 单个事件上报成功:', eventData.eventType)
        } else {
          console.warn('[TrackingUpload] 单个事件上报失败:', eventData.eventType)
          trackingStorage.addFailedEvents([eventData])
        }
      })
      .catch(error => {
        console.error('[TrackingUpload] 单个事件上报异常:', error)
        trackingStorage.addFailedEvents([eventData])
      })
  }

  /**
   * 检查并执行批量上报
   */
  checkAndUploadBatch() {
    if (!this.isInitialized || this.isUploading) {
      return
    }

    try {
      const batchSize = getConfig('upload.batchSize', 1)  // 修改默认值为1
      const events = trackingStorage.getEventsBatch(batchSize)
      
      console.log('[TrackingUpload] 检查批量上报 - 配置批量大小:', batchSize, '当前事件数:', events.length)
      
      if (events.length === 0) {
        return
      }

      // 检查是否达到批量上报条件
      if (events.length >= batchSize || this.shouldForceUpload(events)) {
        console.log('[TrackingUpload] 触发批量上报条件 - 事件数:', events.length, '批量大小:', batchSize)
        this.uploadBatch(events)
      }
    } catch (error) {
      console.error('[TrackingUpload] 检查批量上报失败:', error)
    }
  }

  /**
   * 批量上报事件
   * @param {Array} events 事件列表
   */
  uploadBatch(events) {
    if (!events || events.length === 0) {
      return
    }

    if (getConfig('debug.mockUpload')) {
      console.log('[TrackingUpload] 模拟批量上报模式，跳过实际上报:', events.length)
      trackingStorage.removeEvents(events.length)
      return
    }

    this.doUpload(events)
      .then(success => {
        if (success) {
          console.log('[TrackingUpload] 批量事件上报成功:', events.length)
          trackingStorage.removeEvents(events.length)
        } else {
          console.warn('[TrackingUpload] 批量事件上报失败:', events.length)
          trackingStorage.addFailedEvents(events)
        }
      })
      .catch(error => {
        console.error('[TrackingUpload] 批量事件上报异常:', error)
        trackingStorage.addFailedEvents(events)
      })
  }

  /**
   * 强制上报所有缓存事件
   */
  forceUploadAll() {
    if (!this.isInitialized) {
      return
    }

    try {
      const allEvents = trackingStorage.getEvents()
      if (allEvents.length > 0) {
        console.log('[TrackingUpload] 强制上报所有缓存事件:', allEvents.length)
        this.uploadBatch(allEvents)
      }

      // 同时上报失败队列中的事件
      const failedEvents = trackingStorage.getFailedEvents()
      if (failedEvents.length > 0) {
        console.log('[TrackingUpload] 强制上报失败队列事件:', failedEvents.length)
        this.retryFailedUploads()
      }
    } catch (error) {
      console.error('[TrackingUpload] 强制上报失败:', error)
    }
  }

  /**
   * 重试失败的上报
   */
  retryFailedUploads() {
    if (!this.isInitialized || this.isUploading) {
      return
    }

    try {
      const failedEvents = trackingStorage.getFailedEvents()
      if (failedEvents.length === 0) {
        return
      }

      // 过滤掉重试次数过多的事件
      const maxRetries = getConfig('upload.maxRetries', 3)
      const retryableEvents = failedEvents.filter(event => 
        (event._retryCount || 0) < maxRetries
      )

      if (retryableEvents.length === 0) {
        console.log('[TrackingUpload] 没有可重试的事件')
        return
      }

      console.log('[TrackingUpload] 重试失败事件:', retryableEvents.length)

      this.doUpload(retryableEvents)
        .then(success => {
          if (success) {
            console.log('[TrackingUpload] 失败事件重试成功:', retryableEvents.length)
            trackingStorage.removeSuccessfulFailedEvents(retryableEvents)
          } else {
            console.warn('[TrackingUpload] 失败事件重试失败:', retryableEvents.length)
            // 增加重试次数
            trackingStorage.addFailedEvents(retryableEvents)
          }
        })
        .catch(error => {
          console.error('[TrackingUpload] 失败事件重试异常:', error)
          trackingStorage.addFailedEvents(retryableEvents)
        })
    } catch (error) {
      console.error('[TrackingUpload] 重试失败事件异常:', error)
    }
  }

  /**
   * 执行实际的网络上报
   * @param {Array} events 事件列表
   * @returns {Promise<boolean>} 上报是否成功
   */
  doUpload(events) {
    return new Promise((resolve, reject) => {
      if (this.isUploading) {
        resolve(false)
        return
      }

      this.isUploading = true

      try {
        // 准备上报数据
        const uploadData = this.prepareUploadData(events)
        const apiUrl = this.getUploadUrl(events.length > 1)
        
        console.log('[TrackingUpload] 开始上报:', {
          url: apiUrl,
          eventCount: events.length,
          isBatch: events.length > 1
        })

        // 发起网络请求
        console.log('[TrackingUpload] 准备发送网络请求:', {
          url: apiUrl,
          method: 'POST',
          dataSize: JSON.stringify(uploadData).length,
          eventCount: events.length
        })
        
        wx.request({
          url: apiUrl,
          method: 'POST',
          data: uploadData,
          header: {
            'Content-Type': 'application/json'
          },
          timeout: getConfig('upload.timeout', 10000),
          success: (res) => {
            this.isUploading = false
            console.log('[TrackingUpload] 网络请求成功响应:', {
              statusCode: res.statusCode,
              data: res.data
            })
            
            if (res.statusCode === 200 && res.data && res.data.code === 200) {
              console.log('[TrackingUpload] 上报成功:', res.data.message)
              resolve(true)
            } else {
              console.warn('[TrackingUpload] 上报失败:', res.statusCode, res.data)
              resolve(false)
            }
          },
          fail: (error) => {
            this.isUploading = false
            console.error('[TrackingUpload] 上报请求失败:', error)
            resolve(false)
          }
        })
      } catch (error) {
        this.isUploading = false
        console.error('[TrackingUpload] 准备上报数据失败:', error)
        reject(error)
      }
    })
  }

  /**
   * 准备上报数据
   * @param {Array} events 事件列表
   * @returns {Object} 上报数据
   */
  prepareUploadData(events) {
    console.log('[TrackingUpload] 准备上报数据，事件数量:', events.length)
    
    if (events.length === 1) {
      // 单个事件上报
      const cleanedEvent = this.cleanEventData(events[0])
      console.log('[TrackingUpload] 单个事件上报数据:', {
        eventType: cleanedEvent.eventType,
        pagePath: cleanedEvent.pagePath
      })
      return cleanedEvent
    } else {
      // 批量事件上报
      const cleanedEvents = events.map(event => this.cleanEventData(event))
      
      // 统计批量上报中的pagePath情况
      const pagePathStats = cleanedEvents.reduce((stats, event) => {
        const path = event.pagePath || 'empty'
        stats[path] = (stats[path] || 0) + 1
        return stats
      }, {})
      
      console.log('[TrackingUpload] 批量事件上报pagePath统计:', pagePathStats)
      
      const batchData = {
        batchId: this.generateBatchId(),
        events: cleanedEvents,
        uploadTime: Date.now()
      }
      
      console.log('[TrackingUpload] 批量上报数据准备完成，批次ID:', batchData.batchId)
      return batchData
    }
  }

  /**
   * 清理事件数据
   * @param {Object} event 原始事件数据
   * @returns {Object} 清理后的事件数据
   */
  cleanEventData(event) {
    // 移除内部字段
    const cleanEvent = { ...event }
    delete cleanEvent._storedAt
    delete cleanEvent._retryCount
    delete cleanEvent._failedAt
    
    // 验证并修正 pagePath
    if (!cleanEvent.pagePath || cleanEvent.pagePath.trim() === '') {
      console.warn('[TrackingUpload] 批量上报发现空的pagePath，尝试修正:', cleanEvent.eventType)
      
      // 尝试从当前页面获取路径
      cleanEvent.pagePath = this.getCurrentValidPagePath()
      
      console.log('[TrackingUpload] pagePath已修正为:', cleanEvent.pagePath)
    }
    
    return cleanEvent
  }

  /**
   * 获取当前有效的页面路径（用于批量上报时修正空值）
   * @returns {string} 页面路径
   */
  getCurrentValidPagePath() {
    try {
      // 尝试从小程序页面栈获取
      const pages = getCurrentPages()
      if (pages && pages.length > 0) {
        const currentPage = pages[pages.length - 1]
        if (currentPage) {
          let pagePath = currentPage.route || currentPage.__route__ || ''
          
          // 确保路径以 / 开头
          if (pagePath && !pagePath.startsWith('/')) {
            pagePath = '/' + pagePath
          }
          
          if (pagePath) {
            return pagePath
          }
        }
      }
    } catch (error) {
      console.warn('[TrackingUpload] 获取当前页面路径失败:', error)
    }
    
    // 最后的备用方案
    return '/unknown'
  }

  /**
   * 获取上报URL
   * @param {boolean} isBatch 是否批量上报
   * @returns {string} 上报URL
   */
  getUploadUrl(isBatch) {
    const baseUrl = getConfig('api.baseUrl')
    const endpoint = isBatch ? 
      getConfig('api.batchEndpoint') : 
      getConfig('api.reportEndpoint')
    
    return `${baseUrl}${endpoint}`
  }

  /**
   * 生成批次ID
   * @returns {string} 批次ID
   */
  generateBatchId() {
    const timestamp = Date.now()
    const random = Math.random().toString(36).substr(2, 6)
    return `batch_${timestamp}_${random}`
  }

  /**
   * 判断是否应该强制上报
   * @param {Array} events 事件列表
   * @returns {boolean} 是否强制上报
   */
  shouldForceUpload(events) {
    if (events.length === 0) {
      return false
    }

    // 检查最旧事件的时间
    const oldestEvent = events[0]
    const maxWaitTime = getConfig('upload.uploadInterval', 1000)  // 修改默认值为1秒
    const currentTime = Date.now()
    
    const shouldForce = (currentTime - oldestEvent.timestamp) > maxWaitTime
    console.log('[TrackingUpload] 检查强制上报 - 等待时间:', maxWaitTime, '事件年龄:', currentTime - oldestEvent.timestamp, '是否强制:', shouldForce)
    
    return shouldForce
  }

  /**
   * 启动批量上报定时器
   */
  startBatchUpload() {
    try {
      const uploadInterval = getConfig('upload.uploadInterval', 1000)  // 修改默认值为1秒
      
      this.uploadTimer = setInterval(() => {
        console.log('[TrackingUpload] 定时器触发批量上报检查')
        this.checkAndUploadBatch()
      }, uploadInterval)
      
      console.log('[TrackingUpload] 批量上报定时器已启动, 间隔:', uploadInterval, 'ms')
    } catch (error) {
      console.error('[TrackingUpload] 启动批量上报定时器失败:', error)
    }
  }

  /**
   * 启动重试机制
   */
  startRetryMechanism() {
    try {
      const retryDelay = getConfig('upload.retryDelay', 5000)
      
      this.retryTimer = setInterval(() => {
        this.retryFailedUploads()
      }, retryDelay)
      
      console.log('[TrackingUpload] 重试机制已启动, 间隔:', retryDelay)
    } catch (error) {
      console.error('[TrackingUpload] 启动重试机制失败:', error)
    }
  }

  /**
   * 停止所有定时器
   */
  stopTimers() {
    try {
      if (this.uploadTimer) {
        clearInterval(this.uploadTimer)
        this.uploadTimer = null
        console.log('[TrackingUpload] 批量上报定时器已停止')
      }
      
      if (this.retryTimer) {
        clearInterval(this.retryTimer)
        this.retryTimer = null
        console.log('[TrackingUpload] 重试定时器已停止')
      }
    } catch (error) {
      console.error('[TrackingUpload] 停止定时器失败:', error)
    }
  }

  /**
   * 获取上报统计信息
   * @returns {Object} 统计信息
   */
  getUploadStats() {
    try {
      const storageStats = trackingStorage.getStorageStats()
      const failedEvents = trackingStorage.getFailedEvents()
      
      return {
        isInitialized: this.isInitialized,
        isUploading: this.isUploading,
        strategy: getConfig('upload.strategy'),
        batchSize: getConfig('upload.batchSize'),
        uploadInterval: getConfig('upload.uploadInterval'),
        pendingEvents: storageStats.eventCount,
        failedEvents: failedEvents.length,
        sessionId: this.sessionId,
        userId: this.userId
      }
    } catch (error) {
      console.error('[TrackingUpload] 获取上报统计失败:', error)
      return {
        isInitialized: this.isInitialized,
        isUploading: this.isUploading,
        pendingEvents: 0,
        failedEvents: 0
      }
    }
  }

  /**
   * 销毁上报模块
   */
  destroy() {
    try {
      // 强制上报所有缓存事件
      this.forceUploadAll()
      
      // 停止所有定时器
      this.stopTimers()
      
      // 重置状态
      this.isInitialized = false
      this.sessionId = null
      this.userId = null
      this.isUploading = false
      this.uploadQueue = []
      
      console.log('[TrackingUpload] 上报模块已销毁')
    } catch (error) {
      console.error('[TrackingUpload] 销毁上报模块失败:', error)
    }
  }
}

// 创建全局实例
const trackingUpload = new TrackingUpload()

module.exports = trackingUpload