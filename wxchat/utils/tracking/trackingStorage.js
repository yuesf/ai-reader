/**
 * 埋点本地存储管理模块
 * 负责埋点数据的本地缓存、存储和清理
 * 
 * @author AI-Reader Team
 * @since 2025-01-09
 */

const { TRACKING_CONFIG } = require('./trackingConfig.js')

/**
 * 埋点存储管理类
 */
class TrackingStorage {
  constructor() {
    this.isInitialized = false
    this.storageKey = TRACKING_CONFIG.cache.storageKey
    this.maxSize = TRACKING_CONFIG.cache.maxSize
    this.expireTime = TRACKING_CONFIG.cache.expireTime
  }

  /**
   * 初始化存储
   */
  init() {
    try {
      // 清理过期数据
      this.cleanupExpiredEvents()
      
      this.isInitialized = true
      console.log('[TrackingStorage] 存储初始化完成')
    } catch (error) {
      console.error('[TrackingStorage] 存储初始化失败:', error)
    }
  }

  /**
   * 添加事件到本地存储
   * @param {Object} eventData 事件数据
   */
  addEvent(eventData) {
    try {
      if (!this.isInitialized) {
        console.warn('[TrackingStorage] 存储未初始化')
        return false
      }

      // 获取现有事件列表
      const events = this.getEvents()
      
      // 添加时间戳用于过期检查
      eventData._storedAt = Date.now()
      
      // 添加新事件
      events.push(eventData)
      
      // 检查存储大小限制
      if (events.length > this.maxSize) {
        // 删除最旧的事件
        events.splice(0, events.length - this.maxSize)
        console.warn('[TrackingStorage] 存储已满，删除最旧的事件')
      }
      
      // 保存到本地存储
      wx.setStorageSync(this.storageKey, events)
      
      console.log('[TrackingStorage] 事件已添加到本地存储:', eventData.eventType)
      return true
    } catch (error) {
      console.error('[TrackingStorage] 添加事件失败:', error)
      return false
    }
  }

  /**
   * 获取所有缓存的事件
   * @returns {Array} 事件列表
   */
  getEvents() {
    try {
      const events = wx.getStorageSync(this.storageKey) || []
      return Array.isArray(events) ? events : []
    } catch (error) {
      console.error('[TrackingStorage] 获取事件失败:', error)
      return []
    }
  }

  /**
   * 获取指定数量的事件
   * @param {number} count 获取数量
   * @returns {Array} 事件列表
   */
  getEventsBatch(count = 10) {
    try {
      const events = this.getEvents()
      return events.slice(0, count)
    } catch (error) {
      console.error('[TrackingStorage] 获取批量事件失败:', error)
      return []
    }
  }

  /**
   * 删除指定数量的事件（从头部开始）
   * @param {number} count 删除数量
   */
  removeEvents(count) {
    try {
      const events = this.getEvents()
      
      if (count >= events.length) {
        // 删除所有事件
        this.clearEvents()
      } else {
        // 删除指定数量的事件
        const remainingEvents = events.slice(count)
        wx.setStorageSync(this.storageKey, remainingEvents)
      }
      
      console.log('[TrackingStorage] 已删除事件:', count)
    } catch (error) {
      console.error('[TrackingStorage] 删除事件失败:', error)
    }
  }

  /**
   * 删除指定的事件
   * @param {Array} eventsToRemove 要删除的事件列表
   */
  removeSpecificEvents(eventsToRemove) {
    try {
      const events = this.getEvents()
      
      // 创建要删除事件的标识集合
      const removeIds = new Set(eventsToRemove.map(event => 
        `${event.userId}_${event.sessionId}_${event.timestamp}`
      ))
      
      // 过滤掉要删除的事件
      const remainingEvents = events.filter(event => 
        !removeIds.has(`${event.userId}_${event.sessionId}_${event.timestamp}`)
      )
      
      wx.setStorageSync(this.storageKey, remainingEvents)
      
      console.log('[TrackingStorage] 已删除指定事件:', eventsToRemove.length)
    } catch (error) {
      console.error('[TrackingStorage] 删除指定事件失败:', error)
    }
  }

  /**
   * 清空所有事件
   */
  clearEvents() {
    try {
      wx.removeStorageSync(this.storageKey)
      console.log('[TrackingStorage] 已清空所有事件')
    } catch (error) {
      console.error('[TrackingStorage] 清空事件失败:', error)
    }
  }

  /**
   * 获取存储统计信息
   * @returns {Object} 统计信息
   */
  getStorageStats() {
    try {
      const events = this.getEvents()
      const storageInfo = wx.getStorageInfoSync()
      
      return {
        eventCount: events.length,
        maxSize: this.maxSize,
        storageUsage: storageInfo.currentSize,
        storageLimit: storageInfo.limitSize,
        oldestEvent: events.length > 0 ? events[0].timestamp : null,
        newestEvent: events.length > 0 ? events[events.length - 1].timestamp : null
      }
    } catch (error) {
      console.error('[TrackingStorage] 获取存储统计失败:', error)
      return {
        eventCount: 0,
        maxSize: this.maxSize,
        storageUsage: 0,
        storageLimit: 0,
        oldestEvent: null,
        newestEvent: null
      }
    }
  }

  /**
   * 清理过期事件
   */
  cleanupExpiredEvents() {
    try {
      const events = this.getEvents()
      const currentTime = Date.now()
      
      // 过滤掉过期的事件
      const validEvents = events.filter(event => {
        const storedAt = event._storedAt || event.timestamp
        return (currentTime - storedAt) < this.expireTime
      })
      
      if (validEvents.length !== events.length) {
        wx.setStorageSync(this.storageKey, validEvents)
        console.log('[TrackingStorage] 已清理过期事件:', events.length - validEvents.length)
      }
    } catch (error) {
      console.error('[TrackingStorage] 清理过期事件失败:', error)
    }
  }

  /**
   * 检查存储是否已满
   * @returns {boolean} 是否已满
   */
  isFull() {
    try {
      const events = this.getEvents()
      return events.length >= this.maxSize
    } catch (error) {
      console.error('[TrackingStorage] 检查存储状态失败:', error)
      return false
    }
  }

  /**
   * 检查是否有待上报的事件
   * @returns {boolean} 是否有待上报事件
   */
  hasPendingEvents() {
    try {
      const events = this.getEvents()
      return events.length > 0
    } catch (error) {
      console.error('[TrackingStorage] 检查待上报事件失败:', error)
      return false
    }
  }

  /**
   * 获取失败重试的事件
   * @returns {Array} 失败事件列表
   */
  getFailedEvents() {
    try {
      const failedKey = `${this.storageKey}_failed`
      const failedEvents = wx.getStorageSync(failedKey) || []
      return Array.isArray(failedEvents) ? failedEvents : []
    } catch (error) {
      console.error('[TrackingStorage] 获取失败事件失败:', error)
      return []
    }
  }

  /**
   * 添加失败事件到重试队列
   * @param {Array} events 失败的事件列表
   */
  addFailedEvents(events) {
    try {
      const failedKey = `${this.storageKey}_failed`
      const existingFailed = this.getFailedEvents()
      
      // 添加重试次数和时间戳
      const failedEvents = events.map(event => ({
        ...event,
        _retryCount: (event._retryCount || 0) + 1,
        _failedAt: Date.now()
      }))
      
      // 合并失败事件
      const allFailed = [...existingFailed, ...failedEvents]
      
      // 限制失败队列大小
      const maxFailedSize = Math.floor(this.maxSize / 2)
      if (allFailed.length > maxFailedSize) {
        allFailed.splice(0, allFailed.length - maxFailedSize)
      }
      
      wx.setStorageSync(failedKey, allFailed)
      
      console.log('[TrackingStorage] 已添加失败事件到重试队列:', failedEvents.length)
    } catch (error) {
      console.error('[TrackingStorage] 添加失败事件失败:', error)
    }
  }

  /**
   * 清除失败事件队列
   */
  clearFailedEvents() {
    try {
      const failedKey = `${this.storageKey}_failed`
      wx.removeStorageSync(failedKey)
      console.log('[TrackingStorage] 已清空失败事件队列')
    } catch (error) {
      console.error('[TrackingStorage] 清空失败事件队列失败:', error)
    }
  }

  /**
   * 移除已成功上报的失败事件
   * @param {Array} successEvents 成功上报的事件列表
   */
  removeSuccessfulFailedEvents(successEvents) {
    try {
      const failedEvents = this.getFailedEvents()
      
      // 创建成功事件的标识集合
      const successIds = new Set(successEvents.map(event => 
        `${event.userId}_${event.sessionId}_${event.timestamp}`
      ))
      
      // 过滤掉成功的事件
      const remainingFailed = failedEvents.filter(event => 
        !successIds.has(`${event.userId}_${event.sessionId}_${event.timestamp}`)
      )
      
      const failedKey = `${this.storageKey}_failed`
      wx.setStorageSync(failedKey, remainingFailed)
      
      console.log('[TrackingStorage] 已移除成功上报的失败事件:', successEvents.length)
    } catch (error) {
      console.error('[TrackingStorage] 移除成功失败事件失败:', error)
    }
  }

  /**
   * 定期清理任务
   */
  scheduleCleanup() {
    try {
      // 设置定期清理任务
      setInterval(() => {
        this.cleanupExpiredEvents()
      }, TRACKING_CONFIG.cache.cleanupInterval)
      
      console.log('[TrackingStorage] 定期清理任务已启动')
    } catch (error) {
      console.error('[TrackingStorage] 启动定期清理任务失败:', error)
    }
  }
}

// 创建全局实例
const trackingStorage = new TrackingStorage()

module.exports = trackingStorage