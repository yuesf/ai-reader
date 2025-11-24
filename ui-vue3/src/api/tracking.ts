import axios from 'axios';

// 埋点数据接口定义
export interface TrackingEvent {
  id: number;
  userId: string;
  sessionId: string;
  eventType: string;
  pagePath: string;
  elementId?: string;
  elementText?: string;
  properties?: string; // JSON字符串
  timestamp: number;
  deviceInfo?: string; // JSON字符串
  networkType?: string;
  createdAt: string;
}

export interface UserSession {
  id: number;
  sessionId: string;
  userId: string;
  startTime: number;
  endTime?: number;
  pageCount: number;
  eventCount: number;
  duration: number;
  deviceInfo?: string;
  networkType?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardData {
  todayEvents: number;
  activeUsers: number;
  topPages: Array<{ [key: string]: any }>;
  topEvents: Array<{ [key: string]: any }>;
}

export interface HeatmapData {
  elementId: string;
  clickCount: number;
  elementText?: string;
}

export interface StatisticsData {
  [key: string]: any;
}

// API基础路径
const BASE_URL = ''; // 相对于路径，开发环境会通过代理处理

// 获取实时监控面板数据
export function getDashboard() {
  return axios.get<{ code: number; message: string; data: DashboardData }>(`${BASE_URL}/v1/admin/tracking/dashboard`);
}

// 获取用户行为轨迹
export function getUserPath(userId: string, startDate?: string, endDate?: string, limit: number = 100) {
  const params = new URLSearchParams();
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  params.append('limit', limit.toString());
  
  return axios.get<{ code: number; message: string; data: TrackingEvent[] }>(
    `${BASE_URL}/v1/admin/tracking/users/${userId}/path?${params.toString()}`
  );
}

// 获取页面热力图数据
export function getHeatmapData(pagePath?: string, startDate?: string, endDate?: string) {
  const params = new URLSearchParams();
  if (pagePath) params.append('pagePath', pagePath);
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  
  return axios.get<{ code: number; message: string; data: HeatmapData[] }>(
    `${BASE_URL}/v1/admin/tracking/heatmap?${params.toString()}`
  );
}

// 获取埋点统计数据
export function getStatistics(groupBy: string = 'date', startDate?: string, endDate?: string) {
  const params = new URLSearchParams();
  params.append('groupBy', groupBy);
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  
  return axios.get<{ code: number; message: string; data: StatisticsData[] }>(
    `${BASE_URL}/v1/admin/tracking/statistics?${params.toString()}`
  );
}

// 获取活跃用户数量
export function getActiveUserCount(minutes: number = 30) {
  return axios.get<{ code: number; message: string; data: number }>(
    `${BASE_URL}/v1/admin/tracking/users/active?minutes=${minutes}`
  );
}

// 获取页面访问统计
export function getPageViews(startDate?: string, endDate?: string) {
  const params = new URLSearchParams();
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  
  return axios.get<{ code: number; message: string; data: StatisticsData[] }>(
    `${BASE_URL}/v1/admin/tracking/pages/views?${params.toString()}`
  );
}

// 获取事件类型统计
export function getEventTypes(startDate?: string, endDate?: string) {
  const params = new URLSearchParams();
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);
  
  return axios.get<{ code: number; message: string; data: StatisticsData[] }>(
    `${BASE_URL}/v1/admin/tracking/events/types?${params.toString()}`
  );
}

// 清理过期数据
export function cleanupExpiredData(expireDays: number = 90) {
  return axios.post<{ code: number; message: string; data: { deletedEvents: number; deletedSessions: number } }>(
    `${BASE_URL}/v1/admin/tracking/cleanup?expireDays=${expireDays}`
  );
}
