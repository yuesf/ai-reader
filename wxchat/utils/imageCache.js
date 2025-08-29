/**
 * 图片缓存服务
 * 用于缓存生成的报告图片，避免重复生成
 */

class ImageCache {
  constructor() {
    this.cache = new Map();
    this.maxCacheSize = 50; // 最大缓存数量
    this.cacheExpireTime = 24 * 60 * 60 * 1000; // 24小时过期时间
  }

  /**
   * 生成缓存键
   * @param {string} reportId 报告ID
   * @param {number} pageIndex 页面索引
   * @returns {string} 缓存键
   */
  generateCacheKey(reportId, pageIndex) {
    return `report_${reportId}_page_${pageIndex}`;
  }

  /**
   * 设置缓存
   * @param {string} reportId 报告ID
   * @param {number} pageIndex 页面索引
   * @param {string} imagePath 图片路径
   */
  setCache(reportId, pageIndex, imagePath) {
    const key = this.generateCacheKey(reportId, pageIndex);
    const cacheItem = {
      imagePath,
      timestamp: Date.now(),
      reportId,
      pageIndex
    };

    // 检查缓存大小，如果超过限制则清理最旧的缓存
    if (this.cache.size >= this.maxCacheSize) {
      this.cleanOldestCache();
    }

    this.cache.set(key, cacheItem);
    console.log(`[ImageCache] 缓存图片: ${key} -> ${imagePath}`);
  }

  /**
   * 获取缓存
   * @param {string} reportId 报告ID
   * @param {number} pageIndex 页面索引
   * @returns {string|null} 图片路径或null
   */
  getCache(reportId, pageIndex) {
    const key = this.generateCacheKey(reportId, pageIndex);
    const cacheItem = this.cache.get(key);

    if (!cacheItem) {
      return null;
    }

    // 检查是否过期
    if (Date.now() - cacheItem.timestamp > this.cacheExpireTime) {
      console.log(`[ImageCache] 缓存已过期: ${key}`);
      this.cache.delete(key);
      // 尝试删除过期的本地文件
      this.deleteLocalFile(cacheItem.imagePath);
      return null;
    }

    // 检查本地文件是否存在
    try {
      const fileManager = wx.getFileSystemManager();
      fileManager.accessSync(cacheItem.imagePath);
      console.log(`[ImageCache] 命中缓存: ${key} -> ${cacheItem.imagePath}`);
      return cacheItem.imagePath;
    } catch (error) {
      console.log(`[ImageCache] 缓存文件不存在: ${key}`);
      this.cache.delete(key);
      return null;
    }
  }

  /**
   * 清理最旧的缓存
   */
  cleanOldestCache() {
    let oldestKey = null;
    let oldestTime = Date.now();

    for (const [key, item] of this.cache.entries()) {
      if (item.timestamp < oldestTime) {
        oldestTime = item.timestamp;
        oldestKey = key;
      }
    }

    if (oldestKey) {
      const oldestItem = this.cache.get(oldestKey);
      this.cache.delete(oldestKey);
      this.deleteLocalFile(oldestItem.imagePath);
      console.log(`[ImageCache] 清理最旧缓存: ${oldestKey}`);
    }
  }

  /**
   * 清理指定报告的所有缓存
   * @param {string} reportId 报告ID
   */
  clearReportCache(reportId) {
    const keysToDelete = [];
    for (const [key, item] of this.cache.entries()) {
      if (item.reportId === reportId) {
        keysToDelete.push(key);
        this.deleteLocalFile(item.imagePath);
      }
    }

    keysToDelete.forEach(key => {
      this.cache.delete(key);
      console.log(`[ImageCache] 清理报告缓存: ${key}`);
    });
  }

  /**
   * 清理所有缓存
   */
  clearAllCache() {
    for (const [key, item] of this.cache.entries()) {
      this.deleteLocalFile(item.imagePath);
    }
    this.cache.clear();
    console.log('[ImageCache] 清理所有缓存');
  }

  /**
   * 删除本地文件
   * @param {string} filePath 文件路径
   */
  deleteLocalFile(filePath) {
    try {
      const fileManager = wx.getFileSystemManager();
      fileManager.unlinkSync(filePath);
      console.log(`[ImageCache] 删除本地文件: ${filePath}`);
    } catch (error) {
      console.log(`[ImageCache] 删除文件失败: ${filePath}`, error);
    }
  }

  /**
   * 获取缓存统计信息
   * @returns {object} 缓存统计
   */
  getCacheStats() {
    return {
      size: this.cache.size,
      maxSize: this.maxCacheSize,
      items: Array.from(this.cache.entries()).map(([key, item]) => ({
        key,
        reportId: item.reportId,
        pageIndex: item.pageIndex,
        timestamp: item.timestamp,
        age: Date.now() - item.timestamp
      }))
    };
  }
}

// 创建全局单例
const imageCache = new ImageCache();

module.exports = imageCache;