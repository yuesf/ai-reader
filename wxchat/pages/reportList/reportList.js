// reportList.js
const { reportAPI } = require('../../utils/api.js')
const { getBaseUrl } = require('../../utils/config.js')
const tracking = require('../../utils/tracking/index.js')

// 使用统一配置的 BASE_URL
const BASE_URL = getBaseUrl()

Page({
  data: {
    searchKeyword: '',
    reports: [],
    filteredReports: [],
    loading: false,
    total: 0,
    page: 1,
    pageSize: 20,
    hasMore: true
  },

  onLoad() {
    // 页面浏览埋点
    tracking.trackPageView('/pages/reportList/reportList', '报告列表页')
    
    // 检查登录状态
    this.checkLoginStatus()
    // 页面加载时获取报告列表
    this.loadReports()
  },

  checkLoginStatus() {
    const app = getApp()
    if (!app.globalData.isLoggedIn) {
      wx.reLaunch({
        url: '/pages/login/login'
      })
      return false
    }
    return true
  },

  /**
   * 加载报告列表
   */
  async loadReports() {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      const result = await reportAPI.getReports({
        page: this.data.page,
        pageSize: this.data.pageSize,
        keyword: this.data.searchKeyword
      })
      
      if (result.code === 200) {
        const list = (result.data.list || []).map(item => ({
          ...item,
          thumbnailFull: item.thumbnail ? `${BASE_URL}${item.thumbnail}` : ''
        }))
        this.setData({
          reports: list,
          filteredReports: list,
          total: result.data.total,
          page: result.data.page,
          pageSize: result.data.pageSize,
          hasMore: result.data.list.length < result.data.total
        })
      } else {
        wx.showToast({
          title: result.message || '加载失败',
          icon: 'none'
        })
      }
    } catch (error) {
      console.error('加载报告失败:', error)
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 搜索功能
   */
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({
      searchKeyword: keyword
    })
    
    // 实时搜索
    if (keyword.trim() === '') {
      this.setData({
        filteredReports: this.data.reports
      })
    } else {
      this.performSearch(keyword)
    }
  },

  /**
   * 执行搜索
   */
  async performSearch(keyword) {
    if (!keyword || keyword.trim() === '') {
      this.setData({
        filteredReports: this.data.reports
      })
      return
    }

    // 搜索埋点 - 异步执行
    setTimeout(() => {
      try {
        tracking.trackCustomEvent('search', {
          keyword: keyword,
          pagePath: '/pages/reportList/reportList'
        })
        console.log('[报告列表页] 搜索埋点已发送:', keyword)
      } catch (error) {
        console.error('[报告列表页] 搜索埋点失败:', error)
      }
    }, 0)

    this.setData({ loading: true })
    
    try {
      const result = await reportAPI.searchReports({
        page: 1,
        pageSize: this.data.pageSize,
        keyword
      })
      
      if (result.code === 200) {
        const list = (result.data.list || []).map(item => ({
          ...item,
          thumbnailFull: item.thumbnail ? `${BASE_URL}${item.thumbnail}` : ''
        }))
        this.setData({
          filteredReports: list,
          total: result.data.total
        })
      } else {
        wx.showToast({
          title: result.message || '搜索失败',
          icon: 'none'
        })
      }
    } catch (error) {
      console.error('搜索失败:', error)
      wx.showToast({
        title: '搜索失败，请重试',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 点击搜索按钮
   */
  onSearch() {
    this.performSearch(this.data.searchKeyword)
  },

  /**
   * 点击报告项
   */
  onReportTap(e) {
    const reportId = e.currentTarget.dataset.id
    const report = this.data.filteredReports.find(item => item.id === reportId) || 
                   this.data.reports.find(item => item.id === reportId)
    
    if (report) {
      // 异步执行埋点，不影响页面跳转
      setTimeout(() => {
        try {
          tracking.trackButtonClick('report_card', '报告卡片点击', {
            reportId: reportId,
            reportTitle: report.title,
            reportCategory: report.category,
            reportSource: report.source,
            reportPrice: report.price,
            reportIsFree: report.isFree,
            clickPosition: 'report_list_page'
          })
          console.log('[报告列表页] 报告点击埋点已发送:', reportId, report.title)
        } catch (error) {
          console.error('[报告列表页] 报告点击埋点失败:', error)
        }
      }, 0)

      // 立即跳转到报告详情页
      wx.navigateTo({
        url: `../reportDetail/reportDetail?id=${reportId}`,
        success: () => {
          console.log('[报告列表页] 成功跳转到报告详情页:', reportId)
        },
        fail: (error) => {
          console.error('[报告列表页] 跳转报告详情页失败:', error)
          wx.showToast({
            title: '跳转失败，请重试',
            icon: 'none'
          })
        }
      })
    } else {
      console.error('[报告列表页] 未找到报告信息:', reportId)
      wx.showToast({
        title: '报告信息不存在',
        icon: 'none'
      })
    }
  },

  /**
   * 下拉刷新
   */
  async onPullDownRefresh() {
    // 下拉刷新埋点 - 异步执行
    setTimeout(() => {
      try {
        tracking.trackCustomEvent('pull_refresh', {
          pagePath: '/pages/reportList/reportList'
        })
        console.log('[报告列表页] 下拉刷新埋点已发送')
      } catch (error) {
        console.error('[报告列表页] 下拉刷新埋点失败:', error)
      }
    }, 0)
    
    await this.loadReports()
    wx.stopPullDownRefresh()
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMoreReports()
    }
  },

  /**
   * 加载更多报告
   */
  async loadMoreReports() {
    if (this.data.loading || !this.data.hasMore) return
    
    this.setData({ loading: true })
    
    try {
      const nextPage = this.data.page + 1
      const result = await reportAPI.getReports({
        page: nextPage,
        pageSize: this.data.pageSize,
        keyword: this.data.searchKeyword
      })
      
      if (result.code === 200 && result.data.list.length > 0) {
        const list = (result.data.list || []).map(item => ({
          ...item,
          thumbnailFull: item.thumbnail ? `${BASE_URL}${item.thumbnail}` : ''
        }))
        const newReports = [...this.data.reports, ...list]
        this.setData({
          reports: newReports,
          filteredReports: newReports,
          page: nextPage,
          hasMore: newReports.length < result.data.total
        })

        // 加载更多埋点 - 异步执行
        setTimeout(() => {
          try {
            tracking.trackCustomEvent('load_more', {
              pagePath: '/pages/reportList/reportList',
              page: nextPage,
              totalLoaded: newReports.length
            })
            console.log('[报告列表页] 加载更多埋点已发送:', nextPage)
          } catch (error) {
            console.error('[报告列表页] 加载更多埋点失败:', error)
          }
        }, 0)
      } else {
        this.setData({ hasMore: false })
      }
    } catch (error) {
      console.error('加载更多失败:', error)
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  }
})