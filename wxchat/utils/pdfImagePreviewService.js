// 图片化PDF预览服务（基于后端已将PDF按页转换为图片）
// 提供图片预览URL构建和缓存管理功能，确保每个报告只生成一次临时图片文件

const { BASE_URL } = require('./api.js');
const imageCache = require('./imageCache');

class PdfImagePreviewService {
  constructor() {
    this.fileId = '';
    this.totalPages = 0;
    // 请求中的页面缓存，防止重复请求
    this.pendingRequests = new Map();
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
   * 获取已缓存的本地图片路径
   * @param {string} fileId - 报告文件ID
   * @param {number} pageIndex - 页码
   * @returns {string|null} 本地图片路径或null
   */
  getCachedLocalImagePath(fileId, pageIndex) {
    return imageCache.getCache(fileId, pageIndex);
  }

  /**
   * 缓存本地图片路径
   * @param {string} fileId - 报告文件ID
   * @param {number} pageIndex - 页码
   * @param {string} localPath - 本地图片路径
   */
  cacheLocalImagePath(fileId, pageIndex, localPath) {
    imageCache.setCache(fileId, pageIndex, localPath);
  }

  /**
   * 清除指定报告的缓存记录（用于强制重新生成）
   * @param {string} fileId - 报告文件ID
   */
  clearReportCache(fileId) {
    imageCache.clearReportCache(fileId);
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
   * @returns {string} 图片URL或本地缓存路径
   */
  getPageImage(pageIndex) {
    const page = Math.max(1, pageIndex);
    
    // 首先检查缓存中是否有本地图片
    const cachedPath = this.getCachedLocalImagePath(this.fileId, page);
    if (cachedPath) {
      console.log(`PdfImagePreviewService: getPageImage using cached image for page ${page}: ${cachedPath}`);
      return cachedPath;
    }
    
    // 生成请求键，用于防止重复请求
    const requestKey = `${this.fileId}_${page}`;
    
    // 检查是否已有相同请求正在进行中
    if (this.pendingRequests.has(requestKey)) {
      console.log(`PdfImagePreviewService: getPageImage reusing pending request for page ${page}`);
      return this.pendingRequests.get(requestKey);
    }
    
    const url = this.buildPageImageUrl(page);
    console.log(`PdfImagePreviewService: getPageImage for page ${page}, url: ${url}`);
    
    // 存储请求URL，防止重复请求
    this.pendingRequests.set(requestKey, url);
    
    // 异步下载并缓存图片
    this.downloadAndCacheImage(url, this.fileId, page);
    
    // 设置一个超时，从pendingRequests中移除，允许将来重新请求
    setTimeout(() => {
      this.pendingRequests.delete(requestKey);
    }, 10000); // 10秒后自动清除
    
    return url;
  }

  /**
   * 下载并缓存图片到本地
   * @param {string} url - 图片URL
   * @param {string} fileId - 文件ID
   * @param {number} pageIndex - 页面索引
   */
  async downloadAndCacheImage(url, fileId, pageIndex) {
    try {
      const downloadTask = wx.downloadFile({
        url: url,
        success: (res) => {
          if (res.statusCode === 200) {
            // 生成本地文件名
            const fileName = `report_${fileId}_page_${pageIndex}.jpg`;
            const localPath = `${wx.env.USER_DATA_PATH}/${fileName}`;
            
            // 保存到本地文件系统
            const fileManager = wx.getFileSystemManager();
            fileManager.saveFile({
              tempFilePath: res.tempFilePath,
              filePath: localPath,
              success: () => {
                // 缓存本地路径
                this.cacheLocalImagePath(fileId, pageIndex, localPath);
                console.log(`PdfImagePreviewService: cached image for page ${pageIndex}: ${localPath}`);
              },
              fail: (error) => {
                console.error(`PdfImagePreviewService: failed to save image for page ${pageIndex}:`, error);
              }
            });
          }
        },
        fail: (error) => {
          console.error(`PdfImagePreviewService: failed to download image for page ${pageIndex}:`, error);
        }
      });
    } catch (error) {
      console.error(`PdfImagePreviewService: error downloading image for page ${pageIndex}:`, error);
    }
  }
}

const pdfImagePreviewService = new PdfImagePreviewService();
module.exports = pdfImagePreviewService;