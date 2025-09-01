// app.js
App({
  onLaunch() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 检查登录状态
    this.checkLoginStatus()
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
