import axios from 'axios';
import router from '../router';

// 定义BASE_URL
// const BASE_URL = 'http://127.0.0.1:8080';
const BASE_URL = 'https://yuesf.cn/reader';

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
  pages?: string;
  fileSize?: string;
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

// 配置 axios 默认设置以支持大文件上传
axios.defaults.timeout = 300000; // 5分钟超时
axios.defaults.maxContentLength = 500 * 1024 * 1024; // 500MB
axios.defaults.maxBodyLength = 500 * 1024 * 1024; // 500MB

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
  
  // 对于文件上传请求，设置更长的超时时间
  if (config.url?.includes('/upload/')) {
    config.timeout = 300000; // 5分钟
    config.maxContentLength = 500 * 1024 * 1024; // 500MB
    config.maxBodyLength = 500 * 1024 * 1024; // 500MB
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
  return axios.post<ApiResponse<ReportListResponse>>(BASE_URL+'/v1/reports', body).then(response => {
    // 处理缩略图URL，添加BASE_URL前缀
    if (response.data.data?.list) {
      response.data.data.list = response.data.data.list.map(item => {
        if (item.thumbnail && (!item.thumbnail.startsWith('http') || !item.thumbnail.startsWith('https'))) {
          item.thumbnail = BASE_URL + item.thumbnail;
        }
        return item;
      });
    }
    return response;
  });
}

export function createReport(body: Partial<ReportItem>) {
  return axios.post<ApiResponse<ReportItem>>(BASE_URL+'/v1/reports/create', body);
}

export function updateReport(id: string, body: Partial<ReportItem>) {
  return axios.put<ApiResponse<ReportItem>>(BASE_URL+`/v1/reports/${id}`, body);
}

export function deleteReport(id: string) {
  return axios.delete<ApiResponse<number>>(BASE_URL+`/v1/reports/${id}`);
}

export function batchDelete(ids: string[]) {
  return axios.post<ApiResponse<number>>(BASE_URL+'/v1/reports/delete', { ids });
}

export function generateSummary(id: string) {
  return axios.post<ApiResponse<string>>(BASE_URL+`/v1/reports/${id}/generate-summary`);
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
  return axios.post<ApiResponse<{url: string, filename: string, size: string}>>(BASE_URL+'/v1/upload/report', formData);
}

export function uploadImage(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return axios.post<ApiResponse<{fileId: string, thumbnail: string}>>(BASE_URL+'/v1/upload/image', formData);
}

export function uploadFile(file: File, folder?: string) {
  const formData = new FormData();
  formData.append('file', file);
  if (folder) {
    formData.append('folder', folder);
  }
  return axios.post<ApiResponse<{url: string, filename: string, size: string, folder: string}>>(BASE_URL+'/v1/upload/file', formData);
}

export function deleteFile(fileId: string) {
  return axios.delete<ApiResponse<string>>(BASE_URL+'/v1/upload/file', { params: { fileId } });
}

// 鉴权相关
export function login(username: string, password: string) {
  return axios.post<ApiResponse<{ token: string }>>(BASE_URL+'/v1/auth/login', { username, password });
}

export function logout() {
  return axios.post<ApiResponse<string>>(BASE_URL+'/v1/auth/logout');
}

// 账号管理
export interface UserItem { id?: number; username: string; password?: string; displayName?: string; status?: number }
export interface UserListReq { page?: number; pageSize?: number; keyword?: string; status?: number }
export interface UserListResp { total: number; page: number; pageSize: number; list: UserItem[] }
export function fetchUsers(body: UserListReq) {
  return axios.post<ApiResponse<UserListResp>>(BASE_URL+'/v1/users/list', body);
}
export function createUser(body: UserItem) {
  return axios.post<ApiResponse<number>>(BASE_URL+'/v1/users/create', body);
}
export function updateUser(body: UserItem) {
  return axios.post<ApiResponse<number>>(BASE_URL+'/v1/users/update', body);
}
export function deleteUserApi(id: number) {
  return axios.delete<ApiResponse<number>>(BASE_URL+`/v1/users/${id}`);
}


