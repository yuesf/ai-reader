import axios from 'axios';

// 埋点数据接口定义（JavaScript版本）
export const trackingAPI = {
  // 获取实时监控面板数据
  getDashboard() {
    return axios.get('/v1/admin/tracking/dashboard');
  },

  // 获取用户行为轨迹
  getUserPath(userId, startDate, endDate, limit = 100) {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    params.append('limit', limit.toString());
    
    return axios.get(`/v1/admin/tracking/users/${userId}/path?${params.toString()}`);
  },

  // 获取页面热力图数据
  getHeatmapData(pagePath, startDate, endDate) {
    const params = new URLSearchParams();
    if (pagePath) params.append('pagePath', pagePath);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    return axios.get(`/v1/admin/tracking/heatmap?${params.toString()}`);
  },

  // 获取埋点统计数据
  getStatistics(groupBy = 'date', startDate, endDate) {
    const params = new URLSearchParams();
    params.append('groupBy', groupBy);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    return axios.get(`/v1/admin/tracking/statistics?${params.toString()}`);
  },

  // 获取活跃用户数量
  getActiveUserCount(minutes = 30) {
    return axios.get(`/v1/admin/tracking/users/active?minutes=${minutes}`);
  },

  // 获取页面访问统计
  getPageViews(startDate, endDate) {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    return axios.get(`/v1/admin/tracking/pages/views?${params.toString()}`);
  },

  // 获取事件类型统计
  getEventTypes(startDate, endDate) {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    return axios.get(`/v1/admin/tracking/events/types?${params.toString()}`);
  },

  // 清理过期数据
  cleanupExpiredData(expireDays = 90) {
    return axios.post(`/v1/admin/tracking/cleanup?expireDays=${expireDays}`);
  }
};
