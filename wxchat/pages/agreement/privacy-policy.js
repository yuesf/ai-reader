const tracking = require('../../utils/tracking/index.js')

Page({
  data: {
    
  },

  onLoad(options) {
    console.log('[PrivacyPolicy] 页面加载')
    
    // 页面浏览埋点
    tracking.trackPageView('/pages/agreement/privacy-policy', '隐私政策页')
    
    // 协议页面访问埋点
    tracking.trackCustomEvent('agreement_page_visit', {
      agreementType: 'privacy_policy',
      timestamp: Date.now(),
      from: options.from || 'unknown'
    })
  },

  // 同意协议并返回
  agreeAndReturn() {
    console.log('[PrivacyPolicy] 用户同意隐私政策')
    
    // 协议同意埋点
    tracking.trackCustomEvent('agreement_accepted', {
      agreementType: 'privacy_policy',
      timestamp: Date.now(),
      action: 'button_click'
    })
    
    // 获取当前页面栈
    const pages = getCurrentPages()
    if (pages.length >= 2) {
      const prevPage = pages[pages.length - 2]
      
      // 如果上一页是登录页，则设置协议同意状态
      if (prevPage.route === 'pages/login/login') {
        prevPage.setData({
          isAgreed: true,
          showAgreementTip: false
        })
        
        // 协议状态同步埋点
        tracking.trackCustomEvent('agreement_state_sync', {
          agreementType: 'privacy_policy',
          targetPage: 'login',
          timestamp: Date.now()
        })
      }
    }
    
    wx.showToast({
      title: '已同意协议',
      icon: 'success',
      duration: 1500
    })
    
    // 返回上一页
    setTimeout(() => {
      wx.navigateBack()
    }, 1500)
  },

  onShow() {
    // 页面显示埋点
    tracking.trackCustomEvent('agreement_page_show', {
      agreementType: 'privacy_policy',
      timestamp: Date.now()
    })
  },

  onHide() {
    // 页面隐藏埋点
    tracking.trackCustomEvent('agreement_page_hide', {
      agreementType: 'privacy_policy',
      timestamp: Date.now()
    })
  },

  onUnload() {
    // 页面卸载埋点
    tracking.trackCustomEvent('agreement_page_unload', {
      agreementType: 'privacy_policy',
      timestamp: Date.now()
    })
  }
})