// app.js
const tracking = require('./utils/tracking/index.js')

App({
  onLaunch() {
    console.log('[App] 小程序启动 - onLaunch 被调用')
    
    try {
      // 初始化埋点系统
      console.log('[App] 准备调用 initTracking...')
      this.initTracking()
      console.log('[App] initTracking 调用完成')
    } catch (error) {
      console.error('[App] initTracking 调用失败:', error)
    }
    
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 检查登录状态
    this.checkLoginStatus()
  },

  // 初始化埋点系统
  initTracking() {
    try {
      console.log('[App] 开始初始化埋点系统...')
      
      // 获取用户信息用于埋点
      const userInfo = wx.getStorageSync('userInfo')
      const userId = userInfo ? userInfo.openid : null
      
      console.log('[App] 用户信息:', { userInfo, userId })
      
      // 初始化埋点系统
      const result = tracking.init({
        userId: userId,
        autoTrackPageView: true
      })
      
      console.log('[App] 埋点系统初始化结果:', result)
      console.log('[App] 埋点系统状态:', tracking.getStats())
      
      
      console.log('[App] 埋点系统初始化成功')
    } catch (error) {
      console.error('[App] 埋点系统初始化失败:', error)
      console.error('[App] 错误堆栈:', error.stack)
    }
  },

  checkLoginStatus() {
    const userInfo = wx.getStorageSync('userInfo')
    if (userInfo) {
      this.globalData.userInfo = userInfo
      this.globalData.isLoggedIn = true
    } else {
      this.globalData.isLoggedIn = false
      // 如果未登录，跳转到登录页
      wx.reLaunch({
        url: '/pages/login/login'
      })
    }
  },

  // 登录方法
  login(userInfo) {
    this.globalData.userInfo = userInfo
    this.globalData.isLoggedIn = true
    wx.setStorageSync('userInfo', userInfo)
    
    // 更新埋点系统的用户ID
    try {
      const tracking = require('./utils/tracking/index.js')
      tracking.core.userId = userInfo.openid
      console.log('[App] 埋点系统用户ID已更新:', userInfo.openid)
      
      // 立即上报登录成功事件
      tracking.trackCustomEvent('user_login_success', {
        openid: userInfo.openid,
        nickName: userInfo.nickName,
        timestamp: Date.now(),
        loginSource: 'wechat'
      })
      
      // 强制上报所有缓存的埋点事件
      tracking.upload.forceUploadAll()
      
    } catch (error) {
      console.error('[App] 更新埋点用户ID失败:', error)
    }
  },

  // 退出登录
  logout() {
    this.globalData.userInfo = null
    this.globalData.isLoggedIn = false
    wx.removeStorageSync('userInfo')
    wx.reLaunch({
      url: '/pages/login/login'
    })
  },

  globalData: {
    userInfo: null,
    isLoggedIn: false
  }
})
