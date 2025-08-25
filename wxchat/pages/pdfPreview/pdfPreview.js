// pages/pdfPreview/pdfPreview.js
const { pdfPreviewService } = require('../../utils/pdfPreviewService.js');
const pdfDownloadService = require('../../utils/pdfDownloadService.js');

Page({
  data: {
    // 页面参数
    reportId: '',
    fileId: '',
    title: '',
    
    // PDF下载相关
    showDownloadProgress: false,
    downloadProgress: 0,
    downloadStatus: '',
    downloadedChunks: 0,
    totalChunks: 0,
    pdfFilePath: '',
    pdfDownloaded: false,
    
    // 小程序内预览相关
    showInlinePreview: false,
    canvasWidth: 300,
    canvasHeight: 400,
    zoomLevel: 1.0,
    currentPage: 1,
    totalPages: 1,
    isRendering: false,
    previewStatusText: '准备预览...',
    
    // 触摸手势状态
    touchStartX: null,
    touchStartY: null,
    touchStartTime: null
  },

  onLoad(options) {
    // 获取页面参数
    const { reportId, fileId, title } = options;
    this.setData({
      reportId: reportId || '',
      fileId: fileId || '',
      title: decodeURIComponent(title || 'PDF预览')
    });

    // 设置页面标题
    wx.setNavigationBarTitle({
      title: this.data.title
    });

    // 传递 fileId 给预览服务，用于服务端渲染兜底
    try {
      if (fileId && typeof pdfPreviewService.setFileId === 'function') {
        pdfPreviewService.setFileId(fileId);
      }
    } catch (e) {
      console.warn('设置fileId到pdfPreviewService失败:', e);
    }

    // 自动开始下载
    if (this.data.fileId) {
      this.startPdfDownload();
    }
  },

  /**
   * 开始PDF下载
   */
  async startPdfDownload() {
    if (!this.data.fileId) {
      wx.showToast({
        title: '文件ID不存在',
        icon: 'none'
      });
      return;
    }

    // 防御性检查，避免服务未正确导入导致报错
    if (!pdfDownloadService || typeof pdfDownloadService.startDownload !== 'function') {
      console.error('pdfDownloadService 未正确加载或缺少 startDownload 方法:', pdfDownloadService);
      wx.showToast({
        title: '下载服务不可用',
        icon: 'none'
      });
      return;
    }

    try {
      this.setData({
        showDownloadProgress: true,
        downloadStatus: 'downloading',
        downloadProgress: 0,
        downloadedChunks: 0,
        totalChunks: 0
      });

      // 使用PDF下载服务
      const downloadResult = await pdfDownloadService.startDownload(
        this.data.fileId,
        `${this.data.title}.pdf`,
        this.onDownloadProgress.bind(this),
        this.onDownloadComplete.bind(this),
        this.onDownloadError.bind(this)
      );

      console.log('PDF下载开始:', downloadResult);
      
    } catch (error) {
      console.error('PDF下载失败:', error);
      this.setData({
        downloadStatus: 'failed',
        showDownloadProgress: false
      });
      
      wx.showToast({
        title: '下载失败',
        icon: 'none'
      });
    }
  },

  /**
   * 下载进度回调
   */
  onDownloadProgress(progress, downloadedChunks, totalChunks) {
    this.setData({
      downloadProgress: progress,
      downloadedChunks: downloadedChunks,
      totalChunks: totalChunks
    });
  },

  /**
   * 下载完成回调
   */
  onDownloadComplete(filePath) {
    console.log('PDF下载完成:', filePath);
    
    this.setData({
      pdfDownloaded: true,
      pdfFilePath: filePath,
      showDownloadProgress: false,
      downloadStatus: 'completed',
      downloadProgress: 100
    });

    wx.showToast({
      title: 'PDF下载完成',
      icon: 'success',
      duration: 2000
    });

    // 自动开始预览
    setTimeout(() => {
      this.startInlinePreview();
    }, 500);
  },

  /**
   * 下载错误回调
   */
  onDownloadError(error) {
    console.error('PDF下载错误:', error);
    
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

  /**
   * 暂停下载
   */
  pauseDownload() {
    pdfDownloadService.pauseDownload();
    this.setData({ downloadStatus: 'paused' });
    
    wx.showToast({
      title: '下载已暂停',
      icon: 'success'
    });
  },

  /**
   * 恢复下载
   */
  resumeDownload() {
    pdfDownloadService.resumeDownload();
    this.setData({ downloadStatus: 'downloading' });
    
    wx.showToast({
      title: '下载已恢复',
      icon: 'success'
    });
  },

  /**
   * 取消下载
   */
  cancelDownload() {
    pdfDownloadService.cancelDownload();
    this.setData({
      downloadStatus: 'cancelled',
      showDownloadProgress: false,
      downloadProgress: 0,
      downloadedChunks: 0
    });
    
    wx.showToast({
      title: '下载已取消',
      icon: 'success'
    });
  },

  /**
   * 开始小程序内预览
   */
  startInlinePreview() {
    if (!this.data.pdfFilePath) {
      wx.showToast({
        title: 'PDF文件未准备就绪',
        icon: 'none'
      });
      return;
    }

    // 确保文件存在
    wx.getFileSystemManager().access({
      path: this.data.pdfFilePath,
      success: () => {
        console.log('文件存在，开始预览');
        
        this.setData({
          showInlinePreview: true,
          previewStatusText: '正在解析PDF文件...',
          isRendering: true
        });

        // 延迟执行，确保DOM已加载
        wx.nextTick(() => {
          this.initCanvas().then((canvasObj) => {
            console.log('Canvas初始化成功:', canvasObj);
            
            // 使用PDF预览服务加载PDF文件
            pdfPreviewService.loadPdf(this.data.pdfFilePath).then((pdfInfo) => {
              console.log('PDF加载成功:', pdfInfo);
              
              // 更新页面信息
              this.setData({
                totalPages: pdfInfo.totalPages,
                previewStatusText: `PDF解析完成，共${pdfInfo.totalPages}页，版本${pdfInfo.version}`
              });
              
              // 渲染第一页
              return pdfPreviewService.renderPage(1, canvasObj.canvas);
            }).then((renderResult) => {
              console.log('第一页渲染成功:', renderResult);
              
              // 获取PDF状态
              const status = pdfPreviewService.getStatus();
              const statusText = status.hasRealContent ? 
                '预览就绪（真实PDF内容）' : 
                '预览就绪（模拟内容）';
              
              this.setData({
                previewStatusText: statusText,
                isRendering: false
              });
              
              wx.showToast({
                title: status.hasRealContent ? 'PDF预览已就绪' : 'PDF预览就绪（显示模拟内容）',
                icon: 'success',
                duration: 2000
              });
              
            }).catch((error) => {
              console.error('PDF预览失败:', error);
              this.setData({
                previewStatusText: '预览失败: ' + error.message,
                isRendering: false
              });
              
              wx.showToast({
                title: '预览失败',
                icon: 'none',
                duration: 3000
              });
            });
            
          }).catch((error) => {
            console.error('Canvas初始化失败:', error);
            this.setData({
              previewStatusText: '画布初始化失败',
              isRendering: false
            });
            
            wx.showToast({
              title: '画布初始化失败',
              icon: 'none',
              duration: 3000
            });
          });
        });
      },
      fail: () => {
        console.error('文件不存在:', this.data.pdfFilePath);
        wx.showToast({
          title: '文件不存在，请重新下载',
          icon: 'none'
        });
      }
    });
  },

  /**
   * 隐藏内嵌预览
   */
  hideInlinePreview() {
    this.setData({
      showInlinePreview: false,
      isRendering: false
    });
    
    // 清理PDF预览器资源
    pdfPreviewService.cleanup();
  },

  /**
   * 初始化画布
   */
  async initCanvas() {
    return new Promise((resolve, reject) => {
      try {
        // 确保在页面渲染完成后执行
        wx.nextTick(() => {
          const query = wx.createSelectorQuery();
          // 使用.select()选择Canvas元素
          query.select('#pdfCanvas').fields({ node: true, size: true })
            .exec((res) => {
              console.log('Canvas查询结果:', res);
              if (res && res[0] && res[0].node) {
                const canvas = res[0].node;
                const ctx = canvas.getContext('2d');
                
                if (!ctx) {
                  reject(new Error('无法获取2D绘图上下文'));
                  return;
                }
                
                // 设置画布尺寸（使用新API）
                const windowInfo = wx.getWindowInfo ? wx.getWindowInfo() : { pixelRatio: 1, windowWidth: 320 };
                const canvasWidth = Math.max(100, (windowInfo.windowWidth || 320) - 64); // 左右各32px边距
                const canvasHeight = Math.max(100, Math.floor(canvasWidth * 1.4)); // 保持PDF比例
                
                // 设置实际画布尺寸
                const dpr = windowInfo.pixelRatio || 1;
                canvas.width = canvasWidth * dpr;
                canvas.height = canvasHeight * dpr;
                
                // 设置CSS尺寸
                this.setData({
                  canvasWidth: canvasWidth,
                  canvasHeight: canvasHeight
                });
                
                console.log('Canvas初始化成功:', { canvasWidth, canvasHeight, dpr });
                resolve({ canvas, ctx });
              } else {
                console.error('无法获取Canvas节点:', res);
                reject(new Error('无法获取Canvas节点，请检查Canvas元素是否正确渲染'));
              }
            });
        });
      } catch (error) {
        console.error('Canvas初始化异常:', error);
        reject(error);
      }
    });
  },

  /**
   * 渲染PDF页面
   */
  async renderPdfPage(pageNum) {
    if (!this.data.pdfFilePath) {
      throw new Error('PDF文件未初始化');
    }
    
    this.setData({
      currentPage: pageNum,
      previewStatusText: `正在渲染第${pageNum}页...`,
      isRendering: true
    });
    
    try {
      // 确保在页面渲染完成后执行
      wx.nextTick(async () => {
        // 初始化画布（如果尚未初始化）
        let canvasObj;
        try {
          canvasObj = await this.initCanvas();
        } catch (initError) {
          console.error('画布初始化失败:', initError);
          this.setData({
            previewStatusText: '画布初始化失败',
            isRendering: false
          });
          throw initError;
        }
        
        if (canvasObj && canvasObj.canvas) {
          const canvas = canvasObj.canvas;
          console.log('获取到Canvas节点:', canvas);
          
          // 使用PDF预览服务渲染页面
          try {
            const result = await pdfPreviewService.renderPage(pageNum, canvas);
            if (result.success) {
              this.setData({
                previewStatusText: `第${pageNum}页`,
                isRendering: false
              });
              
              // 更新缩放级别
              const status = pdfPreviewService.getStatus();
              this.setData({ zoomLevel: status.scale });
            } else {
              throw new Error('页面渲染失败');
            }
          } catch (renderError) {
            console.error(`渲染第${pageNum}页失败:`, renderError);
            this.setData({
              previewStatusText: '渲染失败: ' + renderError.message,
              isRendering: false
            });
            throw renderError;
          }
        } else {
          throw new Error('无法获取Canvas节点');
        }
      });
      
    } catch (error) {
      console.error(`渲染第${pageNum}页失败:`, error);
      this.setData({
        previewStatusText: '渲染失败',
        isRendering: false
      });
      throw error;
    }
  },

  /**
   * 缩放控制
   */
  zoomIn() {
    try {
      const newScale = pdfPreviewService.zoomIn();
      this.setData({ zoomLevel: newScale });
      console.log('放大成功，当前缩放级别:', newScale);
    } catch (error) {
      console.error('放大失败:', error);
    }
  },

  zoomOut() {
    try {
      const newScale = pdfPreviewService.zoomOut();
      this.setData({ zoomLevel: newScale });
      console.log('缩小成功，当前缩放级别:', newScale);
    } catch (error) {
      console.error('缩小失败:', error);
    }
  },

  resetZoom() {
    try {
      const newScale = pdfPreviewService.resetScale();
      this.setData({ zoomLevel: newScale });
      console.log('重置缩放成功，当前缩放级别:', newScale);
    } catch (error) {
      console.error('重置缩放失败:', error);
    }
  },

  /**
   * 页面导航
   */
  previousPage() {
    try {
      if (pdfPreviewService.previousPage()) {
        const newPage = pdfPreviewService.getStatus().currentPage;
        this.setData({ currentPage: newPage });
        this.renderPdfPage(newPage);
        console.log('跳转到上一页:', newPage);
      } else {
        wx.showToast({
          title: '已经是第一页',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('上一页失败:', error);
    }
  },

  nextPage() {
    try {
      if (pdfPreviewService.nextPage()) {
        const newPage = pdfPreviewService.getStatus().currentPage;
        this.setData({ currentPage: newPage });
        this.renderPdfPage(newPage);
        console.log('跳转到下一页:', newPage);
      } else {
        wx.showToast({
          title: '已经是最后一页',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('下一页失败:', error);
    }
  },

  /**
   * Canvas触摸事件处理
   */
  onCanvasTouchStart(e) {
    this.setData({
      touchStartX: e.touches[0].clientX,
      touchStartY: e.touches[0].clientY,
      touchStartTime: Date.now()
    });
  },

  onCanvasTouchMove(e) {
    // 防止页面滚动
    e.preventDefault();
  },

  onCanvasTouchEnd(e) {
    if (!this.data.touchStartX || !this.data.touchStartY) return;
    
    const touchEndX = e.changedTouches[0].clientX;
    const touchEndY = e.changedTouches[0].clientY;
    const touchEndTime = Date.now();
    
    const deltaX = touchEndX - this.data.touchStartX;
    const deltaY = touchEndY - this.data.touchStartY;
    const deltaTime = touchEndTime - this.data.touchStartTime;
    
    // 判断是否为有效的滑动手势
    if (deltaTime < 300 && Math.abs(deltaX) > 50 && Math.abs(deltaY) < 100) {
      if (deltaX > 0) {
        // 向右滑动，显示上一页
        console.log('检测到向右滑动，显示上一页');
        this.previousPage();
      } else {
        // 向左滑动，显示下一页
        console.log('检测到向左滑动，显示下一页');
        this.nextPage();
      }
    }
    
    // 清理触摸状态
    this.setData({
      touchStartX: null,
      touchStartY: null,
      touchStartTime: null
    });
  },

  /**
   * 检查PDF内容质量
   */
  checkPdfContentQuality() {
    try {
      const status = pdfPreviewService.getStatus();
      
      if (status.hasRealContent) {
        // 显示真实内容信息
        wx.showModal({
          title: 'PDF内容检测',
          content: `✓ PDF文件解析成功\n• 总页数: ${status.totalPages}页\n• 文件大小: ${Math.round(status.fileSize / 1024)}KB\n• 内容类型: 真实PDF内容`,
          showCancel: false,
          confirmText: '确定'
        });
      } else {
        // 显示模拟内容信息
        wx.showModal({
          title: 'PDF内容检测',
          content: `⚠ PDF文件解析失败\n• 总页数: ${status.totalPages}页（估算）\n• 文件大小: ${Math.round(status.fileSize / 1024)}KB\n• 内容类型: 模拟内容\n\n可能原因：\n• PDF文件格式不支持\n• 文件损坏\n• 解析器限制`,
          showCancel: false,
          confirmText: '确定'
        });
      }
    } catch (error) {
      console.error('PDF内容质量检测失败:', error);
      wx.showToast({
        title: '检测失败',
        icon: 'none'
      });
    }
  },

  /**
   * 重新解析PDF文件
   */
  async reparsePdf() {
    if (!this.data.pdfFilePath) {
      wx.showToast({
        title: 'PDF文件未准备就绪',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({
      title: '重新解析中...'
    });

    try {
      // 清理之前的PDF数据
      pdfPreviewService.cleanup();
      
      // 重新加载PDF文件
      const pdfInfo = await pdfPreviewService.loadPdf(this.data.pdfFilePath);
      
      // 重新渲染当前页
      const canvasObj = await this.initCanvas();
      await pdfPreviewService.renderPage(this.data.currentPage, canvasObj.canvas);
      
      // 更新状态
      const status = pdfPreviewService.getStatus();
      const statusText = status.hasRealContent ? 
        '重新解析完成（真实PDF内容）' : 
        '重新解析完成（模拟内容）';
      
      this.setData({
        totalPages: pdfInfo.totalPages,
        previewStatusText: statusText
      });
      
      wx.hideLoading();
      wx.showToast({
        title: '重新解析完成',
        icon: 'success'
      });
      
    } catch (error) {
      wx.hideLoading();
      console.error('重新解析PDF失败:', error);
      
      this.setData({
        previewStatusText: '重新解析失败'
      });
      
      wx.showToast({
        title: '重新解析失败',
        icon: 'none'
      });
    }
  },

  /**
   * 返回上一页
   */
  onBack() {
    wx.navigateBack();
  },

  /**
   * 页面卸载时清理资源
   */
  onUnload() {
    // 清理PDF预览器资源
    if (pdfPreviewService) {
      pdfPreviewService.cleanup();
    }
  }
});
