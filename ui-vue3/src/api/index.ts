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
  console.log('发送请求:', config.method?.toUpperCase(), config.url);
  console.log('当前token:', token ? token.substring(0, 20) + '...' : 'null');
  
  if (token) {
    config.headers = config.headers || {};
    (config.headers as any)['Authorization'] = `Bearer ${token}`;
    console.log('设置Authorization头:', `Bearer ${token.substring(0, 20)}...`);
  } else {
    console.log('未设置Authorization头');
  }
  
  return config;
});

axios.interceptors.response.use((resp) => resp, (error) => {
  console.error('Axios 响应错误:', error);
  
  if (error?.response?.status === 401) {
    console.log('收到401响应，清除token并跳转登录页');
    localStorage.removeItem('token');
    if (router.currentRoute.value.path !== '/login') {
      router.replace('/login');
    }
  } else if (error.code === 'ERR_NETWORK') {
    console.error('网络错误 - 可能是代理配置问题或后端服务未启动');
  } else if (error.code === 'ECONNREFUSED') {
    console.error('连接被拒绝 - 后端服务可能未启动');
  } else if (error.code === 'ERR_BAD_REQUEST') {
    console.error('请求错误 - 可能是请求格式问题');
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
  console.log('uploadReportFile 被调用，文件信息:', {
    name: file.name,
    size: file.size,
    type: file.type
  });
  
  const formData = new FormData();
  formData.append('file', file);
  
  console.log('FormData 内容:');
  for (let [key, value] of formData.entries()) {
    console.log(key, value);
  }
  
  console.log('发送请求到: /v1/upload/report');
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

export function deleteFile(fileId: string) {
  return axios.delete<ApiResponse<string>>('/v1/upload/file', { params: { fileId } });
}

// 鉴权相关
export function login(username: string, password: string) {
  return axios.post<ApiResponse<{ token: string }>>('/v1/auth/login', { username, password });
}

export function logout() {
  return axios.post<ApiResponse<string>>('/v1/auth/logout');
}

// 账号管理
export interface UserItem { id?: number; username: string; password?: string; displayName?: string; status?: number }
export interface UserListReq { page?: number; pageSize?: number; keyword?: string; status?: number }
export interface UserListResp { total: number; page: number; pageSize: number; list: UserItem[] }
export function fetchUsers(body: UserListReq) {
  return axios.post<ApiResponse<UserListResp>>('/v1/users/list', body);
}
export function createUser(body: UserItem) {
  return axios.post<ApiResponse<number>>('/v1/users/create', body);
}
export function updateUser(body: UserItem) {
  return axios.post<ApiResponse<number>>('/v1/users/update', body);
}
export function deleteUserApi(id: number) {
  return axios.delete<ApiResponse<number>>(`/v1/users/${id}`);
}


