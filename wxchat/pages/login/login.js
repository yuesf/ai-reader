const { userAPI } = require('../../utils/api.js')
const tracking = require('../../utils/tracking/index.js')

Page({
  data: {
    userInfo: null,
    hasUserInfo: false,
    canIUseGetUserProfile: wx.canIUse('getUserProfile'),
    canIUseNicknameComp: wx.canIUse('input.type.nickname'),
    isAgreed: false, // 协议勾选状态
    showAgreementTip: false, // 是否显示协议提示
  },

  onLoad() {
    console.log('[Login] 页面加载开始')
    
    // 页面浏览埋点
    tracking.trackPageView('/pages/login/login', '登录页')
    
    // 登录页面访问埋点
    tracking.trackCustomEvent('login_page_visit', {
      timestamp: Date.now(),
      source: 'direct',
      userAgent: wx.getSystemInfoSync()
    })
    
    if (wx.getUserProfile) {
      this.setData({
        canIUseGetUserProfile: true
      })
    }
  },

  getUserProfile() {
    // 获取用户信息按钮点击埋点
    tracking.trackButtonClick('get_user_profile', '获取用户信息')
    
    wx.getUserProfile({
      desc: '用于完善会员资料',
      success: (res) => {
        console.log('[Login] 获取用户信息成功:', res)
        this.setData({
          userInfo: res.userInfo,
          hasUserInfo: true
        })
        
        // 用户授权成功埋点
        tracking.trackCustomEvent('user_auth_success', {
          nickName: res.userInfo.nickName,
          avatarUrl: res.userInfo.avatarUrl,
          pagePath: '/pages/login/login',
          timestamp: Date.now()
        })
        
        // 获取用户信息成功后，进行微信登录
        this.performWechatLogin(res)
      },
      fail: (err) => {
        console.log('[Login] 获取用户信息失败:', err)
        
        // 用户拒绝授权埋点
        tracking.trackCustomEvent('user_auth_denied', {
          errorMsg: err.errMsg,
          pagePath: '/pages/login/login',
          timestamp: Date.now()
        })
        
        wx.showToast({
          title: '需要授权才能使用',
          icon: 'none'
        })
      }
    })
  },

  performWechatLogin(userProfileRes) {
    // 微信登录开始埋点
    tracking.trackCustomEvent('wechat_login_start', {
      timestamp: Date.now()
    })
    
    wx.login({
      success: async (loginRes) => {
        if (loginRes.code) {
          console.log('[Login] 微信登录成功，code:', loginRes.code)
          
          // 准备发送到后端的用户数据
          const loginData = {
            code: loginRes.code,
            userInfo: userProfileRes.userInfo,
            encryptedData: userProfileRes.encryptedData,
            iv: userProfileRes.iv
          }
          
          try {
            wx.showLoading({ title: '登录中...' })
            
            // 后端登录请求开始埋点
            tracking.trackCustomEvent('backend_login_request_start', {
              timestamp: Date.now()
            })
            
            const result = await userAPI.login(loginData)
            wx.hideLoading()
            
            if (result.code === 200) {
              console.log('[Login] 用户信息保存成功:', result.data)
              
              // 登录成功埋点
              tracking.trackCustomEvent('login_success', {
                openid: result.data.openid,
                nickName: userProfileRes.userInfo.nickName,
                timestamp: Date.now(),
                loginDuration: Date.now() - this.loginStartTime
              })
              
              // 保存用户信息到全局和本地存储
              const app = getApp()
              const userInfo = {
                ...userProfileRes.userInfo,
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
                // 页面跳转埋点
                tracking.trackCustomEvent('page_redirect', {
                  from: '/pages/login/login',
                  to: '/pages/index/index',
                  reason: 'login_success',
                  timestamp: Date.now()
                })
                
                wx.reLaunch({
                  url: '/pages/index/index'
                })
              }, 1500)
            } else {
              // 登录失败埋点
              tracking.trackCustomEvent('login_failed', {
                errorCode: result.code,
                errorMessage: result.message,
                timestamp: Date.now()
              })
              
              wx.showToast({
                title: result.message || '登录失败',
                icon: 'error'
              })
            }
          } catch (error) {
            wx.hideLoading()
            console.error('[Login] 保存用户信息失败:', error)
            
            // 网络错误埋点
            tracking.trackCustomEvent('login_network_error', {
              errorMessage: error.message,
              errorStack: error.stack,
              timestamp: Date.now()
            })
            
            wx.showToast({
              title: '登录失败，请重试',
              icon: 'error'
            })
          }
        } else {
          console.log('[Login] 微信登录失败！' + loginRes.errMsg)
          
          // 微信登录失败埋点
          tracking.trackCustomEvent('wechat_login_failed', {
            errorMsg: loginRes.errMsg,
            timestamp: Date.now()
          })
          
          wx.showToast({
            title: '登录失败',
            icon: 'error'
          })
        }
      },
      fail: (loginErr) => {
        console.log('[Login] 微信登录失败:', loginErr)
        
        // 微信登录异常埋点
        tracking.trackCustomEvent('wechat_login_exception', {
          errorMsg: loginErr.errMsg,
          timestamp: Date.now()
        })
        
        wx.showToast({
          title: '登录失败',
          icon: 'error'
        })
      }
    })
  },

  wxLogin() {
    console.log('[Login] 微信登录按钮被点击')
    
    // 检查协议勾选状态
    if (!this.data.isAgreed) {
      this.setData({
        showAgreementTip: true
      })
      
      // 协议未勾选埋点
      tracking.trackCustomEvent('agreement_not_checked', {
        timestamp: Date.now(),
        action: 'login_attempt'
      })
      
      wx.showToast({
        title: '请先阅读并同意用户协议',
        icon: 'none'
      })
      return
    }
    
    // 记录登录开始时间
    this.loginStartTime = Date.now()
    
    // 微信登录按钮点击埋点
    tracking.trackButtonClick('wechat_login_button', '微信登录按钮')
    
    // 登录流程开始埋点
    tracking.trackCustomEvent('login_process_start', {
      timestamp: this.loginStartTime,
      loginMethod: 'wechat',
      agreementAccepted: true
    })
    
    // 调用获取用户信息
    this.getUserProfile()
  },

  // 切换协议勾选状态
  toggleAgreement() {
    const newAgreedState = !this.data.isAgreed
    this.setData({
      isAgreed: newAgreedState,
      showAgreementTip: false
    })
    
    // 协议勾选状态变更埋点
    tracking.trackCustomEvent('agreement_toggle', {
      timestamp: Date.now(),
      isAgreed: newAgreedState,
      action: newAgreedState ? 'check' : 'uncheck'
    })
    
    console.log('[Login] 协议勾选状态:', newAgreedState)
  },

  // 查看用户服务协议
  viewUserAgreement(e) {
    // 安全地阻止事件冒泡
    if (e && typeof e.stopPropagation === 'function') {
      e.stopPropagation()
    }
    
    // 用户协议查看埋点
    tracking.trackCustomEvent('view_user_agreement', {
      timestamp: Date.now(),
      from: 'login_page'
    })
    
    wx.navigateTo({
      url: '/pages/agreement/user-agreement'
    })
  },

  // 查看隐私政策
  viewPrivacyPolicy(e) {
    // 安全地阻止事件冒泡
    if (e && typeof e.stopPropagation === 'function') {
      e.stopPropagation()
    }
    
    // 隐私政策查看埋点
    tracking.trackCustomEvent('view_privacy_policy', {
      timestamp: Date.now(),
      from: 'login_page'
    })
    
    wx.navigateTo({
      url: '/pages/agreement/privacy-policy'
    })
  },

  onGetUserInfo(e) {
    console.log('[Login] 旧版本获取用户信息:', e.detail.userInfo)
    
    // 兼容旧版本的埋点
    tracking.trackCustomEvent('old_version_user_info', {
      hasUserInfo: !!e.detail.userInfo,
      timestamp: Date.now()
    })
    
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
              // 旧版本登录成功埋点
              tracking.trackCustomEvent('old_version_login_success', {
                openid: result.data.openid,
                timestamp: Date.now()
              })
              
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
              tracking.trackCustomEvent('old_version_login_failed', {
                errorCode: result.code,
                errorMessage: result.message,
                timestamp: Date.now()
              })
              
              wx.showToast({
                title: result.message || '登录失败',
                icon: 'error'
              })
            }
          } catch (error) {
            wx.hideLoading()
            console.error('[Login] 保存用户信息失败:', error)
            
            tracking.trackCustomEvent('old_version_login_error', {
              errorMessage: error.message,
              timestamp: Date.now()
            })
            
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
    // 跳过登录按钮点击埋点
    tracking.trackButtonClick('skip_login_button', '跳过登录')
    
    // 跳过登录事件埋点
    tracking.trackCustomEvent('skip_login', {
      timestamp: Date.now(),
      from: '/pages/login/login',
      to: '/pages/index/index'
    })
    
    wx.reLaunch({
      url: '/pages/index/index'
    })
  },

  onShow() {
    // 页面显示埋点
    tracking.trackCustomEvent('login_page_show', {
      timestamp: Date.now()
    })
  },

  onHide() {
    // 页面隐藏埋点
    tracking.trackCustomEvent('login_page_hide', {
      timestamp: Date.now()
    })
  },

  onUnload() {
    // 页面卸载埋点
    tracking.trackCustomEvent('login_page_unload', {
      timestamp: Date.now()
    })
  }
})