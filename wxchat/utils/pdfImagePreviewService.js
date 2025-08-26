// 图片化PDF预览服务（基于后端已将PDF按页转换为图片）

const { BASE_URL } = require('./api.js');

class PdfImagePreviewService {
  constructor() {
    this.fileId = '';
    this.totalPages = 0;
    this.pageImageCache = new Map();
  }

  setFile(fileId) {
    this.fileId = fileId || '';
    this.totalPages = 0;
    this.pageImageCache.clear();
  }

  // 获取页数（优先调 /v1/pdf/info/{fileId}，否则允许调用方传入兜底）
  async fetchTotalPages(fetcher) {
    if (!this.fileId) throw new Error('fileId 为空');
    if (this.totalPages > 0) return this.totalPages;

    if (typeof fetcher === 'function') {
      const info = await fetcher(this.fileId);
      if (info && info.code === 200 && info.data && info.data.totalPages) {
        this.totalPages = info.data.totalPages;
      }
    }

    if (!this.totalPages) {
      // 兜底：未知页数时，先按 1 页处理，待首张加载成功后再更新
      this.totalPages = 1;
    }
    return this.totalPages;
  }

  // 构造某页图片URL（如后端提供 /v1/pdf/page/{fileId}/{page}.png）
  buildPageImageUrl(pageIndex) {
    if (!this.fileId) throw new Error('fileId 为空');
    const page = Math.max(1, pageIndex);
    return `${BASE_URL}/v1/pdf/page/${this.fileId}/${page}`;
  }

  // 获取某页图片（返回可直接用于 <image> 的 src）
  getPageImage(pageIndex) {
    const page = Math.max(1, pageIndex);
    if (this.pageImageCache.has(page)) return this.pageImageCache.get(page);
    const url = this.buildPageImageUrl(page);
    this.pageImageCache.set(page, url);
    return url;
  }
}

const pdfImagePreviewService = new PdfImagePreviewService();
module.exports = pdfImagePreviewService;
