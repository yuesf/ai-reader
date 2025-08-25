/**
 * PDF工具函数
 * 用于处理PDF相关操作，避免在Worker中调用不支持的API
 */

/**
 * 规范化文件名，避免重复扩展名
 * @param {string} fileName 原始文件名
 * @return {string} 规范化后的文件名
 */
function normalizeFileName(fileName) {
  if (!fileName) return 'document.pdf';
  
  // 使用正则表达式匹配重复的扩展名，如 .pdf.pdf
  const regex = /\.([a-zA-Z0-9]+)\.\1$/i;
  if (regex.test(fileName)) {
    return fileName.replace(regex, '.$1');
  }
  
  // 确保文件名以.pdf结尾
  if (!fileName.toLowerCase().endsWith('.pdf')) {
    return fileName + '.pdf';
  }
  
  return fileName;
}

/**
 * 在主线程中打开PDF文件，避免在Worker中调用UI API
 * @param {string} filePath 文件路径
 */
function openPdfFile(filePath) {
  // 检查文件是否存在
  wx.getFileSystemManager().access({
    path: filePath,
    success: () => {
      // 在主线程中打开文件
      wx.openDocument({
        filePath: filePath,
        fileType: 'pdf',
        success: () => {
          console.log('PDF文件打开成功');
        },
        fail: (err) => {
          console.error('打开PDF文件失败:', err);
          wx.showToast({
            title: '无法打开文件: ' + err.errMsg,
            icon: 'none'
          });
        }
      });
    },
    fail: () => {
      console.error('文件不存在或访问失败:', filePath);
      wx.showToast({
        title: '文件不存在',
        icon: 'none'
      });
    }
  });
}

/**
 * 替代 wx.reportRealtimeAction 的方法，在主线程中使用 wx.reportAnalytics
 * @param {string} action 操作名称
 * @param {Object} data 数据
 */
function reportPdfAction(action, data) {
  // 使用 wx.reportAnalytics 替代 wx.reportRealtimeAction
  wx.reportAnalytics(action, data || {});
}

module.exports = {
  normalizeFileName,
  openPdfFile,
  reportPdfAction
};