// api.js - API服务文件
// 将基础URL指向后端服务（根据实际部署环境修改为域名/内网地址）
const BASE_URL = 'https://yuesf.cn/reader'  // 本地测试用，生产环境改为实际域名

/**
 * 通用请求方法
 * @param {string} url - 请求地址
 * @param {object} options - 请求配置
 * @returns {Promise} 请求结果
 */
const request = (url, options = {}) => {
  console.log(`API: request to ${url}`, options);
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}${url}`,
      ...options,
      success: (res) => {
        console.log(`API: request success for ${url}`, res);
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data)
          } else {
            reject(new Error(res.data.message || '请求失败'))
          }
        } else {
          reject(new Error(`HTTP错误: ${res.statusCode}`))
        }
      },
      fail: (err) => {
        console.error(`API: request failed for ${url}`, err);
        reject(new Error(err.errMsg || '网络请求失败'))
      }
    })
  })
}

/**
 * GET请求
 * @param {string} url - 请求地址
 * @param {object} params - 查询参数
 * @returns {Promise} 请求结果
 */
const get = (url, params = {}) => {
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  
  const fullUrl = queryString ? `${url}?${queryString}` : url
  
  return request(fullUrl, {
    method: 'GET'
  })
}

/**
 * POST请求
 * @param {string} url - 请求地址
 * @param {object} data - 请求数据
 * @returns {Promise} 请求结果
 */
const post = (url, data = {}) => {
  return request(url, {
    method: 'POST',
    data
  })
}

/**
 * 报告相关API（小程序专用接口）
 */
const reportAPI = {
  /**
   * 获取报告列表（小程序专用）
   * @param {object} params - 查询参数
   * @returns {Promise} 报告列表
   */
  getReports: (params = {}) => {
    // 使用小程序专用接口 POST /v1/mini/reports
    console.log('API: reportAPI.getReports called', params);
    return post('/v1/mini/reports', params)
  },

  /**
   * 搜索报告（小程序专用）
   * @param {object} searchParams - 搜索参数
   * @returns {Promise} 搜索结果
   */
  searchReports: (searchParams) => {
    // 使用小程序专用接口 POST /v1/mini/reports
    console.log('API: reportAPI.searchReports called', searchParams);
    return post('/v1/mini/reports', searchParams)
  },

  /**
   * 获取报告详情（小程序专用）
   * @param {string} id - 报告ID
   * @returns {Promise} 报告详情
   */
  getReportDetail: (id) => {
    // 使用小程序专用接口 GET /v1/mini/reports/{id}
    console.log('API: reportAPI.getReportDetail called', id);
    return get(`/v1/mini/reports/${id}`)
  },
  
  /**
   * 获取报告文件URL（小程序专用）
   * @param {string} id - 报告ID
   * @returns {Promise} 报告文件URL
   */
  getReportFileUrl: (id) => {
    // 使用小程序专用接口 GET /v1/mini/reports/{id}/file
    console.log('API: reportAPI.getReportFileUrl called', id);
    return get(`/v1/mini/reports/${id}/file`)
  },

  /**
   * 获取PDF文件信息（用于分片下载）
   * @param {string} fileId - 文件ID
   * @returns {Promise} PDF文件信息
   */
  getPdfFileInfo: (fileId) => {
    // 使用PDF流服务接口 GET /v1/pdf/info/{fileId}
    console.log('API: reportAPI.getPdfFileInfo called', fileId);
    return get(`/v1/pdf/info/${fileId}`)
  },

  /**
   * 获取PDF文件分片（用于分片下载）
   * @param {string} fileId - 文件ID
   * @param {number} chunkIndex - 分片索引
   * @returns {Promise} PDF文件分片数据
   */
  getPdfChunk: (fileId, chunkIndex) => {
    // 使用PDF流服务接口 GET /v1/pdf/chunk/{fileId}/{chunkIndex}
    console.log('API: reportAPI.getPdfChunk called', { fileId, chunkIndex });
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${BASE_URL}/v1/pdf/chunk/${fileId}/${chunkIndex}`,
        method: 'GET',
        responseType: 'arraybuffer',
        success: (res) => {
         
          if (res.statusCode === 200) {
            resolve({
              data: res.data,
              headers: res.header,
              code: res.statusCode
            });
          } else {
            reject(new Error(`分片下载失败: ${res.statusCode}`));
          }
        },
        fail: (err) => {
          reject(new Error(`网络请求失败: ${err.errMsg}`));
        }
      });
    });
  },

  /**
   * 获取PDF服务健康状态（用于测试连接）
   * @returns {Promise} 健康状态信息
   */
  getPdfHealth: () => {
    // 使用PDF流服务接口 GET /v1/pdf/health
    console.log('API: reportAPI.getPdfHealth called');
    return get('/v1/pdf/health');
  },
  
  /**
   * 获取PDF信息（用于图片预览）
   * @param {string} fileId - 文件ID
   * @returns {Promise} PDF信息
   */
  getPdfInfo: (fileId) => {
    console.log('API: reportAPI.getPdfInfo called', fileId);
    return get(`/v1/pdf/info/${fileId}`);
  }
}

/**
 * Mock数据服务（开发环境使用）
 */
const mockAPI = {
  /**
   * 获取Mock报告列表
   * @returns {Promise} Mock数据
   */
  getReports: () => {
    return new Promise((resolve) => {
      // 模拟网络延迟
      setTimeout(() => {
        const mockData = require('../mock/index-data.json')
        resolve(mockData)
      }, 500)
    })
  },

  /**
   * 搜索Mock报告
   * @param {string} keyword - 搜索关键词
   * @returns {Promise} 搜索结果
   */
  searchReports: (keyword) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const mockData = require('../mock/index-data.json')
        
        if (!keyword || keyword.trim() === '') {
          resolve(mockData)
          return
        }

        // 过滤数据
        const filteredList = mockData.data.list.filter(report => 
          report.title.toLowerCase().includes(keyword.toLowerCase()) ||
          report.source.toLowerCase().includes(keyword.toLowerCase()) ||
          report.category.toLowerCase().includes(keyword.toLowerCase()) ||
          report.tags.some(tag => tag.toLowerCase().includes(keyword.toLowerCase()))
        )

        const result = {
          ...mockData,
          data: {
            ...mockData.data,
            list: filteredList,
            total: filteredList.length
          }
        }

        resolve(result)
      }, 300)
    })
  }
}

// 导出API服务
module.exports = {
  // 生产环境API（小程序专用）
  reportAPI,
  
  // Mock数据API（开发环境使用）
  mockAPI,
  
  // 通用请求方法
  request,
  get,
  post,
  BASE_URL
}