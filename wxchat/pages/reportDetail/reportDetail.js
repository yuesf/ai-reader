const { reportAPI, BASE_URL } = require('../../utils/api.js')


Page({
  data: {
    activeTab: 'description', // 修改默认选中标签为简介
    reportTitle: '',
    reportSubtitle: '',
    reportDate: '',
    downloadPrice: '¥1.98',
    reportId: '',
    reportSummary: '',
    reportIsFree: false
  },
  
  onLoad(options) {
    // 从页面参数获取报告ID
    const reportId = options.id;
    this.setData({ reportId });
    
    // 使用 reportAPI 获取报告详情
    this.getReportDetail(reportId);
  },
  
  getReportDetail(reportId) {
    // 使用 reportAPI 封装的方法调用接口
    reportAPI.getReportDetail(reportId)
      .then(res => {
        console.log('报告详情接口返回数据:', res); // 添加打印日志
        if (res.code === 200) {
          const report = res.data;
          this.setData({
            reportTitle: report.title,
            reportSubtitle: `${report.source} | ${report.category} | 共${report.pages}页`,
            reportDate: report.createTime,
            reportSummary: report.summary,
            downloadPrice: report.price,
            reportIsFree: report.isFree
          });
        }
      })
      .catch(err => {
        console.error('获取报告详情失败:', err);
      });
  },
  
  handleTabChange(e) {
    this.setData({ activeTab: e.detail });
  },
  
  downloadDocument() {
    // 下载文档逻辑
    wx.showModal({
      title: '提示',
      content: '请确认是否购买并下载文档',
      success: (res) => {
        if (res.confirm) {
          // 实现支付和下载逻辑
          console.log('开始下载文档');
        }
      }
    });
  }
});