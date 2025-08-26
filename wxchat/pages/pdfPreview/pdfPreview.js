// pages/pdfPreview/pdfPreview.js
const pdfImagePreviewService = require('../../utils/pdfImagePreviewService.js');
const { reportAPI } = require('../../utils/api.js');
const pdfDownloadService = require('../../utils/pdfDownloadService.js');

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

    // 防重：请求中的页
    pendingPages: {}
  },

  async onLoad(options) {
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

    // 初始化尝试加载第1、2页，保证可滑动
    try {
      await this.tryAppendPage(1);
      await this.tryAppendPage(2).catch(() => {});
      this.setData({
        loading: false,
        currentPage: 1,
        scrollIntoViewId: 'page-1'
      });
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
    }
  },

  // 追加一页（探测成功才追加），带防重
  async tryAppendPage(pageIndex) {
    if (pageIndex < 1) return Promise.reject(new Error('invalid'));
    const exists = this.data.pageImages.some(p => p.page === pageIndex);
    if (exists) return Promise.resolve();
    if (this.data.pendingPages[pageIndex]) return Promise.resolve();

    const url = pdfImagePreviewService.getPageImage(pageIndex);
    this.setData({ [`pendingPages[${pageIndex}]`]: true });

    return new Promise((resolve, reject) => {
      wx.getImageInfo({
        src: url,
        success: () => {
          const exists2 = this.data.pageImages.find(p => p.page === pageIndex);
          if (!exists2) {
            const newItem = { page: pageIndex, id: `page-${pageIndex}`, src: url, loaded: false, error: false };
            const pageImages = this.data.pageImages.concat(newItem);
            this.setData({ pageImages, visiblePages: pageImages.length, isLastPage: false });
          }
          this.setData({ [`pendingPages[${pageIndex}]`]: false });
          resolve();
      },
      fail: () => {
          this.setData({ [`pendingPages[${pageIndex}]`]: false, isLastPage: true });
          reject(new Error('last-page'));
        }
      });
    });
  },

  // 滚动到底：继续尝试加载后续5页（带防重）
  async onScrollToLower() {
    if (this.data.isLastPage) return;
    const start = (this.data.pageImages[this.data.pageImages.length - 1]?.page || 0) + 1;
    for (let i = 0; i < BATCH_SIZE; i++) {
      const p = start + i;
      try {
        await this.tryAppendPage(p);
      } catch (e) {
        break;
      }
    }
  },

  // 确保目标页存在（不存在则尝试按需加载）
  async ensurePageExists(targetPage) {
    const exists = this.data.pageImages.some(p => p.page === targetPage);
    if (exists) return true;
    if (this.data.isLastPage) return false;
    try {
      await this.tryAppendPage(targetPage);
      return true;
    } catch (e) {
      return false;
    }
  },

  // 跳转到目标页（若不存在则尝试加载）
  async goToPageOrLoad(target) {
    if (target < 1) return;
    const ok = await this.ensurePageExists(target);
    if (ok) {
      this.setData({ currentPage: target, scrollIntoViewId: `page-${target}` });
    } else {
      wx.showToast({ title: '已是最后一页', icon: 'none' });
    }
  },

  // 触摸手势（左右滑动翻页）
  onTouchStart(e) {
    const t = e.touches && e.touches[0] ? e.touches[0] : { clientX: 0, clientY: 0 };
    this.setData({ touchStartX: t.clientX, touchStartY: t.clientY, touchStartTime: Date.now() });
  },
  async onTouchEnd(e) {
    if (!e.changedTouches || !e.changedTouches[0]) return;
    const end = e.changedTouches[0];
    const deltaX = end.clientX - this.data.touchStartX;
    const deltaY = end.clientY - this.data.touchStartY;
    const deltaTime = Date.now() - this.data.touchStartTime;

    if (deltaTime < 400 && Math.abs(deltaX) > 40 && Math.abs(deltaY) < 60) {
      if (deltaX < 0) {
        await this.goToPageOrLoad(this.data.currentPage + 1);
      } else {
        const prev = this.data.currentPage - 1;
        if (prev >= 1) this.setData({ currentPage: prev, scrollIntoViewId: `page-${prev}` });
      }
    }
  },

  // 图片加载完成/失败回调
  onImageLoad(e) {
    const page = Number(e.currentTarget.dataset.page);
    const key = `pageImages[${this.data.pageImages.findIndex(p=>p.page===page)}].loaded`;
    if (key.endsWith('].loaded')) this.setData({ [key]: true });
  },
  onImageError(e) {
    const page = Number(e.currentTarget.dataset.page);
    const key = `pageImages[${this.data.pageImages.findIndex(p=>p.page===page)}].error`;
    if (key.endsWith('].error')) this.setData({ [key]: true });
  },

  // 缩放（整体图片容器缩放）
  zoomIn() {
    const scale = Math.min(3.0, this.data.pageScale + 0.25);
    this.setData({ pageScale: scale });
  },
  zoomOut() {
    const scale = Math.max(0.5, this.data.pageScale - 0.25);
    this.setData({ pageScale: scale });
  },
  resetZoom() {
    this.setData({ pageScale: 1.0 }); // 重置缩放为1.0
  },

  // 下载（保留）
  async startPdfDownload() {
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
        () => this.setData({ downloadStatus: 'completed', downloadProgress: 100, showDownloadProgress: false }),
        (err) => {
          console.error(err);
          this.setData({ downloadStatus: 'failed', showDownloadProgress: false });
        }
      );
    } catch (e) {
      this.setData({ downloadStatus: 'failed', showDownloadProgress: false });
    }
  },

  onUnload() {}
});