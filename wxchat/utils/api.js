// api.js - API服务文件
const BASE_URL = 'https://api.example.com/v1'

/**
 * 通用请求方法
 * @param {string} url - 请求地址
 * @param {object} options - 请求配置
 * @returns {Promise} 请求结果
 */
const request = (url, options = {}) => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${BASE_URL}${url}`,
      ...options,
      success: (res) => {
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
 * 报告相关API
 */
const reportAPI = {
  /**
   * 获取报告列表
   * @param {object} params - 查询参数
   * @returns {Promise} 报告列表
   */
  getReports: (params = {}) => {
    return get('/reports', params)
  },

  /**
   * 搜索报告
   * @param {object} searchParams - 搜索参数
   * @returns {Promise} 搜索结果
   */
  searchReports: (searchParams) => {
    return post('/reports/search', searchParams)
  },

  /**
   * 获取报告详情
   * @param {string} id - 报告ID
   * @returns {Promise} 报告详情
   */
  getReportDetail: (id) => {
    return get(`/reports/${id}`)
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
  // 生产环境API
  reportAPI,
  
  // Mock数据API（开发环境使用）
  mockAPI,
  
  // 通用请求方法
  request,
  get,
  post
}
