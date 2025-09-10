// pages/reportDetail/reportDetail.js
const { reportAPI } = require('../../utils/api.js')
const config = require('../../utils/config.js')
const { trackPage, trackButton, trackDownload, trackClick } = require('../../utils/tracking/index.js')

Page({
  data: {
    // 报告基本信息
    reportTitle: '',
    reportSubtitle: '',
    reportDate: '',
    downloadPrice: '¥1.98',
    reportId: '',
    reportSummary: '',
    reportIsFree: false,
    reportFileId: '', // 报告文件ID
    reportSource: '', // 报告来源
    reportCategory: '', // 报告分类
    reportPages: 0, // 报告页数
    reportPrice: '0.00', // 报告价格
    reportThumbnail: '', // 报告缩略图
    reportTags: [], // 报告标签
    reportAuthor: '', // 报告作者
    reportPublishDate: '', // 发布日期
    reportUpdateDate: '', // 更新日期
    reportViewCount: 0, // 浏览次数
    reportDownloadCount: 0, // 下载次数
    reportRating: 0, // 评分
    reportComments: [], // 评论列表
    
    // 页面状态
    loading: false,
    error: false,
    errorMessage: '',
    
  },

  onLoad(options) {
    console.log('ReportDetail: onLoad called', options);
    
    // 页面浏览埋点 - 异步执行
    setTimeout(() => {
      try {
        trackPage('reportDetail', '报告详情页', { reportId: options.id });
        console.log('[报告详情] 页面浏览埋点已发送:', options.id)
      } catch (error) {
        console.error('[报告详情] 页面浏览埋点失败:', error)
      }
    }, 0)
    
    // 移除登录检查，允许游客浏览
    console.log('[报告详情页] 允许游客模式浏览')
    
    // 从页面参数获取报告ID
    const reportId = options.id;
    this.setData({ reportId });
    
    // 获取报告详情
    this.getReportDetail(reportId);
  },

  // 检查登录状态（保留方法但不强制跳转）- 增强日志记录
  checkLoginStatus() {
    const app = getApp();
    const globalIsLoggedIn = app.globalData.isLoggedIn;
    const globalUserInfo = app.globalData.userInfo;
    const storageUserInfo = wx.getStorageSync('userInfo');
    
    // 详细的登录状态检查日志
    console.log('[登录状态检查] 详细信息:', {
      globalIsLoggedIn: globalIsLoggedIn,
      hasGlobalUserInfo: !!globalUserInfo,
      hasStorageUserInfo: !!storageUserInfo,
      globalUserOpenId: globalUserInfo ? globalUserInfo.openid : null,
      storageUserOpenId: storageUserInfo ? storageUserInfo.openid : null,
      timestamp: new Date().toISOString(),
      page: 'reportDetail'
    });
    
    // 检查数据一致性
    if (globalIsLoggedIn && !globalUserInfo) {
      console.warn('[登录状态检查] 数据不一致: globalData.isLoggedIn为true但userInfo为空');
    }
    
    if (!globalIsLoggedIn && globalUserInfo) {
      console.warn('[登录状态检查] 数据不一致: globalData.isLoggedIn为false但userInfo存在');
    }
    
    if (globalUserInfo && !storageUserInfo) {
      console.warn('[登录状态检查] 数据不一致: 全局userInfo存在但本地存储为空');
    }
    
    if (!globalUserInfo && storageUserInfo) {
      console.warn('[登录状态检查] 数据不一致: 全局userInfo为空但本地存储存在');
      // 尝试从本地存储恢复用户信息
      app.globalData.userInfo = storageUserInfo;
      app.globalData.isLoggedIn = true;
      console.log('[登录状态检查] 已从本地存储恢复用户信息');
      return true;
    }
    
    return globalIsLoggedIn;
  },

  /**
   * 获取报告详情
   */
  async getReportDetail(reportId) {
    console.log('ReportDetail: getReportDetail called', reportId);
    try {
      this.setData({ loading: true, error: false });
      
      const result = await reportAPI.getReportDetail(reportId);
      
      console.log('报告详情接口返回数据:', result);
      if (result.code === 200) {
        const report = result.data;
        this.setData({
          reportTitle: report.title,
          reportSubtitle: report.subtitle || `${report.source} | ${report.category} | 共${report.pages}页`,
          reportSummary: report.summary,
          reportSource: report.source,
          reportCategory: report.category,
          reportPages: report.pages,
          reportPrice: report.price,
          reportIsFree: report.isFree,
          reportFileId: report.reportFileId || '',
          reportThumbnail: report.thumbnail || '',
          reportTags: report.tags || [],
          reportAuthor: report.author || '',
          reportPublishDate: report.publishDate || '',
          reportUpdateDate: report.updateDate || '',
          reportViewCount: report.viewCount || 0,
          reportDownloadCount: report.downloadCount || 0,
          reportRating: report.rating || 0,
          reportComments: report.comments || []
        });
        
      } else {
        this.setData({ 
          error: true, 
          errorMessage: result.message || '获取报告详情失败' 
        });
        
        wx.showToast({
          title: result.message || '获取报告详情失败',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('获取报告详情失败:', error);
      this.setData({ 
        error: true, 
        errorMessage: '网络错误，请重试' 
      });
      
      wx.showToast({
        title: '获取报告详情失败，请重试',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 下载文档 - 修复埋点异步执行
   */
  downloadDocument() {
    console.log('ReportDetail: downloadDocument called');
    
    // 下载按钮点击埋点 - 异步执行，不影响主业务
    setTimeout(() => {
      try {
        trackClick('download_button', '下载按钮点击', {
          reportId: this.data.reportId,
          reportTitle: this.data.reportTitle,
          isFree: this.data.reportIsFree,
          price: this.data.reportPrice,
          fileId: this.data.reportFileId
        });
        console.log('[报告详情] 下载按钮埋点已发送:', this.data.reportId)
      } catch (error) {
        console.error('[报告详情] 下载按钮埋点失败:', error)
        // 埋点失败不影响主业务
      }
    }, 0)
    
    if (!this.data.reportFileId) {
      wx.showToast({
        title: '报告文件不存在',
        icon: 'none'
      });
      return;
    }

    // 如果是付费报告，显示购买确认
    if (!this.data.reportIsFree) {
      wx.showModal({
        title: '购买确认',
        content: `确认购买此报告？\n价格：${this.data.reportPrice}`,
        success: (res) => {
          if (res.confirm) {
            this.processDownload();
          }
        }
      });
    } else {
      // 免费报告直接下载
      this.processDownload();
    }
  },

  /**
   * 处理下载逻辑
   */
  async processDownload() {
    console.log('ReportDetail: processDownload called');
    try {
      wx.showLoading({ title: '准备下载...' });
      
      // 这里可以添加实际的下载逻辑
      // 例如：调用下载API、生成下载链接等
      
      setTimeout(() => {
        wx.hideLoading();
        wx.showToast({
          title: '下载已开始',
          icon: 'success'
        });
      }, 1000);
      
    } catch (error) {
      wx.hideLoading();
      console.error('下载失败:', error);
      wx.showToast({
        title: '下载失败，请重试',
        icon: 'none'
      });
    }
  },

  /**
   * 跳转到预览页面 - 添加登录验证和详细日志
   */
  goToPreview() {
    console.log('ReportDetail: goToPreview called', {
      reportId: this.data.reportId,
      fileId: this.data.reportFileId,
      title: this.data.reportTitle
    });

    // 获取当前登录状态
    const isLoggedIn = this.checkLoginStatus();
    const app = getApp();
    const userInfo = app.globalData.userInfo;
    
    // 详细的登录状态日志
    console.log('[预览报告] 登录状态检查:', {
      isLoggedIn: isLoggedIn,
      hasUserInfo: !!userInfo,
      userOpenId: userInfo ? userInfo.openid : null,
      userNickName: userInfo ? userInfo.nickName : null,
      globalDataIsLoggedIn: app.globalData.isLoggedIn,
      storageUserInfo: wx.getStorageSync('userInfo'),
      timestamp: new Date().toISOString(),
      reportId: this.data.reportId,
      reportTitle: this.data.reportTitle
    });

    // 预览按钮点击埋点 - 异步执行，不影响主业务
    setTimeout(() => {
      try {
        trackClick('preview_button', '预览按钮点击', {
          reportId: this.data.reportId,
          reportTitle: this.data.reportTitle,
          fileId: this.data.reportFileId,
          isLoggedIn: isLoggedIn,
          hasUserInfo: !!userInfo,
          userOpenId: userInfo ? userInfo.openid : null
        });
        console.log('[报告详情] 预览按钮埋点已发送:', this.data.reportId)
      } catch (error) {
        console.error('[报告详情] 预览按钮埋点失败:', error)
        // 埋点失败不影响主业务
      }
    }, 0)

    // 检查登录状态，预览功能需要登录
    if (!isLoggedIn) {
      console.log('[预览报告] 用户未登录，需要跳转到登录页面');
      
      // 显示登录提示
      wx.showToast({
        title: '请先登录后预览',
        icon: 'none',
        duration: 2000
      });

      // 登录跳转埋点
      setTimeout(() => {
        try {
          trackClick('login_redirect', '预览登录跳转', {
            from: 'reportDetail',
            reportId: this.data.reportId,
            reason: 'preview_required_login',
            timestamp: new Date().toISOString()
          });
          console.log('[预览报告] 登录跳转埋点已发送');
        } catch (error) {
          console.error('[报告详情] 登录跳转埋点失败:', error)
        }
      }, 0)
      
      // 延迟跳转到登录页，让用户看到提示
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/login/login',
          success: () => {
            console.log('[预览报告] 成功跳转到登录页面');
          },
          fail: (err) => {
            console.error('[预览报告] 跳转到登录页面失败:', err);
          }
        });
      }, 1000);
      return;
    }

    console.log('[预览报告] 用户已登录，继续预览流程');

    if (!this.data.reportFileId) {
      console.log('[预览报告] 报告文件ID不存在:', this.data.reportFileId);
      wx.showToast({
        title: '报告文件不存在',
        icon: 'none'
      });
      return;
    }

    // 跳转到预览页面，传递必要的参数
    const url = `/pages/pdfPreview/pdfPreview?reportId=${this.data.reportId}&fileId=${this.data.reportFileId}&title=${encodeURIComponent(this.data.reportTitle)}`;
    console.log('[预览报告] 准备跳转到预览页面:', url);
    
    wx.navigateTo({
      url: url,
      success: (res) => {
        console.log('[预览报告] 跳转预览页面成功:', res);
      },
      fail: (err) => {
        console.error('[预览报告] 跳转预览页面失败:', err);
        wx.showToast({
          title: '跳转失败: ' + err.errMsg,
          icon: 'none'
        });
      }
    });
  },


  /**
   * 分享报告 - 修复埋点异步执行
   */
  onShareAppMessage() {
    // 分享到微信好友埋点 - 异步执行
    setTimeout(() => {
      try {
        trackClick('share_wechat', '分享到微信好友', {
          reportId: this.data.reportId,
          reportTitle: this.data.reportTitle
        });
        console.log('[报告详情] 分享微信好友埋点已发送:', this.data.reportId)
      } catch (error) {
        console.error('[报告详情] 分享微信好友埋点失败:', error)
      }
    }, 0)

    return {
      title: this.data.reportTitle,
      path: `/pages/reportDetail/reportDetail?id=${this.data.reportId}`,
      imageUrl: this.data.reportThumbnail
    };
  },

  /**
   * 分享到朋友圈 - 修复埋点异步执行
   */
  onShareTimeline() {
    // 分享到朋友圈埋点 - 异步执行
    setTimeout(() => {
      try {
        trackButton('share_timeline', '分享到朋友圈');
        console.log('[报告详情] 分享朋友圈埋点已发送')
      } catch (error) {
        console.error('[报告详情] 分享朋友圈埋点失败:', error)
      }
    }, 0)

    return {
      title: this.data.reportTitle,
      imageUrl: this.data.reportThumbnail
    };
  },

  /**
   * 返回上一页 - 修复埋点异步执行
   */
  onBack() {
    // 返回按钮埋点 - 异步执行
    setTimeout(() => {
      try {
        trackButton('back_button', '返回按钮');
        console.log('[报告详情] 返回按钮埋点已发送')
      } catch (error) {
        console.error('[报告详情] 返回按钮埋点失败:', error)
      }
    }, 0)

    wx.navigateBack();
  }
});