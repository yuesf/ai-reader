// pages/reportDetail/reportDetail.js
const { reportAPI } = require('../../utils/api.js')
const config = require('../../utils/config.js')

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
    // 从页面参数获取报告ID
    const reportId = options.id;
    this.setData({ reportId });
    
    // 获取报告详情
    this.getReportDetail(reportId);
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
   * 下载文档
   */
  downloadDocument() {
    console.log('ReportDetail: downloadDocument called');
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
   * 跳转到预览页面
   */
  goToPreview() {
    console.log('ReportDetail: goToPreview called', {
      reportId: this.data.reportId,
      fileId: this.data.reportFileId,
      title: this.data.reportTitle
    });

    if (!this.data.reportFileId) {
      wx.showToast({
        title: '报告文件不存在',
        icon: 'none'
      });
      return;
    }

    // 跳转到预览页面，传递必要的参数
    const url = `/pages/pdfPreview/pdfPreview?reportId=${this.data.reportId}&fileId=${this.data.reportFileId}&title=${encodeURIComponent(this.data.reportTitle)}`;
    console.log('准备跳转到:', url);
    
    wx.navigateTo({
      url: url,
      success: (res) => {
        console.log('跳转成功:', res);
      },
      fail: (err) => {
        console.error('跳转失败:', err);
        wx.showToast({
          title: '跳转失败: ' + err.errMsg,
          icon: 'none'
        });
      }
    });
  },

  /**
   * 分享报告
   */
  onShareAppMessage() {
    return {
      title: this.data.reportTitle,
      path: `/pages/reportDetail/reportDetail?id=${this.data.reportId}`,
      imageUrl: this.data.reportThumbnail
    };
  },

  /**
   * 分享到朋友圈
   */
  onShareTimeline() {
    return {
      title: this.data.reportTitle,
      imageUrl: this.data.reportThumbnail
    };
  },

  /**
   * 返回上一页
   */
  onBack() {
    wx.navigateBack();
  }
});