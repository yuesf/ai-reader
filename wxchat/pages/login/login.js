const { userAPI } = require('../../utils/api.js')

Page({
  data: {
    userInfo: null,
    hasUserInfo: false,
    canIUseGetUserProfile: wx.canIUse('getUserProfile'),
    canIUseNicknameComp: wx.canIUse('input.type.nickname'),
  },

  onLoad() {
    if (wx.getUserProfile) {
      this.setData({
        canIUseGetUserProfile: true
      })
    }
  },

  getUserProfile() {
    wx.getUserProfile({
      desc: '用于完善会员资料',
      success: (res) => {
        console.log(res)
        this.setData({
          userInfo: res.userInfo,
          hasUserInfo: true
        })
        
        // 获取用户信息成功后，进行微信登录
        wx.login({
          success: async (loginRes) => {
            if (loginRes.code) {
              console.log('微信登录成功，code:', loginRes.code)
              
              // 准备发送到后端的用户数据
              const loginData = {
                code: loginRes.code,
                userInfo: res.userInfo,
                encryptedData: res.encryptedData,
                iv: res.iv
              }
              
              try {
                // 调用后端API保存用户信息
                wx.showLoading({ title: '登录中...' })
                const result = await userAPI.login(loginData)
                wx.hideLoading()
                
                if (result.code === 200) {
                  console.log('用户信息保存成功:', result.data)
                  
                  // 保存用户信息到全局和本地存储
                  const app = getApp()
                  const userInfo = {
                    ...res.userInfo,
                    openid: result.data.openid,
                    sessionKey: result.data.sessionKey
                  }
                  app.login(userInfo)
                  
                  wx.showToast({
                    title: '登录成功',
                    icon: 'success'
                  })
                  
                  // 登录成功后跳转到首页
                  setTimeout(() => {
                    wx.reLaunch({
                      url: '/pages/index/index'
                    })
                  }, 1500)
                } else {
                  wx.showToast({
                    title: result.message || '登录失败',
                    icon: 'error'
                  })
                }
              } catch (error) {
                wx.hideLoading()
                console.error('保存用户信息失败:', error)
                wx.showToast({
                  title: '登录失败，请重试',
                  icon: 'error'
                })
              }
            } else {
              console.log('微信登录失败！' + loginRes.errMsg)
              wx.showToast({
                title: '登录失败',
                icon: 'error'
              })
            }
          },
          fail: (loginErr) => {
            console.log('微信登录失败', loginErr)
            wx.showToast({
              title: '登录失败',
              icon: 'error'
            })
          }
        })
      },
      fail: (err) => {
        console.log('获取用户信息失败', err)
        wx.showToast({
          title: '需要授权才能使用',
          icon: 'none'
        })
      }
    })
  },

  wxLogin() {
    // 直接调用 getUserProfile，因为这个方法已经在用户点击事件中
    this.getUserProfile()
  },

  onGetUserInfo(e) {
    console.log(e.detail.userInfo)
    this.setData({
      userInfo: e.detail.userInfo,
      hasUserInfo: true
    })
    
    // 对于旧版本的兼容处理
    wx.login({
      success: async (loginRes) => {
        if (loginRes.code) {
          const loginData = {
            code: loginRes.code,
            userInfo: e.detail.userInfo
          }
          
          try {
            wx.showLoading({ title: '登录中...' })
            const result = await userAPI.login(loginData)
            wx.hideLoading()
            
            if (result.code === 200) {
              const app = getApp()
              const userInfo = {
                ...e.detail.userInfo,
                openid: result.data.openid,
                sessionKey: result.data.sessionKey
              }
              app.login(userInfo)
              
              wx.showToast({
                title: '登录成功',
                icon: 'success'
              })
              
              setTimeout(() => {
                wx.reLaunch({
                  url: '/pages/index/index'
                })
              }, 1500)
            } else {
              wx.showToast({
                title: result.message || '登录失败',
                icon: 'error'
              })
            }
          } catch (error) {
            wx.hideLoading()
            console.error('保存用户信息失败:', error)
            wx.showToast({
              title: '登录失败，请重试',
              icon: 'error'
            })
          }
        }
      }
    })
  },

  goToIndex() {
    wx.reLaunch({
      url: '/pages/index/index'
    })
  }
})