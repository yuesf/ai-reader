// pages/pdfViewer/pdfViewer.js
Page({
  data: {
    pdfUrl: '',
    loading: true,
    error: false,
    errorMessage: ''
  },

  onLoad(options) {
    if (options.url) {
      const pdfUrl = decodeURIComponent(options.url);
      console.log('PDF预览URL:', pdfUrl);
      
      this.setData({
        pdfUrl: pdfUrl,
        loading: false
      });
      
      // 如果是本地PDF文件，直接使用wx.openDocument打开
      if (pdfUrl.startsWith(wx.env.USER_DATA_PATH)) {
        this.openLocalPdf(pdfUrl);
      }
    } else {
      this.setData({
        loading: false,
        error: true,
        errorMessage: '缺少PDF文件URL'
      });
    }
  },
  
  // 打开本地PDF文件
  openLocalPdf(filePath) {
    wx.openDocument({
      filePath: filePath,
      fileType: 'pdf',
      success: (res) => {
        console.log('打开本地PDF成功');
        // 打开成功后关闭当前页面
        wx.navigateBack();
      },
      fail: (err) => {
        console.error('打开本地PDF失败:', err);
        this.setData({
          loading: false,
          error: true,
          errorMessage: 'PDF文件打开失败: ' + (err.errMsg || '未知错误')
        });
      }
    });
  },

  onShareAppMessage() {
    return {
      title: 'PDF文档预览',
      path: '/pages/pdfViewer/pdfViewer'
    };
  },

  // PDF加载成功
  onPdfLoadSuccess() {
    console.log('PDF加载成功');
    this.setData({
      loading: false,
      error: false
    });
  },

  // PDF加载失败
  onPdfLoadError(error) {
    console.error('PDF加载失败:', error);
    this.setData({
      loading: false,
      error: true,
      errorMessage: 'PDF文件加载失败，请稍后重试'
    });
  },

  // 返回上一页
  onBack() {
    wx.navigateBack();
  },

  // 重新加载
  onReload() {
    this.setData({ loading: true, error: false });
    // 简单的重新加载，实际项目中可能需要更复杂的逻辑
    setTimeout(() => {
      this.onLoad({ url: encodeURIComponent(this.data.pdfUrl) });
    }, 500);
  }
});