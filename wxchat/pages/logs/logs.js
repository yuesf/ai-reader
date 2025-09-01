// logs.js
const util = require('../../utils/util.js')

Page({
  data: {
    logs: []
  },
  onLoad() {
    // 检查登录状态
    if (!this.checkLoginStatus()) {
      return;
    }
    
    this.setData({
      logs: (wx.getStorageSync('logs') || []).map(log => {
        return {
          date: util.formatTime(new Date(log)),
          timeStamp: log
        }
      })
    })
  },

  checkLoginStatus() {
    const app = getApp()
    if (!app.globalData.isLoggedIn) {
      wx.reLaunch({
        url: '/pages/login/login'
      })
      return false
    }
    return true
  }
})
