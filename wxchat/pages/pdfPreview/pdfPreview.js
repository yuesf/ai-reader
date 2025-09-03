// pages/pdfPreview\pdfPreview.js
const pdfImagePreviewService = require('../../utils/pdfImagePreviewService.js');
const { reportAPI } = require('../../utils/api.js');
const pdfDownloadService = require('../../utils/pdfDownloadService.js');
const { trackPage, trackClick } = require('../../utils/tracking/index.js');

const BATCH_SIZE = 5;

Page({
  data: {
    reportId: '',
    fileId: '',
    title: '',
    
    // 下载显示（保留）
    showDownloadProgress: false,
    downloadProgress: 0,
    downloadStatus: '',

    // 图片预览
    totalPages: 0, // 展示用
    pageImages: [], // { page, id, src, loaded, error }
    visiblePages: 0,
    pageScale: 1.0, // 修改默认缩放比例为1.0
    currentPage: 1,
    scrollIntoViewId: '',

    // 手势
    touchStartX: 0,
    touchStartY: 0,
    touchStartTime: 0,

    // UI
    loading: true,
    isLastPage: false,
    showEndLine: false, // 添加底线提示显示标志

    // 防重：请求中的页
    pendingPages: {},
    
    // 防止重复触发滑动事件
    isSwiping: false,

    // 滚动位置控制
    scrollLeft: 0,
    scrollTop: 0
  },

  async onLoad(options) {
    // 页面浏览埋点
    trackPage('pdfPreview', 'PDF预览页', { 
      reportId: options?.reportId, 
      fileId: options?.fileId 
    });
    
    // 检查登录状态
    if (!this.checkLoginStatus()) {
      return;
    }
    
    const { reportId, fileId, title } = options || {};
    this.setData({
      reportId: reportId || '',
      fileId: fileId || '',
      title: decodeURIComponent(title || 'PDF预览（图片）')
    });

    wx.setNavigationBarTitle({ title: this.data.title });

    if (!this.data.fileId) {
      wx.showToast({ title: '缺少文件ID', icon: 'none' });
      return;
    }

    // 绑定文件
    pdfImagePreviewService.setFile(this.data.fileId);

    console.log('PdfPreview: onLoad, initializing pages');
    // 初始化尝试加载第1、2页，保证可滑动
    try {
      // 获取报告总页数
      await pdfImagePreviewService.fetchTotalPages(reportAPI.getPdfInfo);
      const totalPages = pdfImagePreviewService.getTotalPages();
      console.log('PdfPreview: got total pages', totalPages);
      
      // 使用 Promise.all 并行加载第一页和第二页
      await Promise.all([
        this.tryAppendPage(1),
        this.tryAppendPage(2)
      ]);

      // 检查是否成功加载了至少一页
      if (this.data.pageImages.length === 0) {
        throw new Error('no pages loaded');
      }

      this.setData({
        loading: false,
        currentPage: 1,
        scrollIntoViewId: 'page-1',
        totalPages: totalPages
      });
      console.log('PdfPreview: onLoad completed, pages loaded');
    } catch (e) {
      console.error('PdfPreview: onLoad error', e);
      this.setData({ loading: false });
      wx.showToast({ 
        title: e.message === 'no pages loaded' ? '未加载到任何页面' : '加载失败', 
        icon: 'none' 
      });
    }
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
  },

  // 追加一页（探测成功才追加），带防重
  async tryAppendPage(pageIndex) {
    console.log(`PdfPreview: tryAppendPage called for page ${pageIndex}`);
    if (pageIndex < 1) return Promise.reject(new Error('invalid'));
    
    // Check if page already exists or is pending
    const exists = this.data.pageImages.some(p => p.page === pageIndex);
    if (exists) {
      console.log(`PdfPreview: tryAppendPage page ${pageIndex} already exists`);
      return Promise.resolve();
    }
    if (this.data.pendingPages[pageIndex]) {
      console.log(`PdfPreview: tryAppendPage page ${pageIndex} is pending`);
      return Promise.resolve();
    }
    
    // Check if page exceeds total pages
    if (this.data.totalPages > 0 && pageIndex > this.data.totalPages) {
      console.log(`PdfPreview: tryAppendPage page ${pageIndex} exceeds total pages ${this.data.totalPages}`);
      return Promise.reject(new Error('exceeds total pages'));
    }

    // Mark page as pending BEFORE generating image URL
    this.setData({ [`pendingPages[${pageIndex}]`]: true });
    
    // 首先检查缓存
    const cachedPath = pdfImagePreviewService.getCachedLocalImagePath(this.data.fileId, pageIndex);
    let src;
    
    if (cachedPath) {
      console.log(`PdfPreview: using cached image for page ${pageIndex}: ${cachedPath}`);
      src = cachedPath;
      // 直接添加到页面列表，无需验证
      const newItem = { 
        page: pageIndex, 
        id: `page-${pageIndex}`, 
        src: src, 
        loaded: true, 
        error: false 
      };
      const pageImages = this.data.pageImages.concat(newItem);
      this.setData({ 
        pageImages, 
        visiblePages: pageImages.length, 
        isLastPage: false,
        [`pendingPages[${pageIndex}]`]: false 
      });
      console.log(`PdfPreview: tryAppendPage added cached page ${pageIndex}`);
      return Promise.resolve();
    } else {
      src = pdfImagePreviewService.getPageImage(pageIndex);
      if (!src) {
        console.error(`PdfPreview: getPageImage returned null or empty for page ${pageIndex}`);
        this.setData({ [`pendingPages[${pageIndex}]`]: false });
        return Promise.reject(new Error('empty image src'));
      }
    }

    console.log(`PdfPreview: tryAppendPage setting pending for page ${pageIndex}`);

    return new Promise((resolve, reject) => {
      wx.getImageInfo({
        src: src,
        success: (res) => {
          console.log(`PdfPreview: wx.getImageInfo success for page ${pageIndex}`, res);
          const exists2 = this.data.pageImages.find(p => p.page === pageIndex);
          if (!exists2) {
            const newItem = { 
              page: pageIndex, 
              id: `page-${pageIndex}`, 
              src: src, 
              loaded: false, 
              error: false 
            };
            const pageImages = this.data.pageImages.concat(newItem);
            this.setData({ pageImages, visiblePages: pageImages.length, isLastPage: false });
            this.setData({ [`pendingPages[${pageIndex}]`]: false });
            console.log(`PdfPreview: tryAppendPage added page ${pageIndex}`);
            resolve();
          } else {
            this.setData({ [`pendingPages[${pageIndex}]`]: false });
            console.log(`PdfPreview: tryAppendPage page ${pageIndex} already exists in second check`);
            resolve();
          }
        },
        fail: (err) => {
          console.error(`PdfPreview: wx.getImageInfo failed for page ${pageIndex}`, err);
          this.setData({ [`pendingPages[${pageIndex}]`]: false });
          
          // When we get an error for a page, check if we've reached the end
          // Only mark as last page if we were trying to load a page sequentially
          const maxPage = this.data.pageImages.reduce((max, p) => Math.max(max, p.page), 0);
          if (this.data.totalPages > 0 && pageIndex > this.data.totalPages) {
            // We know the exact total pages and this page exceeds it
            this.setData({ isLastPage: true, showEndLine: true });
          } else if (pageIndex <= maxPage + 2) { // Allow some gap for async loading
            this.setData({ isLastPage: true, showEndLine: true });
          }
          reject(new Error('last-page'));
        }
      });
    });
  },

  // 滚动到底：继续尝试加载后续5页（带防重）
  async onScrollToLower() {
    console.log('PdfPreview: onScrollToLower called');
    if (this.data.isLastPage) {
      // 当已经是最后一页时，显示底线提示
      this.setData({ showEndLine: true });
      console.log('PdfPreview: onScrollToLower reached last page');
      return;
    }
    const start = (this.data.pageImages[this.data.pageImages.length - 1]?.page || 0) + 1;
    console.log(`PdfPreview: onScrollToLower starting from page ${start}`);
    for (let i = 0; i < BATCH_SIZE; i++) {
      const p = start + i;
      
      // Check if we know the total pages and have exceeded them
      if (this.data.totalPages > 0 && p > this.data.totalPages) {
        this.setData({ isLastPage: true, showEndLine: true });
        console.log(`PdfPreview: onScrollToLower reached end at page ${p} (total: ${this.data.totalPages})`);
        break;
      }
      
      try {
        await this.tryAppendPage(p);
      } catch (e) {
        // 当加载失败时，表示已到最后一页，显示底线提示
        this.setData({ isLastPage: true, showEndLine: true });
        console.log(`PdfPreview: onScrollToLower reached end at page ${p}`);
        break;
      }
    }
  },

  // 确保目标页存在（不存在则尝试按需加载）
  async ensurePageExists(targetPage) {
    console.log(`PdfPreview: ensurePageExists called for page ${targetPage}`);
    
    // Check if target page exceeds known total pages
    if (this.data.totalPages > 0 && targetPage > this.data.totalPages) {
      console.log(`PdfPreview: ensurePageExists page ${targetPage} exceeds total pages ${this.data.totalPages}`);
      wx.showToast({ title: `超出最大页数 ${this.data.totalPages}`, icon: 'none' });
      return false;
    }
    
    const exists = this.data.pageImages.some(p => p.page === targetPage);
    if (exists) {
      console.log(`PdfPreview: ensurePageExists page ${targetPage} already exists`);
      return true;
    }
    if (this.data.isLastPage) {
      console.log(`PdfPreview: ensurePageExists reached last page`);
      return false;
    }
    try {
      await this.tryAppendPage(targetPage);
      console.log(`PdfPreview: ensurePageExists page ${targetPage} loaded successfully`);
      return true;
    } catch (e) {
      console.error(`PdfPreview: ensurePageExists failed for page ${targetPage}`, e);
      return false;
    }
  },

  // 跳转到目标页（若不存在则尝试加载）
  async goToPageOrLoad(target) {
    console.log(`PdfPreview: goToPageOrLoad called for page ${target}`);
    if (target < 1) return;
    const ok = await this.ensurePageExists(target);
    if (ok) {
      this.setData({ currentPage: target, scrollIntoViewId: `page-${target}` });
      console.log(`PdfPreview: goToPageOrLoad navigated to page ${target}`);
    } else {
      wx.showToast({ title: '已是最后一页', icon: 'none' });
    }
  },

  // 触摸手势（左右滑动翻页）- 仅在未缩放时启用
  onTouchStart(e) {
    // 如果已缩放，不处理翻页手势，让scroll-view处理滚动
    if (this.data.pageScale > 1.0) {
      return;
    }
    const t = e.touches && e.touches[0] ? e.touches[0] : { clientX: 0, clientY: 0 };
    this.setData({ touchStartX: t.clientX, touchStartY: t.clientY, touchStartTime: Date.now() });
  },
  async onTouchEnd(e) {
    // 如果已缩放，不处理翻页手势，让scroll-view处理滚动
    if (this.data.pageScale > 1.0) {
      return;
    }
    
    // 防止重复触发滑动事件
    if (this.data.isSwiping) {
      console.log('PdfPreview: onTouchEnd skipped due to isSwiping');
      return;
    }
    
    if (!e.changedTouches || !e.changedTouches[0]) return;
    const end = e.changedTouches[0];
    const deltaX = end.clientX - this.data.touchStartX;
    const deltaY = end.clientY - this.data.touchStartY;
    const deltaTime = Date.now() - this.data.touchStartTime;

    // 标记正在滑动，防止重复触发
    this.setData({ isSwiping: true });
    
    if (deltaTime < 400 && Math.abs(deltaX) > 40 && Math.abs(deltaY) < 60) {
      if (deltaX < 0) {
        console.log('PdfPreview: onTouchEnd detected left swipe');
        await this.goToPageOrLoad(this.data.currentPage + 1);
      } else {
        console.log('PdfPreview: onTouchEnd detected right swipe');
        const prev = this.data.currentPage - 1;
        if (prev >= 1) this.setData({ currentPage: prev, scrollIntoViewId: `page-${prev}` });
      }
    }
    
    // 延迟重置滑动状态，防止误触
    setTimeout(() => {
      this.setData({ isSwiping: false });
    }, 50);
  },

  // 图片加载完成/失败回调
  onImageLoad(e) {
    const page = Number(e.currentTarget.dataset.page);
    console.log(`PdfPreview: onImageLoad called for page ${page}`);
    const key = `pageImages[${this.data.pageImages.findIndex(p=>p.page===page)}].loaded`;
    if (key.endsWith('].loaded')) this.setData({ [key]: true });
    
    // 清除pendingRequests中的记录，允许将来重新请求
    const requestKey = `${this.data.fileId}_${page}`;
    pdfImagePreviewService.pendingRequests.delete(requestKey);
  },
  onImageError(e) {
    const page = Number(e.currentTarget.dataset.page);
    console.log(`PdfPreview: onImageError called for page ${page}`);
    const key = `pageImages[${this.data.pageImages.findIndex(p=>p.page===page)}].error`;
    if (key.endsWith('].error')) this.setData({ [key]: true });
    
    // 清除pendingRequests中的记录，允许将来重新请求
    const requestKey = `${this.data.fileId}_${page}`;
    pdfImagePreviewService.pendingRequests.delete(requestKey);
  },

  // 缩放（整体图片容器缩放）
  zoomIn() {
    // 放大埋点
    trackClick('zoom_in', 'pdfPreview', {
      reportId: this.data.reportId,
      currentScale: this.data.pageScale,
      currentPage: this.data.currentPage
    });
    
    const scale = Math.min(3.0, this.data.pageScale + 0.25);
    this.setData({ pageScale: scale });
  },
  zoomOut() {
    // 缩小埋点
    trackClick('zoom_out', 'pdfPreview', {
      reportId: this.data.reportId,
      currentScale: this.data.pageScale,
      currentPage: this.data.currentPage
    });
    
    const scale = Math.max(0.5, this.data.pageScale - 0.25);
    this.setData({ pageScale: scale });
  },
  resetZoom() {
    // 重置缩放埋点
    trackClick('reset_zoom', 'pdfPreview', {
      reportId: this.data.reportId,
      currentPage: this.data.currentPage
    });
    
    this.setData({ pageScale: 1.0, scrollLeft: 0, scrollTop: 0 });
  },

  // 浮动控制按钮事件处理
  onZoomIn(e) {
    console.log('PdfPreview: onZoomIn called', e.detail);
    this.zoomIn();
  },

  onZoomOut(e) {
    console.log('PdfPreview: onZoomOut called', e.detail);
    this.zoomOut();
  },

  onReset(e) {
    console.log('PdfPreview: onReset called', e.detail);
    this.setData({ 
      pageScale: 1.0, 
      scrollLeft: 0, 
      scrollTop: 0
    });
    wx.showToast({
      title: '回到首页',
      icon: 'success',
      duration: 1000
    });
  },

  // 清除当前报告的缓存（用于调试或强制刷新）
  clearCurrentReportCache() {
    // 清除缓存埋点
    trackClick('clear_cache', 'pdfPreview', {
      reportId: this.data.reportId,
      fileId: this.data.fileId
    });
    
    if (this.data.fileId) {
      pdfImagePreviewService.clearReportCache(this.data.fileId);
      wx.showToast({
        title: '缓存已清除',
        icon: 'success'
      });
      console.log('PdfPreview: cleared cache for current report');
    }
  },

  // 下载（保留）
  async startPdfDownload() {
    // PDF下载埋点
    trackClick('pdf_download', 'pdfPreview', {
      reportId: this.data.reportId,
      fileId: this.data.fileId,
      title: this.data.title
    });
    
    const { fileId, title } = this.data;
    if (!fileId) return;
    if (!pdfDownloadService || typeof pdfDownloadService.startDownload !== 'function') {
      wx.showToast({ title: '下载服务不可用', icon: 'none' });
      return;
    }
    
    this.setData({ showDownloadProgress: true, downloadStatus: 'downloading', downloadProgress: 0 });
    try {
      await pdfDownloadService.startDownload(
        fileId,
        `${title}.pdf`,
        (progress) => this.setData({ downloadProgress: progress }),
        (filePath) => {
          this.setData({ downloadStatus: 'completed', downloadProgress: 100, showDownloadProgress: false });
          wx.showToast({ title: '下载完成', icon: 'success' });
        },
        (err) => {
          console.error(err);
          this.setData({ downloadStatus: 'failed', showDownloadProgress: false });
          wx.showToast({ title: '下载失败', icon: 'none' });
        }
      );
    } catch (e) {
      this.setData({ downloadStatus: 'failed', showDownloadProgress: false });
      wx.showToast({ title: '下载异常', icon: 'none' });
    }
  },

  onUnload() {
    console.log('PdfPreview: onUnload called');
    
    // 清除所有与当前文件相关的pendingRequests
    if (this.data.fileId) {
      const prefix = `${this.data.fileId}_`;
      // 遍历并删除所有与当前文件相关的pendingRequests
      for (const key of pdfImagePreviewService.pendingRequests.keys()) {
        if (key.startsWith(prefix)) {
          pdfImagePreviewService.pendingRequests.delete(key);
        }
      }
      console.log('PdfPreview: onUnload cleared all pending requests for current file');
    }
  }
});