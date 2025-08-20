import axios from 'axios';
import router from '../router';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface ReportItem {
  id: string;
  title: string;
  summary?: string;
  source?: string;
  category?: string;
  pages?: number;
  fileSize?: number;
  publishDate?: string;
  updateDate?: string;
  thumbnail?: string;
  tags?: string[];
  downloadCount?: number;
  viewCount?: number;
  isFree?: boolean;
  price?: number;
}

export interface ReportListRequest {
  page?: number;
  pageSize?: number;
  keyword?: string;
  category?: string;
  source?: string;
  startDate?: string;
  endDate?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface ReportListResponse {
  total: number;
  page: number;
  pageSize: number;
  list: ReportItem[];
}

// 全局 axios 拦截器：携带 token，处理 401
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers || {};
    (config.headers as any)['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

axios.interceptors.response.use((resp) => resp, (error) => {
  if (error?.response?.status === 401) {
    localStorage.removeItem('token');
    if (router.currentRoute.value.path !== '/login') {
      router.replace('/login');
    }
  }
  return Promise.reject(error);
});

export function fetchReports(body: ReportListRequest) {
  return axios.post<ApiResponse<ReportListResponse>>('/v1/reports', body);
}

export function createReport(body: Partial<ReportItem>) {
  return axios.post<ApiResponse<ReportItem>>('/v1/reports/create', body);
}

export function deleteReport(id: string) {
  return axios.delete<ApiResponse<number>>(`/v1/reports/${id}`);
}

export function batchDelete(ids: string[]) {
  return axios.post<ApiResponse<number>>('/v1/reports/delete', { ids });
}

// 文件上传相关接口
export function uploadReportFile(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return axios.post<ApiResponse<{url: string, filename: string, size: string}>>('/v1/upload/report', formData);
}

export function uploadImage(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return axios.post<ApiResponse<{url: string, filename: string, size: string}>>('/v1/upload/image', formData);
}

export function uploadFile(file: File, folder?: string) {
  const formData = new FormData();
  formData.append('file', file);
  if (folder) {
    formData.append('folder', folder);
  }
  return axios.post<ApiResponse<{url: string, filename: string, size: string, folder: string}>>('/v1/upload/file', formData);
}

export function deleteFile(url: string) {
  return axios.delete<ApiResponse<string>>('/v1/upload/file', { params: { url } });
}

// 鉴权相关
export function login(username: string, password: string) {
  return axios.post<ApiResponse<{ token: string }>>('/v1/auth/login', { username, password });
}


