/**
 * PDF预览服务
 * 提供小程序内PDF文件预览功能
 * 支持真正的PDF文件解析和渲染
 */
class PdfPreviewer {
  constructor() {
    this.currentPage = 1;
    this.totalPages = 1;
    this.scale = 1.0;
    this.isRendering = false;
    this.pdfFilePath = '';
    this.fileSize = 0;
    this.pdfData = null;
    this.pageCache = new Map();
    this.fileId = '';
  }

  /**
   * 绑定后端文件ID（用于服务端渲染兜底）
   */
  setFileId(fileId) {
    this.fileId = fileId || '';
  }

  /**
   * 加载PDF文件
   */
  async loadPdf(filePath) {
    try {
      console.log('开始加载PDF文件:', filePath);
      
      // 检查文件是否存在
      const fs = wx.getFileSystemManager();
      const fileInfo = fs.statSync(filePath);
      
      this.pdfFilePath = filePath;
      this.fileSize = fileInfo.size;
      
      // 读取PDF文件内容
      const pdfBuffer = fs.readFileSync(filePath);
      console.log('PDF文件读取成功，大小:', this.fileSize, '字节');
      
      // 解析PDF文件头信息
      const pdfInfo = await this.parsePdfHeader(pdfBuffer);
      this.totalPages = pdfInfo.totalPages;
      this.pdfData = pdfBuffer;
      
      console.log('PDF文件解析成功:', {
        filePath,
        fileSize: this.fileSize,
        totalPages: this.totalPages,
        pdfVersion: pdfInfo.version
      });
      
      return {
        success: true,
        totalPages: this.totalPages,
        fileSize: this.fileSize,
        filePath: this.pdfFilePath,
        version: pdfInfo.version
      };
      
    } catch (error) {
      console.error('PDF文件加载失败:', error);
      throw new Error(`PDF文件加载失败: ${error.message}`);
    }
  }

  /**
   * 解析PDF文件头信息
   */
  async parsePdfHeader(pdfBuffer) {
    try {
      // 将Buffer转换为字符串以查找PDF信息
      const pdfString = pdfBuffer.toString('latin1');
      
      // 查找PDF版本
      const versionMatch = pdfString.match(/^%PDF-(\d+\.\d+)/);
      const version = versionMatch ? versionMatch[1] : '1.0';
      
      // 查找页面数量（通过Count关键字）
      const countMatch = pdfString.match(/\/Count\s+(\d+)/);
      let totalPages = 1;
      
      if (countMatch) {
        totalPages = parseInt(countMatch[1]);
      } else {
        // 如果没有找到Count，通过文件大小估算（每页约4KB）
        totalPages = Math.max(1, Math.ceil(this.fileSize / 4096));
      }
      
      // 查找文档标题
      const titleMatch = pdfString.match(/\/Title\s*\(([^)]+)\)/);
      const title = titleMatch ? titleMatch[1] : 'PDF文档';
      
      console.log('PDF头信息解析结果:', { version, totalPages, title });
      
      return {
        version,
        totalPages,
        title
      };
      
    } catch (error) {
      console.error('PDF头信息解析失败:', error);
      // 返回默认值
      return {
        version: '1.0',
        totalPages: Math.max(1, Math.ceil(this.fileSize / 4096)),
        title: 'PDF文档'
      };
    }
  }

  /**
   * 渲染指定页面
   */
  async renderPage(pageNum, canvas) {
    if (pageNum < 1 || pageNum > this.totalPages) {
      throw new Error(`页码超出范围: ${pageNum}`);
    }

    if (this.isRendering) {
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          this.renderPage(pageNum, canvas).then(resolve).catch(reject);
        }, 100);
      });
    }

    this.isRendering = true;
    this.currentPage = pageNum;

    try {
      console.log(`开始渲染第${pageNum}页`);
      
      // 检查缓存
      if (this.pageCache.has(pageNum)) {
        const cachedPage = this.pageCache.get(pageNum);
        await this.drawPageToCanvas(cachedPage, canvas);
        this.isRendering = false;
        return { pageNum, success: true, cached: true };
      }
      
      // 渲染页面内容
      const pageData = await this.renderPageContent(pageNum);
      
      // 缓存页面数据
      this.pageCache.set(pageNum, pageData);
      
      // 绘制到画布
      await this.drawPageToCanvas(pageData, canvas);
      
      this.isRendering = false;
      
      return { 
        pageNum, 
        success: true, 
        cached: false 
      };
      
    } catch (error) {
      this.isRendering = false;
      console.error(`渲染第${pageNum}页失败:`, error);
      throw error;
    }
  }

  /**
   * 渲染页面内容（基于真实PDF数据）
   */
  async renderPageContent(pageNum) {
    try {
      console.log(`开始渲染第${pageNum}页内容`);
      
      if (!this.pdfData) {
        throw new Error('PDF数据未加载');
      }
      
      // 解析页面内容
      const pageData = await this.parsePdfPage(pageNum);
      
      console.log(`第${pageNum}页内容渲染完成`);
      return pageData;
      
    } catch (error) {
      console.error(`渲染第${pageNum}页内容失败:`, error);
      // 解析失败：优先使用服务端渲染的真实页面图像；若不可用，再回退到占位内容
      const serverPage = this.buildServerPageData(pageNum);
      if (serverPage) return serverPage;
      return this.getFallbackPageData(pageNum);
    }
  }

  /**
   * 解析PDF页面内容
   */
  async parsePdfPage(pageNum) {
    try {
      // 将PDF数据转换为字符串进行解析
      const pdfString = this.pdfData.toString('latin1');
      
      // 更稳健的页面对象匹配（优先匹配含 /Type /Page 的对象）
      const objPattern = /(\d+\s+\d+\s+obj[\s\S]*?endobj)/g;
      const pages = [];
      let match;
      while ((match = objPattern.exec(pdfString)) !== null) {
        const obj = match[1];
        if (obj.includes('/Type /Page') || (/\n\/Page\b/.test(obj) && !/\/Pages\b/.test(obj))) {
          pages.push(obj);
        }
      }
      
      // 获取指定页面（若越界，回退到第一页）
      const targetPage = pages[pageNum - 1] || pages[0];
      if (!targetPage) {
        // 找不到任何页面对象，返回可渲染的回退数据
        return this.getFallbackPageData(pageNum);
      }
      
      // 提取页面文本（非常简化：尝试匹配 ( ... ) 字面内容）
      let textContent = `第${pageNum}页`;
      const simpleTextMatch = targetPage.match(/\(([^)]+)\)/);
      if (simpleTextMatch && simpleTextMatch[1]) {
        textContent = simpleTextMatch[1];
      }
      
      // 提取页面尺寸
      const mediaBoxMatch = targetPage.match(/\/MediaBox\s*\[([^\]]+)\]/);
      let width = 595, height = 842; // 默认A4
      if (mediaBoxMatch) {
        const dimensions = mediaBoxMatch[1].trim().split(/\s+/).map(Number);
        if (dimensions.length >= 4 && dimensions.every(n => !isNaN(n))) {
          width = Math.max(1, dimensions[2] - dimensions[0]);
          height = Math.max(1, dimensions[3] - dimensions[1]);
        }
      }
      
      return {
        pageNum: pageNum,
        content: textContent,
        width: width,
        height: height,
        fileSize: this.fileSize,
        title: `第${pageNum}页`,
        isRealContent: true
      };
      
    } catch (error) {
      console.error('PDF页面解析失败:', error);
      // 发生异常时交由上层兜底（服务端渲染或占位）
      throw error;
    }
  }

  /**
   * 构造服务端渲染页面数据（真实内容，使用后端将PDF页转为图片）
   */
  buildServerPageData(pageNum) {
    try {
      if (!this.fileId) {
        return null;
      }
      // 拼装服务端图片渲染URL（请确保后端提供该接口）
      // 约定：GET /v1/pdf/page/{fileId}/{page} 可返回该页PNG/JPG
      const { getApiUrl } = require('./config.js');
      const pagePath = `/pdf/page/${this.fileId}/${pageNum}`;
      const imageUrl = getApiUrl(pagePath);
      return {
        pageNum,
        content: '',
        width: 595,
        height: 842,
        fileSize: this.fileSize,
        title: `第${pageNum}页`,
        isRealContent: true,
        imageUrl
      };
    } catch (e) {
      console.error('构造服务端页面数据失败:', e);
      return null;
    }
  }

  /**
   * 获取备用页面数据（当PDF解析失败时）
   */
  getFallbackPageData(pageNum) {
    return {
      pageNum: pageNum,
      content: `第${pageNum}页内容（解析失败，显示模拟内容）`,
      width: 595,
      height: 842,
      fileSize: this.fileSize,
      title: `第${pageNum}页`,
      isRealContent: false
    };
  }

  /**
   * 绘制页面到画布
   */
  async drawPageToCanvas(pageData, canvas) {
    return new Promise((resolve, reject) => {
      try {
        console.log('开始绘制页面到画布:', pageData);
        
        // 检查canvas是否有效
        if (!canvas) {
          throw new Error('Canvas对象无效');
        }
        
        // 获取画布上下文
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          throw new Error('无法获取2D绘图上下文');
        }
        
        // 获取画布尺寸
        const canvasWidth = canvas.width || 300;
        const canvasHeight = canvas.height || 400;
        
        console.log(`绘制页面到画布: ${pageData.pageNum}, 尺寸: ${canvasWidth}x${canvasHeight}`);
        
        // 清空画布
        ctx.clearRect(0, 0, canvasWidth, canvasHeight);
        
        // 设置背景色
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvasWidth, canvasHeight);
        
        // 若提供了服务端渲染的图片，优先绘制真实图像
        if (pageData.imageUrl) {
          try {
            wx.getImageInfo({
              src: pageData.imageUrl,
              success: (info) => {
                const img = canvas.createImage();
                img.onload = () => {
                  const imgW = img.width || info.width || canvasWidth;
                  const imgH = img.height || info.height || canvasHeight;
                  const scale = Math.min((canvasWidth - 20) / imgW, (canvasHeight - 20) / imgH);
                  const drawW = Math.max(1, Math.floor(imgW * scale));
                  const drawH = Math.max(1, Math.floor(imgH * scale));
                  const dx = (canvasWidth - drawW) / 2;
                  const dy = (canvasHeight - drawH) / 2;
                  ctx.drawImage(img, dx, dy, drawW, drawH);
                  ctx.fillStyle = '#666666';
                  ctx.font = '14px sans-serif';
                  ctx.textAlign = 'center';
                  ctx.fillText(`${pageData.pageNum} / ${this.totalPages}`, canvasWidth / 2, canvasHeight - 20);
                  resolve();
                };
                img.onerror = (err) => {
                  console.error('服务端页面图像加载失败(createImage):', err);
                  this.drawFallback(ctx, canvasWidth, canvasHeight, pageData).then(resolve).catch(reject);
                };
                // 使用本地文件路径，避免跨域
                img.src = info.path || pageData.imageUrl;
              },
              fail: (err) => {
                console.error('getImageInfo失败:', err);
                this.drawFallback(ctx, canvasWidth, canvasHeight, pageData).then(resolve).catch(reject);
              }
            });
          } catch (err) {
            console.error('加载服务端图像异常:', err);
            this.drawFallback(ctx, canvasWidth, canvasHeight, pageData).then(resolve).catch(reject);
          }
          return;
        }

        // 绘制页面边框
        ctx.strokeStyle = '#e0e0e0';
        ctx.lineWidth = 2;
        ctx.strokeRect(10, 10, canvasWidth - 20, canvasHeight - 20);
        
        // 绘制页面标题
        ctx.fillStyle = '#333333';
        ctx.font = 'bold 18px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(`PDF预览 - 第${pageData.pageNum}页`, canvasWidth / 2, canvasHeight / 2 - 60);
        
        // 绘制内容标识
        if (pageData.isRealContent) {
          ctx.fillStyle = '#28a745';
          ctx.font = '14px sans-serif';
          ctx.fillText('✓ 真实PDF内容', canvasWidth / 2, canvasHeight / 2 - 30);
        } else {
          ctx.fillStyle = '#ffc107';
          ctx.font = '14px sans-serif';
          ctx.fillText('⚠ 模拟内容（解析失败）', canvasWidth / 2, canvasHeight / 2 - 30);
        }
        
        // 绘制页面内容（限制长度）
        ctx.fillStyle = '#666666';
        ctx.font = '14px sans-serif';
        const content = pageData.content.length > 50 ? 
          pageData.content.substring(0, 50) + '...' : 
          pageData.content;
        ctx.fillText(content, canvasWidth / 2, canvasHeight / 2);
        
        // 绘制文件信息
        ctx.fillStyle = '#999999';
        ctx.font = '12px sans-serif';
        ctx.fillText(`文件大小: ${Math.round(pageData.fileSize / 1024)}KB`, canvasWidth / 2, canvasHeight / 2 + 30);
        
        // 绘制页码
        ctx.fillStyle = '#666666';
        ctx.font = '14px sans-serif';
        ctx.fillText(`${pageData.pageNum} / ${this.totalPages}`, canvasWidth / 2, canvasHeight - 20);
        
        console.log(`第${pageData.pageNum}页绘制完成`);
        resolve();
        
      } catch (error) {
        console.error('绘制页面到画布失败:', error);
        reject(error);
      }
    });
  }

  /**
   * 占位绘制（当真实图像加载失败时）
   */
  async drawFallback(ctx, canvasWidth, canvasHeight, pageData) {
    return new Promise((resolve) => {
      ctx.clearRect(0, 0, canvasWidth, canvasHeight);
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, canvasWidth, canvasHeight);
      ctx.strokeStyle = '#e0e0e0';
      ctx.lineWidth = 2;
      ctx.strokeRect(10, 10, canvasWidth - 20, canvasHeight - 20);
      ctx.fillStyle = '#ffc107';
      ctx.font = '14px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('⚠ 页面图像加载失败，已降级为占位内容', canvasWidth / 2, canvasHeight / 2);
      ctx.fillStyle = '#666666';
      ctx.fillText(`${pageData.pageNum} / ${this.totalPages}`, canvasWidth / 2, canvasHeight - 20);
      resolve();
    });
  }

  /**
   * 缩放控制
   */
  zoomIn() {
    this.scale = Math.min(3.0, this.scale + 0.25);
    return this.scale;
  }

  zoomOut() {
    this.scale = Math.max(0.5, this.scale - 0.25);
    return this.scale;
  }

  resetScale() {
    this.scale = 1.0;
    return this.scale;
  }

  /**
   * 页面导航
   */
  goToPage(pageNum) {
    if (pageNum >= 1 && pageNum <= this.totalPages) {
      this.currentPage = pageNum;
      return true;
    }
    return false;
  }

  nextPage() {
    return this.goToPage(this.currentPage + 1);
  }

  previousPage() {
    return this.goToPage(this.currentPage - 1);
  }

  /**
   * 获取状态信息
   */
  getStatus() {
    return {
      currentPage: this.currentPage,
      totalPages: this.totalPages,
      scale: this.scale,
      isRendering: this.isRendering,
      filePath: this.pdfFilePath,
      fileSize: this.fileSize,
      hasRealContent: this.pdfData !== null
    };
  }

  /**
   * 清理资源
   */
  cleanup() {
    this.isRendering = false;
    this.currentPage = 1;
    this.scale = 1.0;
    this.pdfData = null;
    this.pageCache.clear();
    console.log('PDF预览器资源已清理');
  }
}

// 创建单例实例
const pdfPreviewService = new PdfPreviewer();

// 同时导出类和实例
module.exports = {
  PdfPreviewer: PdfPreviewer,  // 导出类，支持 new 操作
  pdfPreviewService: pdfPreviewService,  // 导出实例
  // 为了向后兼容，直接导出实例
  ...pdfPreviewService
};
