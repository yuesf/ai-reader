/**
 * 缓存测试工具
 * 用于测试和演示图片缓存功能
 */

const imageCache = require('./imageCache');
const pdfImagePreviewService = require('./pdfImagePreviewService');

class CacheTestUtils {
  /**
   * 测试缓存基本功能
   */
  static testBasicCache() {
    console.log('=== 缓存基本功能测试 ===');
    
    const reportId = 'test_report_001';
    const pageIndex = 1;
    const imagePath = '/tmp/test_image.jpg';
    
    // 测试设置缓存
    imageCache.setCache(reportId, pageIndex, imagePath);
    console.log('✓ 缓存设置成功');
    
    // 测试获取缓存
    const cachedPath = imageCache.getCache(reportId, pageIndex);
    console.log('✓ 缓存获取结果:', cachedPath);
    
    // 测试缓存统计
    const stats = imageCache.getCacheStats();
    console.log('✓ 缓存统计:', {
      size: stats.size,
      maxSize: stats.maxSize,
      itemCount: stats.items.length
    });
    
    // 测试清理缓存
    imageCache.clearReportCache(reportId);
    console.log('✓ 缓存清理完成');
    
    const statsAfterClear = imageCache.getCacheStats();
    console.log('✓ 清理后统计:', {
      size: statsAfterClear.size,
      itemCount: statsAfterClear.items.length
    });
  }
  
  /**
   * 测试缓存过期机制
   */
  static testCacheExpiration() {
    console.log('=== 缓存过期机制测试 ===');
    
    const reportId = 'test_report_002';
    const pageIndex = 1;
    const imagePath = '/tmp/test_image_2.jpg';
    
    // 设置缓存
    imageCache.setCache(reportId, pageIndex, imagePath);
    console.log('✓ 缓存已设置');
    
    // 模拟时间过期（修改缓存项的时间戳）
    const cacheKey = imageCache.generateCacheKey(reportId, pageIndex);
    const cacheItem = imageCache.cache.get(cacheKey);
    if (cacheItem) {
      // 将时间戳设置为25小时前（超过24小时过期时间）
      cacheItem.timestamp = Date.now() - (25 * 60 * 60 * 1000);
      console.log('✓ 模拟缓存过期');
      
      // 尝试获取过期缓存
      const expiredCache = imageCache.getCache(reportId, pageIndex);
      console.log('✓ 过期缓存获取结果:', expiredCache); // 应该返回null
    }
  }
  
  /**
   * 测试缓存容量限制
   */
  static testCacheCapacity() {
    console.log('=== 缓存容量限制测试 ===');
    
    const originalMaxSize = imageCache.maxCacheSize;
    imageCache.maxCacheSize = 3; // 临时设置为3个
    
    console.log('✓ 临时设置最大缓存数量为3');
    
    // 添加4个缓存项，测试自动清理
    for (let i = 1; i <= 4; i++) {
      imageCache.setCache(`test_report_${i}`, 1, `/tmp/test_image_${i}.jpg`);
      console.log(`✓ 添加缓存项 ${i}`);
      
      const stats = imageCache.getCacheStats();
      console.log(`  当前缓存数量: ${stats.size}`);
    }
    
    const finalStats = imageCache.getCacheStats();
    console.log('✓ 最终缓存统计:', {
      size: finalStats.size,
      maxSize: imageCache.maxCacheSize,
      items: finalStats.items.map(item => item.key)
    });
    
    // 恢复原始设置
    imageCache.maxCacheSize = originalMaxSize;
    imageCache.clearAllCache();
    console.log('✓ 恢复原始设置并清理测试缓存');
  }
  
  /**
   * 测试PDF预览服务缓存集成
   */
  static testPdfServiceIntegration() {
    console.log('=== PDF预览服务缓存集成测试 ===');
    
    const fileId = 'test_file_001';
    const pageIndex = 1;
    
    // 设置文件ID
    pdfImagePreviewService.setFile(fileId);
    console.log('✓ 设置文件ID:', fileId);
    
    // 模拟缓存一个图片路径
    const mockImagePath = '/tmp/mock_cached_image.jpg';
    pdfImagePreviewService.cacheLocalImagePath(fileId, pageIndex, mockImagePath);
    console.log('✓ 模拟缓存图片路径');
    
    // 测试获取缓存路径
    const cachedPath = pdfImagePreviewService.getCachedLocalImagePath(fileId, pageIndex);
    console.log('✓ 获取缓存路径:', cachedPath);
    
    // 测试清理报告缓存
    pdfImagePreviewService.clearReportCache(fileId);
    console.log('✓ 清理报告缓存');
    
    // 验证缓存已清理
    const clearedPath = pdfImagePreviewService.getCachedLocalImagePath(fileId, pageIndex);
    console.log('✓ 清理后获取路径:', clearedPath); // 应该返回null
  }
  
  /**
   * 运行所有测试
   */
  static runAllTests() {
    console.log('开始运行缓存功能测试...\n');
    
    try {
      this.testBasicCache();
      console.log('');
      
      this.testCacheExpiration();
      console.log('');
      
      this.testCacheCapacity();
      console.log('');
      
      this.testPdfServiceIntegration();
      console.log('');
      
      console.log('✅ 所有测试完成！');
    } catch (error) {
      console.error('❌ 测试过程中出现错误:', error);
    }
  }
  
  /**
   * 获取缓存使用情况报告
   */
  static getCacheReport() {
    const stats = imageCache.getCacheStats();
    const report = {
      summary: {
        totalItems: stats.size,
        maxCapacity: stats.maxSize,
        usagePercentage: Math.round((stats.size / stats.maxSize) * 100)
      },
      items: stats.items.map(item => ({
        reportId: item.reportId,
        pageIndex: item.pageIndex,
        ageInMinutes: Math.round(item.age / (1000 * 60)),
        ageInHours: Math.round(item.age / (1000 * 60 * 60))
      })),
      recommendations: []
    };
    
    // 添加建议
    if (report.summary.usagePercentage > 80) {
      report.recommendations.push('缓存使用率较高，建议清理部分旧缓存');
    }
    
    const oldItems = report.items.filter(item => item.ageInHours > 12);
    if (oldItems.length > 0) {
      report.recommendations.push(`发现 ${oldItems.length} 个超过12小时的缓存项，可考虑清理`);
    }
    
    if (report.summary.totalItems === 0) {
      report.recommendations.push('当前无缓存项，首次使用时会自动创建缓存');
    }
    
    return report;
  }
}

module.exports = CacheTestUtils;