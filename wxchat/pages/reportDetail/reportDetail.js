const { reportAPI } = require('../../utils/api.js')
const config = require('../../utils/config.js')
const { buildPreviewUrl, PERFORMANCE_CONFIG, ERROR_CONFIG } = require('../../utils/pdfConfig.js')
const pdfDownloadService = require('../../utils/pdfDownloadService.js')

Page({
  data: {
    activeTab: 'description', // 修改默认选中标签为简介
    reportTitle: '',
    reportSubtitle: '',
    reportDate: '',
    downloadPrice: '¥1.98',
    reportId: '',
    reportSummary: '',
    reportIsFree: false,
    pdfUrl: '', // 添加PDF文件URL
    pdfFilePath: '', // 保存PDF文件路径
    hasPreviewed: false, // 添加是否已预览标志
    previewLoading: false, // 添加预览加载状态
    pdfViewerUrl: '', // 新增PDF.js预览URL
    pdfLoadError: false, // 新增PDF加载错误状态
    pdfDownloaded: false, // 新增PDF是否已下载状态
    showPdfViewer: false, // 控制PDF查看器显示
    pdfFileSize: '', // PDF文件大小
    pdfPageCount: 0, // PDF页数
    previewMode: 'fast', // 预览模式：fast-快速预览，full-完整预览
    loadRetryCount: 0, // 加载重试次数
    currentCdnIndex: 0, // 当前CDN索引
    
    // 新增：分片下载相关状态
    downloadStatus: 'idle', // 下载状态：idle, downloading, paused, completed, failed
    downloadProgress: 0, // 下载进度 0-100
    downloadTaskId: '', // 下载任务ID
    showDownloadProgress: false, // 是否显示下载进度
    downloadedChunks: 0, // 已下载分片数
    totalChunks: 0, // 总分片数
    fileInfo: null, // 文件信息
    reportFileId: '', // 报告文件ID
    // 在页面数据中添加调试状态
    debugMode: false,
    connectionStatus: 'unknown',
    downloadDebugInfo: null
  },
  
  onLoad(options) {
    // 从页面参数获取报告ID
    const reportId = options.id;
    this.setData({ reportId });
    
    // 使用 reportAPI 获取报告详情
    this.getReportDetail(reportId);
    // 添加防止重复预览的标志位初始化
    this.setData({ hasPreviewed: false });
  },
  
  async getReportDetail(reportId) {
    try {
      const result = await reportAPI.getReportDetail(reportId);
      
      console.log('报告详情接口返回数据:', result);
      if (result.code === 200) {
        const report = result.data;
        this.setData({
          reportTitle: report.title,
          reportSubtitle: `${report.source} | ${report.category} | 共${report.pages}页`,
          reportSummary: report.summary,
          downloadPrice: report.price,
          reportIsFree: report.isFree,
          reportFileId: report.reportFileId || '' // 保存报告文件ID
        });
      } else {
        wx.showToast({
          title: result.message || '获取报告详情失败',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('获取报告详情失败:', error);
      wx.showToast({
        title: '获取报告详情失败，请重试',
        icon: 'none'
      });
    }
  },
  
  handleTabChange(e) {
    const newTab = e.currentTarget.dataset.detail;
    // 使用setData的回调确保状态更新完成后再进行其他操作
    this.setData({ activeTab: newTab }, () => {
      // 当切换到预览标签时自动预览PDF
      if (newTab === 'preview') {
        this.previewPDF();
      }
    });
  },
  
  downloadDocument() {
    // 下载文档逻辑
    wx.showModal({
      title: '提示',
      content: '请确认是否购买并下载文档',
      success: (res) => {
        if (res.confirm) {
          // 实现支付和下载逻辑
          console.log('开始下载文档');
          this.startPdfDownload();
        }
      }
    });
  },
  
  // 新增：开始PDF分片下载
  async startPdfDownload() {
    try {
      // 检查是否有报告文件ID
      if (!this.data.reportFileId) {
        wx.showToast({
          title: '报告文件不存在',
          icon: 'none'
        });
        return;
      }
      
      // 显示下载选项
      wx.showModal({
        title: '选择下载模式',
        content: '请选择下载模式：\n1. 正常模式：支持加密解密\n2. 测试模式：跳过加密解密',
        confirmText: '正常模式',
        cancelText: '测试模式',
        success: (res) => {
          const testMode = res.confirm ? false : true;
          this.startPdfDownloadWithMode(testMode);
        }
      });
      
    } catch (error) {
      console.error('准备下载失败:', error);
      wx.showToast({
        title: '准备下载失败',
        icon: 'none'
      });
    }
  },

  // 新增：根据模式开始PDF下载
  async startPdfDownloadWithMode(testMode) {
    try {
      console.log(`开始PDF下载，测试模式: ${testMode}`);
      
      // 调试信息：检查pdfDownloadService
      console.log('pdfDownloadService对象:', pdfDownloadService);
      console.log('pdfDownloadService.startDownload类型:', typeof pdfDownloadService.startDownload);
      
      // 获取PDF文件信息
      const fileInfo = await this.getPdfFileInfo(this.data.reportFileId);
      
      if (!fileInfo) {
        wx.showToast({
          title: '获取文件信息失败',
          icon: 'none'
        });
        return;
      }
      
      // 开始下载
      pdfDownloadService.startDownload(
        this.data.reportFileId,
        fileInfo.filename || 'report.pdf',
        this.onDownloadProgress.bind(this),
        this.onDownloadComplete.bind(this),
        this.onDownloadError.bind(this),
        testMode // 传递测试模式参数
      );
      
      // 更新UI状态
      this.setData({
        isDownloading: true,
        downloadProgress: 0,
        downloadStatus: '开始下载...'
      });
      
      wx.showToast({
        title: testMode ? '测试模式下载开始' : '正常模式下载开始',
        icon: 'success'
      });
      
    } catch (error) {
      console.error('开始下载失败:', error);
      wx.showToast({
        title: '开始下载失败',
        icon: 'none'
      });
    }
  },
  
  // 获取PDF文件信息
  async getPdfFileInfo(fileId) {
    try {
      const result = await reportAPI.getPdfFileInfo(fileId);
      
      if (result.code === 200) {
        return result.data;
      } else {
        wx.showToast({
          title: result.message || '获取文件信息失败',
          icon: 'none'
        });
        return null;
      }
    } catch (error) {
      console.error('获取PDF文件信息失败:', error);
      wx.showToast({
        title: '获取文件信息失败，请重试',
        icon: 'none'
      });
      return null;
    }
  },
  
  // 下载进度回调
  onDownloadProgress(progress) {
    this.setData({
      downloadProgress: progress,
      downloadedChunks: Math.round((progress / 100) * this.data.totalChunks)
    });
  },
  
  // 下载完成回调
  onDownloadComplete(filePath) {
    this.setData({
      downloadStatus: 'completed',
      downloadProgress: 100,
      pdfFilePath: filePath,
      pdfDownloaded: true,
      showDownloadProgress: false
    });
    
    wx.showToast({
      title: 'PDF下载完成',
      icon: 'success',
      duration: 2000
    });
    
    // 自动显示PDF预览
    this.showPdfPreview();
  },
  
  // 下载错误回调
  onDownloadError(error) {
    this.setData({
      downloadStatus: 'failed',
      showDownloadProgress: false
    });
    
    wx.showToast({
      title: `下载失败: ${error.message}`,
      icon: 'none',
      duration: 3000
    });
  },
  
  // 暂停下载
  pauseDownload() {
    if (this.data.downloadTaskId) {
      pdfDownloadService.pauseDownload(this.data.downloadTaskId);
      this.setData({ downloadStatus: 'paused' });
      
      wx.showToast({
        title: '下载已暂停',
        icon: 'success'
      });
    }
  },
  
  // 恢复下载
  resumeDownload() {
    if (this.data.downloadTaskId) {
      pdfDownloadService.resumeDownload(this.data.downloadTaskId);
      this.setData({ downloadStatus: 'downloading' });
      
      wx.showToast({
        title: '下载已恢复',
        icon: 'success'
      });
    }
  },
  
  // 取消下载
  cancelDownload() {
    if (this.data.downloadTaskId) {
      pdfDownloadService.cancelDownload(this.data.downloadTaskId);
      this.setData({
        downloadStatus: 'idle',
        showDownloadProgress: false,
        downloadProgress: 0,
        downloadedChunks: 0
      });
      
      wx.showToast({
        title: '下载已取消',
        icon: 'success'
      });
    }
  },
  
  // 重新优化预览PDF功能 - 采用分片下载方案
  previewPDF() {
    // 如果已经下载了PDF文件，直接显示预览
    if (this.data.pdfDownloaded && this.data.pdfFilePath) {
      this.showPdfPreview();
      return;
    }
    
    // 如果正在下载，提示用户等待
    if (this.data.downloadStatus === 'downloading') {
      wx.showToast({
        title: '文件正在下载中，请稍候',
        icon: 'none'
      });
      return;
    }
    
    // 如果下载失败，提示重新下载
    if (this.data.downloadStatus === 'failed') {
      wx.showModal({
        title: '提示',
        content: '文件下载失败，是否重新下载？',
        success: (res) => {
          if (res.confirm) {
            this.startPdfDownload();
          }
        }
      });
      return;
    }
    
    // 检查是否有报告文件ID
    if (!this.data.reportFileId) {
      wx.showToast({
        title: '报告文件不存在',
        icon: 'none'
      });
      return;
    }
    
    // 开始下载PDF文件
    this.startPdfDownload();
  },
  
  // 显示PDF预览 - 使用配置文件优化版本
  showPdfPreview() {
    const pdfUrl = this.data.pdfFilePath;
    if (!pdfUrl) {
      wx.showToast({
        title: 'PDF文件未准备就绪',
        icon: 'none'
      });
      return;
    }
    
    // 直接使用wx.openDocument打开本地PDF文件
    if (pdfUrl.startsWith(wx.env.USER_DATA_PATH)) {
      wx.openDocument({
        filePath: pdfUrl,
        fileType: 'pdf',
        success: (res) => {
          console.log('打开PDF文档成功');
        },
        fail: (err) => {
          console.error('打开PDF文档失败', err);
          wx.showToast({
            title: '打开PDF文档失败: ' + err.errMsg,
            icon: 'none',
            duration: 3000
          });
        }
      });
      return;
    }
    
    // 对于网络文件，使用PDF.js viewer预览
    try {
      // 使用配置文件构建优化的预览URL（仅适用于网络文件）
      const fullPdfUrl = buildPreviewUrl(pdfUrl, this.data.previewMode);
      
      this.setData({ 
        pdfViewerUrl: fullPdfUrl,
        showPdfViewer: true,
        hasPreviewed: true 
      });
      
      console.log('优化PDF预览URL:', fullPdfUrl);
      
      // 设置加载超时
      this.setLoadTimeout();
      
    } catch (error) {
      console.error('构建预览URL失败:', error);
      this.handlePreviewError('UNKNOWN_ERROR');
    }
  },
  
  // 设置加载超时
  setLoadTimeout() {
    setTimeout(() => {
      if (this.data.showPdfViewer && !this.data.pdfViewerUrl) {
        this.handlePreviewError('TIMEOUT_ERROR');
      }
    }, PERFORMANCE_CONFIG.LOAD_TIMEOUT);
  },
  
  // 处理预览错误
  handlePreviewError(errorType) {
    const { MESSAGES, RETRY_STRATEGY } = ERROR_CONFIG;
    const canRetry = RETRY_STRATEGY[errorType];
    
    if (canRetry && this.data.loadRetryCount < PERFORMANCE_CONFIG.MAX_RETRIES) {
      // 可以重试
      this.retryPreview();
    } else {
      // 不能重试或重试次数已满
      this.setData({ 
        showPdfViewer: false,
        pdfLoadError: true 
      });
      
      wx.showToast({
        title: MESSAGES[errorType] || MESSAGES.UNKNOWN_ERROR,
        icon: 'none',
        duration: 3000
      });
    }
  },
  
  // 重试预览
  retryPreview() {
    const { loadRetryCount } = this.data;
    const newRetryCount = loadRetryCount + 1;
    
    this.setData({ 
      loadRetryCount: newRetryCount,
      showPdfViewer: false 
    });
    
    wx.showToast({
      title: `正在重试预览 (${newRetryCount}/${PERFORMANCE_CONFIG.MAX_RETRIES})`,
      icon: 'none',
      duration: 2000
    });
    
    // 延迟重试
    setTimeout(() => {
      this.showPdfPreview();
    }, PERFORMANCE_CONFIG.RETRY_INTERVAL);
  },
  
  // 隐藏PDF预览
  hidePdfPreview() {
    this.setData({ 
      showPdfViewer: false,
      pdfViewerUrl: ''
    });
  },
  
  // 切换预览模式
  switchPreviewMode() {
    const newMode = this.data.previewMode === 'fast' ? 'full' : 'fast';
    this.setData({ previewMode: newMode });
    
    // 重新构建预览URL
    if (this.data.showPdfViewer) {
      this.showPdfPreview();
    }
    
    wx.showToast({
      title: `已切换到${newMode === 'fast' ? '快速' : '完整'}预览模式`,
      icon: 'success',
      duration: 1500
    });
  },
  
  // 打开PDF文档（外部应用）
  openPdfDocument() {
    if (!this.data.pdfFilePath) {
      wx.showToast({
        title: '文件尚未准备就绪',
        icon: 'none'
      });
      return;
    }
    
    wx.openDocument({
      filePath: this.data.pdfFilePath,
      fileType: 'pdf',
      success: (res) => {
        console.log('打开文档成功');
      },
      fail: (err) => {
        console.error('打开文档失败', err);
        wx.showToast({
          title: '打开文档失败',
          icon: 'none'
        });
      }
    });
  },
  
  // 页面卸载时清理资源
  onUnload() {
    // 取消正在进行的下载
    if (this.data.downloadTaskId && this.data.downloadStatus === 'downloading') {
      pdfDownloadService.cancelDownload(this.data.downloadTaskId);
    }
  },
  
  /**
   * 测试PDF流服务连接
   */
  async testPdfConnection() {
    try {
      this.setData({ connectionStatus: 'testing' });
      
      const { testConnection } = require('../../utils/pdfDownloadService.js');
      const isConnected = await testConnection();
      
      this.setData({ 
        connectionStatus: isConnected ? 'connected' : 'failed',
        debugMode: true
      });
      
      wx.showToast({
        title: isConnected ? '连接正常' : '连接失败',
        icon: isConnected ? 'success' : 'error'
      });
      
    } catch (error) {
      console.error('连接测试失败:', error);
      this.setData({ connectionStatus: 'failed' });
      
      wx.showToast({
        title: '连接测试失败',
        icon: 'error'
      });
    }
  },

  /**
   * 调试PDF下载
   */
  debugPdfDownload() {
    try {
      const { debugChunkData } = require('../../utils/pdfDownloadService.js');
      
      if (this.data.report && this.data.report.reportFileId) {
        debugChunkData(this.data.report.reportFileId);
        
        // 获取下载状态
        const { getDownloadStatus } = require('../../utils/pdfDownloadService.js');
        const status = getDownloadStatus(this.data.report.reportFileId);
        
        this.setData({ downloadDebugInfo: status });
        
        wx.showToast({
          title: '调试信息已输出到控制台',
          icon: 'success'
        });
      } else {
        wx.showToast({
          title: '报告文件ID不存在',
          icon: 'error'
        });
      }
      
    } catch (error) {
      console.error('调试失败:', error);
      wx.showToast({
        title: '调试失败',
        icon: 'error'
      });
    }
  },

  /**
   * 切换调试模式
   */
  toggleDebugMode() {
    this.setData({ debugMode: !this.data.debugMode });
  }
});