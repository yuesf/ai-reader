// 图片化PDF预览服务（基于后端已将PDF按页转换为图片）

const { BASE_URL } = require('./api.js');

class PdfImagePreviewService {
  constructor() {
    this.fileId = '';
    this.totalPages = 0;
  }

  setFile(fileId) {
    this.fileId = fileId || '';
    this.totalPages = 0;
    console.log('PdfImagePreviewService: setFile', { fileId });
  }

  // 获取页数（优先调 /v1/pdf/info/{fileId}，否则允许调用方传入兜底）
  async fetchTotalPages(fetcher) {
    if (!this.fileId) throw new Error('fileId 为空');
    if (this.totalPages > 0) {
      console.log('PdfImagePreviewService: fetchTotalPages from cache', { totalPages: this.totalPages });
      return this.totalPages;
    }

    if (typeof fetcher === 'function') {
      const info = await fetcher(this.fileId);
      if (info && info.code === 200 && info.data && info.data.totalPages) {
        this.totalPages = info.data.totalPages;
        console.log('PdfImagePreviewService: fetchTotalPages from API', { totalPages: this.totalPages });
      }
    }

    if (!this.totalPages) {
      // 兜底：未知页数时，先按 1 页处理，待首张加载成功后再更新
      this.totalPages = 1;
      console.log('PdfImagePreviewService: fetchTotalPages fallback to 1');
    }
    return this.totalPages;
  }

  // 构造某页图片URL（如后端提供 /v1/pdf/page/{fileId}/{page}.png）
  buildPageImageUrl(pageIndex) {
    if (!this.fileId) throw new Error('fileId 为空');
    const page = Math.max(1, pageIndex);
    const url = `${BASE_URL}/v1/pdf/page/${this.fileId}/${page}`;
    console.log('PdfImagePreviewService: buildPageImageUrl', { pageIndex, url });
    return url;
  }

  // 获取某页图片（返回可直接用于 <image> 的 src）
  getPageImage(pageIndex) {
    const page = Math.max(1, pageIndex);
    const url = this.buildPageImageUrl(page);
    console.log('PdfImagePreviewService: getPageImage', { pageIndex, url });
    return url;
  }
}

const pdfImagePreviewService = new PdfImagePreviewService();
module.exports = pdfImagePreviewService;