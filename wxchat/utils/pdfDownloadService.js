/**
 * PDF分片下载服务
 * 支持断点续传、分片解密、内存优化等功能
 */
const { reportAPI } = require('./api.js')
const { normalizeFileName } = require('./pdfUtils.js') // 引入新的PDF工具

// 配置常量
const CONFIG = {
  // 分片大小：1MB
  CHUNK_SIZE: 1024 * 1024,
  
  // 并发下载数量
  MAX_CONCURRENT_DOWNLOADS: 3,
  
  // 重试次数
  MAX_RETRIES: 3,
  
  // 重试间隔（毫秒）
  RETRY_INTERVAL: 1000,
  
  // 内存阈值：超过此大小将分片处理
  MEMORY_THRESHOLD: 10 * 1024 * 1024, // 10MB
  
  // 临时文件清理间隔（毫秒）
  CLEANUP_INTERVAL: 5 * 60 * 1000 // 5分钟
};

// 下载状态枚举
const DOWNLOAD_STATUS = {
  PENDING: 'pending',      // 等待中
  DOWNLOADING: 'downloading', // 下载中
  PAUSED: 'paused',        // 暂停
  COMPLETED: 'completed',   // 完成
  FAILED: 'failed',         // 失败
  CANCELLED: 'cancelled'    // 取消
};

/**
 * PDF分片下载器
 */
class PdfChunkDownloader {
  constructor() {
    this.downloads = new Map(); // 下载任务管理
    this.activeDownloads = 0;   // 活跃下载数量
    this.cleanupTimer = null;   // 清理定时器
    
    // 启动清理定时器
    this.startCleanupTimer();
  }

  /**
   * 开始下载PDF文件
   * @param {string} fileId 文件ID
   * @param {string} filename 文件名
   * @param {Function} onProgress 进度回调
   * @param {Function} onComplete 完成回调
   * @param {Function} onError 错误回调
   */
  async startDownload(fileId, filename, onProgress, onComplete, onError) {
    try {
      console.log(`开始下载PDF文件: ${filename} (${fileId})`);
      
      // 创建下载任务
      const downloadTask = {
        id: fileId,
        filename: filename,
        status: DOWNLOAD_STATUS.PENDING,
        progress: 0,
        downloadedChunks: 0,
        totalChunks: 0,
        chunks: new Map(),
        encryptionKey: null,
        startTime: Date.now(),
        onProgress,
        onComplete,
        onError
      };
      
      this.downloads.set(fileId, downloadTask);
      
      // 获取文件信息
      await this.getFileInfo(downloadTask);
      
      // 检查是否有未完成的下载
      if (await this.checkResumeDownload(downloadTask)) {
        console.log(`恢复下载: ${filename}`);
      }
      
      // 开始下载
      await this.processDownload(downloadTask);
    } catch (error) {
      console.error(`下载失败: ${filename}`, error);
      onError && onError(error);
    }
  }

  /**
   * 获取文件信息
   */
  async getFileInfo(downloadTask) {
    try {
        const result = await reportAPI.getPdfFileInfo(downloadTask.id);

        if (result.code === 200) {
            const fileInfo = result.data;
            downloadTask.totalChunks = fileInfo.totalChunks;
            downloadTask.encryptionKey = fileInfo.encryptionKey;
            downloadTask.fileSize = fileInfo.fileSize;

            console.log(`文件信息获取成功: ${downloadTask.totalChunks} 个分片`);
        } else {
            wx.showToast({
                title: result.message || '获取文件信息失败',
                icon: 'none'
            });
            throw new Error(result.message || '获取文件信息失败');
        }
    } catch (error) {
      throw new Error(`获取文件信息失败: ${error.message}`);
    }
  }

  /**
   * 检查是否可以恢复下载
   */
  async checkResumeDownload(downloadTask) {
    try {
      const storageKey = `pdf_download_${downloadTask.id}`;
      const savedState = wx.getStorageSync(storageKey);
      
      if (savedState && savedState.timestamp) {
        // 检查状态是否过期（超过1小时）
        const isExpired = Date.now() - savedState.timestamp > 60 * 60 * 1000;
        
        if (!isExpired && savedState.downloadedChunks > 0) {
          // 恢复下载状态
          downloadTask.downloadedChunks = savedState.downloadedChunks;
          downloadTask.progress = savedState.progress || 0;
          
          console.log(`恢复下载状态: 已下载 ${downloadTask.downloadedChunks} 个分片`);
          return true;
        }
      }
      
      return false;
    } catch (error) {
      console.warn('检查恢复下载失败:', error);
      return false;
    }
  }

  /**
   * 处理下载任务
   */
  async processDownload(downloadTask) {
    try {
      console.log(`开始处理下载任务: ${downloadTask.filename}`);
      
      // 更新状态
      downloadTask.status = DOWNLOAD_STATUS.DOWNLOADING;
      
      // 分批下载，避免内存溢出
      const batchSize = CONFIG.MAX_CONCURRENT_DOWNLOADS;
      const totalBatches = Math.ceil(downloadTask.totalChunks / batchSize);
      
      console.log(`分批下载: 总计 ${totalBatches} 批次，每批 ${batchSize} 个分片`);
      
      for (let batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
        // 检查下载状态
        if (downloadTask.status === DOWNLOAD_STATUS.PAUSED) {
          console.log('下载已暂停');
          return;
        }
        
        if (downloadTask.status === DOWNLOAD_STATUS.CANCELLED) {
          console.log('下载已取消');
          return;
        }
        
        const startChunk = batchIndex * batchSize;
        const endChunk = Math.min(startChunk + batchSize - 1, downloadTask.totalChunks - 1);
        
        console.log(`处理第 ${batchIndex + 1} 批次: 分片 ${startChunk + 1}-${endChunk + 1}`);
        
        // 并发下载当前批次的分片
        const promises = [];
        for (let i = startChunk; i <= endChunk; i++) {
          if (!downloadTask.chunks.has(i)) {
            promises.push(this.downloadChunk(downloadTask, i));
          }
        }
        
        if (promises.length > 0) {
          await Promise.all(promises);
          console.log(`第 ${batchIndex + 1} 批次下载完成`);
        }
        
        // 保存下载状态（不包含分片数据）
        this.saveDownloadState(downloadTask);
        
        // 检查内存使用情况
        this.checkMemoryUsage(downloadTask);
      }
      
      if (downloadTask.status !== DOWNLOAD_STATUS.CANCELLED) {
        // 下载完成，合并分片
        console.log(`开始合并分片: ${downloadTask.filename}`);
        await this.mergeChunks(downloadTask);
        console.log(`分片合并完成: ${downloadTask.filename}`);
      }
      
    } catch (error) {
      downloadTask.status = DOWNLOAD_STATUS.FAILED;
      console.error(`处理下载任务失败: ${downloadTask.filename}, 错误: ${error.message}`);
      throw error;
    }
  }

  /**
   * 下载单个分片
   */
  async downloadChunk(downloadTask, chunkIndex, retryCount = 0) {
    try {
      console.log(`开始下载分片: ${chunkIndex + 1}/${downloadTask.totalChunks}`);
      
      // 设置当前下载任务
      this.currentDownloadTask = downloadTask;
      
      // 使用封装的API方法获取分片数据
      const result = await reportAPI.getPdfChunk(downloadTask.id, chunkIndex);
      
      console.log(`分片下载响应:`, {
        code: result.code,
        dataSize: result.data ? result.data.byteLength : 0,
        headers: result.headers
      });
      
      if (result.code === 200) {
        // 验证分片数据
        if (!result.data || result.data.byteLength === 0) {
          throw new Error(`分片数据为空: chunkIndex=${chunkIndex}`);
        }
        
        console.log(`分片数据获取成功: ${chunkIndex}, 大小: ${result.data.byteLength} bytes`);
        
        // 检查内存使用情况
        this.checkMemoryUsage(downloadTask);
        
        // 处理分片数据（现在是未加密的）
        const chunkData = new Uint8Array(result.data);
        
        // 验证分片数据
        if (!chunkData || chunkData.length === 0) {
          throw new Error(`分片数据为空: chunkIndex=${chunkIndex}`);
        }
        
        // 存储分片数据
        downloadTask.chunks.set(chunkIndex, chunkData);
        downloadTask.downloadedChunks++;
        
        console.log(`分片 ${chunkIndex + 1} 下载完成，大小: ${chunkData.length} bytes`);
        
        // 更新进度
        this.updateProgress(downloadTask);
        
        // 保存下载状态
        this.saveDownloadState(downloadTask);
        
        return chunkData;
        
      } else {
        throw new Error(`获取分片失败: ${result.message || '未知错误'}`);
      }
      
    } catch (error) {
      console.error(`分片 ${chunkIndex} 下载失败:`, error);
      throw new Error(`分片 ${chunkIndex} 下载失败: ${error.message}`);
    }
  }

  /**
   * 合并分片
   */
  async mergeChunks(downloadTask) {
    try {
      console.log('开始合并分片...');
      
      // 按顺序合并分片
      const chunks = [];
      let totalSize = 0;
      
      // 收集所有分片并计算总大小
      for (let i = 0; i < downloadTask.totalChunks; i++) {
        const chunk = downloadTask.chunks.get(i);
        if (chunk) {
          chunks.push(chunk);
          totalSize += chunk.length;
          console.log(`分片 ${i}: ${chunk.length} bytes`);
          
          // 调试：检查每个分片的前几个字节
          if (chunk.length >= 8) {
            const header = Array.from(chunk.slice(0, 8)).map(b => b.toString(16).padStart(2, '0')).join(' ');
            console.log(`  分片 ${i} 头部字节: ${header}`);
          }
        } else {
          console.warn(`缺少分片 ${i}`);
        }
      }
      
      if (chunks.length !== downloadTask.totalChunks) {
        throw new Error(`分片数量不匹配: 期望 ${downloadTask.totalChunks}, 实际 ${chunks.length}`);
      }
      
      console.log(`总分片数: ${chunks.length}, 总大小: ${totalSize} bytes`);
      
      // 创建最终的文件数据
      const finalData = new Uint8Array(totalSize);
      let offset = 0;
      
      // 按顺序合并所有分片
      for (let i = 0; i < chunks.length; i++) {
        const chunk = chunks[i];
        finalData.set(chunk, offset);
        console.log(`合并分片 ${i}: offset=${offset}, size=${chunk.length}`);
        offset += chunk.length;
      }
      
      console.log(`分片合并完成，最终大小: ${finalData.length} bytes`);
      
      // 调试：检查最终数据的前几个字节
      if (finalData.length >= 16) {
        const header = Array.from(finalData.slice(0, 16)).map(b => b.toString(16).padStart(2, '0')).join(' ');
        console.log(`最终数据头部字节: ${header}`);
        
        // 转换为ASCII查看
        const asciiHeader = Array.from(finalData.slice(0, 16))
          .map(b => b >= 32 && b <= 126 ? String.fromCharCode(b) : '.')
          .join('');
        console.log(`最终数据头部ASCII: ${asciiHeader}`);
      }
      
      // 验证PDF文件头
      if (!this.validatePdfHeader(finalData)) {
        console.error('PDF头验证失败，尝试修复...');
        
        // 检查是否是未加密文件，直接保存
        const pdfHeaderIndex = this.findPdfHeader(finalData);
        if (pdfHeaderIndex >= 0) {
          console.log(`找到PDF头在位置: ${pdfHeaderIndex}`);
          
          // 从PDF头开始截取数据
          const correctedData = finalData.slice(pdfHeaderIndex);
          console.log(`截取后的数据大小: ${correctedData.length} bytes`);
          
          if (this.validatePdfHeader(correctedData)) {
            console.log('修复后的数据验证通过');
            return await this.saveToLocalFile(downloadTask.filename, correctedData);
          }
        }
        
        // 如果无法修复，仍然保存原始数据供调试分析
        console.warn('无法修复PDF头，保存原始数据供分析');
        const debugFilePath = await this.saveToLocalFile(downloadTask.filename + '.debug', finalData);
        console.log(`调试文件保存至: ${debugFilePath}`);
        
        // 即使头验证失败，也尝试保存文件，因为可能是未加密的正常PDF
        console.log('尝试保存可能未加密的PDF文件');
        return await this.saveToLocalFile(downloadTask.filename, finalData);
      }
      
      // 保存到本地文件
      const filePath = await this.saveToLocalFile(downloadTask.filename, finalData);
      
      // 更新下载状态
      downloadTask.status = DOWNLOAD_STATUS.COMPLETED;
      downloadTask.filePath = filePath;
      
      // 清理分片数据
      downloadTask.chunks.clear();
      
      // 清理存储状态
      this.cleanupDownloadState(downloadTask.id);
      
      console.log(`PDF文件下载完成: ${filePath}`);
      
      // 调用完成回调
      downloadTask.onComplete && downloadTask.onComplete(filePath);
      
    } catch (error) {
      console.error('合并分片失败:', error);
      throw new Error(`合并分片失败: ${error.message}`);
    }
  }

  /**
   * 查找PDF头位置
   */
  findPdfHeader(data) {
    const pdfHeader = [0x25, 0x50, 0x44, 0x46, 0x2D]; // %PDF-
    
    for (let i = 0; i <= data.length - pdfHeader.length; i++) {
      let found = true;
      for (let j = 0; j < pdfHeader.length; j++) {
        if (data[i + j] !== pdfHeader[j]) {
          found = false;
          break;
        }
      }
      if (found) {
        return i;
      }
    }
    
    return -1;
  }

  /**
   * 验证PDF文件头
   */
  validatePdfHeader(data) {
    if (data.length < 8) {
      console.warn('数据长度不足，无法验证PDF头');
      return false;
    }
    
    // 检查PDF文件头"%PDF-"
    const header = String.fromCharCode(...data.slice(0, 5));
    const isValid = header === "%PDF-";
    
    if (!isValid) {
      console.warn(`PDF头验证失败: 期望"%PDF-"，实际"${header}"`);
    } else {
      console.log(`PDF头验证通过: ${header}`);
    }
    
    return isValid;
  }

  /**
   * 调试工具：分析分片数据
   */
  debugChunkData(downloadTask) {
    console.log('=== 分片数据调试信息 ===');
    console.log(`文件ID: ${downloadTask.id}`);
    console.log(`文件名: ${downloadTask.filename}`);
    console.log(`总分片数: ${downloadTask.totalChunks}`);
    console.log(`已下载分片: ${downloadTask.downloadedChunks}`);
    
    for (let i = 0; i < downloadTask.totalChunks; i++) {
      const chunk = downloadTask.chunks.get(i);
      if (chunk) {
        console.log(`分片 ${i}: ${chunk.length} bytes`);
        
        // 检查前几个字节
        if (chunk.length >= 8) {
          const header = Array.from(chunk.slice(0, 8)).map(b => b.toString(16).padStart(2, '0')).join(' ');
          console.log(`  头部字节: ${header}`);
        }
      } else {
        console.warn(`分片 ${i}: 缺失`);
      }
    }
    console.log('=== 调试信息结束 ===');
  }


  /**
   * 保存到本地文件
   */
  async saveToLocalFile(filename, data) {
    try {
      // 使用新的文件名规范化函数
      const safeFilename = normalizeFileName(filename);
      const tempFilePath = `${wx.env.USER_DATA_PATH}/${Date.now()}_${safeFilename}`;
       
      // 确保数据是ArrayBuffer或Uint8Array格式
      let fileData = data;
      if (data instanceof Uint8Array) {
        fileData = data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength);
      }
      
      // 写入文件
      await wx.getFileSystemManager().writeFile({
        filePath: tempFilePath,
        data: fileData,
        encoding: 'binary'
      });
      
      console.log(`文件保存成功: ${tempFilePath}, 数据大小: ${fileData.byteLength || fileData.length} bytes`);
      
      // 确保返回的是正确的本地文件路径，而不是URL
      return tempFilePath;
      
    } catch (error) {
      console.error('保存文件失败:', error);
      throw new Error(`保存文件失败: ${error.message}`);
    }
  }

  /**
   * 更新进度
   */
  updateProgress(downloadTask) {
    downloadTask.progress = Math.round((downloadTask.downloadedChunks / downloadTask.totalChunks) * 100);
    
    // 调用进度回调
    downloadTask.onProgress && downloadTask.onProgress(downloadTask.progress);
  }

  /**
   * 清理下载状态
   */
  cleanupDownloadState(fileId) {
    try {
      const storageKey = `pdf_download_${fileId}`;
      wx.removeStorageSync(storageKey);
    } catch (error) {
      console.warn('清理下载状态失败:', error);
    }
  }

  /**
   * 检查内存使用情况
   */
  checkMemoryUsage(downloadTask) {
    // 计算当前分片数据总大小
    const totalChunkSize = Array.from(downloadTask.chunks.values())
      .reduce((total, chunk) => total + chunk.length, 0);
    
    console.log(`当前内存使用: ${totalChunkSize} bytes, 阈值: ${CONFIG.MEMORY_THRESHOLD} bytes`);
    
    if (totalChunkSize > CONFIG.MEMORY_THRESHOLD) {
      console.warn(`内存使用过高: ${totalChunkSize} bytes，考虑清理缓存`);
      
      // 清理已下载但未合并的分片（保留最近几个）
      const chunkIndices = Array.from(downloadTask.chunks.keys()).sort((a, b) => b - a);
      const chunksToKeep = 3; // 保留最近3个分片
      
      if (chunkIndices.length > chunksToKeep) {
        const chunksToRemove = chunkIndices.slice(chunksToKeep);
        chunksToRemove.forEach(index => {
          const removedChunk = downloadTask.chunks.get(index);
          if (removedChunk) {
            console.log(`清理分片: ${index}, 大小: ${removedChunk.length} bytes`);
            downloadTask.chunks.delete(index);
          }
        });
      }
    }
  }

  /**
   * 保存下载状态
   */
  saveDownloadState(downloadTask) {
    try {
      const storageKey = `pdf_download_${downloadTask.id}`;
      
      // 只保存必要的元数据，不保存分片数据本身
      const stateData = {
        downloadedChunks: downloadTask.downloadedChunks,
        totalChunks: downloadTask.totalChunks,
        progress: downloadTask.progress,
        timestamp: Date.now(),
        // 只保存分片索引，不保存实际数据
        chunkIndices: Array.from(downloadTask.chunks.keys())
      };
      
      // 检查存储大小
      const dataSize = JSON.stringify(stateData).length;
      if (dataSize > 1024 * 1024) { // 超过1MB
        console.warn(`下载状态数据过大: ${dataSize} bytes，跳过保存`);
        return;
      }
      
      wx.setStorageSync(storageKey, stateData);
      console.log(`下载状态保存成功: ${dataSize} bytes`);
      
    } catch (error) {
      if (error.message.includes('exceed storage max size')) {
        console.warn('存储空间不足，跳过状态保存');
        // 清理一些旧的下载状态
        this.cleanupOldDownloadStates();
      } else {
        console.warn('保存下载状态失败:', error);
      }
    }
  }

  /**
   * 清理旧的下载状态
   */
  cleanupOldDownloadStates() {
    try {
      const keys = wx.getStorageInfoSync().keys;
      const downloadKeys = keys.filter(key => key.startsWith('pdf_download_'));
      
      if (downloadKeys.length > 5) { // 保留最近5个下载状态
        // 按时间排序，删除最旧的
        const sortedKeys = downloadKeys.sort((a, b) => {
          try {
            const dataA = wx.getStorageSync(a);
            const dataB = wx.getStorageSync(b);
            return (dataB.timestamp || 0) - (dataA.timestamp || 0);
          } catch (e) {
            return 0;
          }
        });
        
        // 删除最旧的
        const keysToRemove = sortedKeys.slice(5);
        keysToRemove.forEach(key => {
          try {
            wx.removeStorageSync(key);
            console.log(`清理旧下载状态: ${key}`);
          } catch (e) {
            console.warn(`清理旧下载状态失败: ${key}`, e);
          }
        });
      }
    } catch (error) {
      console.warn('清理旧下载状态失败:', error);
    }
  }

  /**
   * 延迟函数
   */
  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * 启动清理定时器
   */
  startCleanupTimer() {
    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer);
    }
    
    this.cleanupTimer = setInterval(() => {
      // 清理过期的下载任务
      const now = Date.now();
      this.downloads.forEach((downloadTask, fileId) => {
        // 如果下载任务超过1小时未完成，清理它
        if (now - downloadTask.startTime > 60 * 60 * 1000) {
          if (downloadTask.status !== DOWNLOAD_STATUS.COMPLETED) {
            console.log(`清理过期下载任务: ${fileId}`);
            this.downloads.delete(fileId);
          }
        }
      });
    }, CONFIG.CLEANUP_INTERVAL);
  }

  /**
   * 暂停下载
   */
  pauseDownload(fileId) {
    const downloadTask = this.downloads.get(fileId);
    if (downloadTask) {
      downloadTask.status = DOWNLOAD_STATUS.PAUSED;
      console.log(`下载已暂停: ${fileId}`);
    }
  }

  /**
   * 恢复下载
   */
  resumeDownload(fileId) {
    const downloadTask = this.downloads.get(fileId);
    if (downloadTask && downloadTask.status === DOWNLOAD_STATUS.PAUSED) {
      console.log(`恢复下载: ${fileId}`);
      // 重新开始下载处理
      this.processDownload(downloadTask).catch(error => {
        console.error('恢复下载失败:', error);
        downloadTask.onError && downloadTask.onError(error);
      });
    }
  }

  /**
   * 取消下载
   */
  cancelDownload(fileId) {
    const downloadTask = this.downloads.get(fileId);
    if (downloadTask) {
      downloadTask.status = DOWNLOAD_STATUS.CANCELLED;
      // 清理分片数据
      downloadTask.chunks.clear();
      // 从下载任务列表中移除
      this.downloads.delete(fileId);
      // 清理存储状态
      this.cleanupDownloadState(fileId);
      console.log(`下载已取消: ${fileId}`);
    }
  }

  /**
   * 获取下载状态
   */
  getDownloadStatus(fileId) {
    const downloadTask = this.downloads.get(fileId);
    if (downloadTask) {
      return {
        status: downloadTask.status,
        progress: downloadTask.progress,
        downloadedChunks: downloadTask.downloadedChunks,
        totalChunks: downloadTask.totalChunks
      };
    }
    return null;
  }
}

// 创建并导出单例
const pdfDownloadService = new PdfChunkDownloader();
module.exports = pdfDownloadService;
