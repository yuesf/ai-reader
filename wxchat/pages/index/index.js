// index.js
const { reportAPI, BASE_URL } = require('../../utils/api.js')

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
    // 页面加载时获取报告列表
    this.loadReports()
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
    const report = this.data.reports.find(item => item.id === reportId)
    
    if (report) {
      // wx.showModal({
      //   title: '报告详情',
      //   content: `${report.title}\n\n来源：${report.source}\n分类：${report.category}\n页数：${report.pages}页\n发布时间：${report.publishDate}\n\n${report.summary}`,
      //   showCancel: false,
      //   confirmText: '确定'
      // })

    
    }
      // 这里可以添加跳转到报告详情页的逻辑
      wx.navigateTo({
       url: `../reportDetail/reportDetail?id=${reportId}`
     })
    
  },

  /**
   * 下拉刷新
   */
  async onPullDownRefresh() {
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
      // 模拟分页加载
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
