// 图片化PDF预览服务（基于后端已将PDF按页转换为图片）
// 提供图片预览URL构建和缓存管理功能，确保每个报告只生成一次临时图片文件

const { BASE_URL } = require('./api.js');

class PdfImagePreviewService {
  constructor() {
    this.fileId = '';
    this.totalPages = 0;
    // 全局缓存，记录已生成临时图片的报告文件ID
    // 用于确保每个报告文件只生成一次本地临时图片文件
    this.generatedImageCache = new Set();
    // 本地临时文件路径映射
    this.localImagePaths = new Map();
  }

  /**
   * 设置当前处理的文件ID
   * @param {string} fileId - 报告文件ID
   */
  setFile(fileId) {
    this.fileId = fileId || '';
    this.totalPages = 0;
  }

  /**
   * 检查是否已为指定报告生成过临时图片文件
   * @param {string} fileId - 报告文件ID
   * @returns {boolean} 是否已生成过临时图片
   */
  hasGeneratedImages(fileId) {
    return this.generatedImageCache.has(fileId);
  }

  /**
   * 标记指定报告已生成临时图片文件
   * @param {string} fileId - 报告文件ID
   */
  markImagesAsGenerated(fileId) {
    this.generatedImageCache.add(fileId);
  }

  /**
   * 获取已缓存的本地图片路径
   * @param {string} fileId - 报告文件ID
   * @param {number} pageIndex - 页码
   * @returns {string|undefined} 本地图片路径或undefined
   */
  getCachedLocalImagePath(fileId, pageIndex) {
    const key = `${fileId}_${pageIndex}`;
    return this.localImagePaths.get(key);
  }

  /**
   * 缓存本地图片路径
   * @param {string} fileId - 报告文件ID
   * @param {number} pageIndex - 页码
   * @param {string} localPath - 本地图片路径
   */
  cacheLocalImagePath(fileId, pageIndex, localPath) {
    const key = `${fileId}_${pageIndex}`;
    this.localImagePaths.set(key, localPath);
    console.log(`Cached local image path for ${key}: ${localPath}`);
  }

  /**
   * 清除指定报告的缓存记录（用于强制重新生成）
   * @param {string} fileId - 报告文件ID
   */
  clearReportCache(fileId) {
    // 清除生成标记
    this.generatedImageCache.delete(fileId);
    
    // 清除本地路径缓存
    for (const key of this.localImagePaths.keys()) {
      if (key.startsWith(`${fileId}_`)) {
        this.localImagePaths.delete(key);
      }
    }
  }

  /**
   * 获取PDF总页数
   * @param {Function} fetcher - 获取PDF信息的函数
   * @returns {Promise<number>} 总页数
   */
  async fetchTotalPages(fetcher) {
    if (!this.fileId) throw new Error('fileId 为空');
    if (this.totalPages > 0) {
      console.log('PdfImagePreviewService: fetchTotalPages from cache', { totalPages: this.totalPages });
      return this.totalPages;
    }

    if (typeof fetcher === 'function') {
      console.log('PdfImagePreviewService: fetchTotalPages from API, fileId:', this.fileId);
      const info = await fetcher(this.fileId);
      if (info && info.code === 200 && info.data && info.data.totalPages) {
        this.totalPages = info.data.totalPages;
        console.log('PdfImagePreviewService: fetchTotalPages from API success', { totalPages: this.totalPages });
      }
    }

    if (!this.totalPages) {
      // 兜底：未知页数时，先按 1 页处理，待首张加载成功后再更新
      this.totalPages = 1;
      console.log('PdfImagePreviewService: fetchTotalPages fallback to 1');
    }
    return this.totalPages;
  }

  /**
   * 获取PDF总页数（同步）
   * @returns {number} 总页数
   */
  getTotalPages() {
    return this.totalPages;
  }

  /**
   * 设置PDF总页数
   * @param {number} totalPages - 总页数
   */
  setTotalPages(totalPages) {
    if (totalPages > 0) {
      this.totalPages = totalPages;
    }
  }

  /**
   * 构造某页图片URL
   * @param {number} pageIndex - 页码
   * @returns {string} 图片URL
   */
  buildPageImageUrl(pageIndex) {
    if (!this.fileId) throw new Error('fileId 为空');
    const page = Math.max(1, pageIndex);
    const url = `${BASE_URL}/v1/pdf/page/${this.fileId}/${page}`;
    console.log(`PdfImagePreviewService: buildPageImageUrl for page ${page}, url: ${url}`);
    return url;
  }

  /**
   * 获取某页图片（返回可直接用于 <image> 的 src）
   * @param {number} pageIndex - 页码
   * @returns {string} 图片URL
   */
  getPageImage(pageIndex) {
    const page = Math.max(1, pageIndex);
    const url = this.buildPageImageUrl(page);
    console.log(`PdfImagePreviewService: getPageImage for page ${page}, url: ${url}`);
    return url;
  }
}

const pdfImagePreviewService = new PdfImagePreviewService();
module.exports = pdfImagePreviewService;