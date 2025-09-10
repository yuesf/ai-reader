// index.js - 博客首页
const { reportAPI } = require('../../utils/api.js')
const { getBaseUrl } = require('../../utils/config.js')
const tracking = require('../../utils/tracking/index.js')

// 使用统一配置的 BASE_URL
const BASE_URL = getBaseUrl()

Page({
  data: {
    // 搜索相关
    searchKeyword: '',
    showSearch: false,
    
    // 分类相关
    categories: ['技术报告', '行业分析', '市场研究', '投资报告', '政策解读'],
    currentCategory: '',
    
    // 文章数据
    articles: [],
    filteredArticles: [],
    featuredArticles: [],
    
    // 分页相关
    loading: false,
    page: 1,
    pageSize: 10,
    hasMore: true,
    total: 0
  },

  onLoad() {
    // 页面浏览埋点
    tracking.trackPageView('/pages/index/index', '博客首页')
    
    // 移除登录检查，允许游客浏览
    console.log('[首页] 允许游客模式浏览')
    // 加载博客内容
    this.loadBlogContent()
  },

  // 检查登录状态（保留方法但不强制跳转）
  checkLoginStatus() {
    const app = getApp()
    return app.globalData.isLoggedIn
  },


  /**
   * 加载博客内容
   */
  async loadBlogContent() {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    
    try {
      const result = await reportAPI.getReports({
        page: this.data.page,
        pageSize: this.data.pageSize,
        keyword: this.data.searchKeyword,
        category: this.data.currentCategory
      })
      
      if (result.code === 200) {
        const list = (result.data.list || []).map(item => this.transformToBlogArticle(item))
        
        // 分离精选文章和普通文章
        const featured = list.filter(item => item.isFeatured).slice(0, 3)
        const regular = list.filter(item => !item.isFeatured)
        
        this.setData({
          articles: this.data.page === 1 ? regular : [...this.data.articles, ...regular],
          filteredArticles: this.data.page === 1 ? regular : [...this.data.filteredArticles, ...regular],
          featuredArticles: this.data.page === 1 ? featured : this.data.featuredArticles,
          total: result.data.total,
          page: result.data.page,
          hasMore: (this.data.articles.length + regular.length) < result.data.total
        })
      } else {
        wx.showToast({
          title: result.message || '加载失败',
          icon: 'none'
        })
      }
    } catch (error) {
      console.error('加载博客内容失败:', error)
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 将报告数据转换为博客文章格式
   */
  transformToBlogArticle(report) {
    return {
      id: report.id,
      title: report.title,
      summary: this.generateSummary(report.title, report.category),
      category: report.category,
      tags: report.tags || [],
      publishDate: report.publishDate,
      readCount: report.readCount || Math.floor(Math.random() * 1000) + 50,
      coverImage: report.thumbnail ? `${BASE_URL}${report.thumbnail}` : '',
      reportId: report.id,
      isFeatured: report.isFeatured || Math.random() > 0.8, // 随机设置精选
      source: report.source,
      pages: report.pages
    }
  },

  /**
   * 生成文章摘要
   */
  generateSummary(title, category) {
    const summaryTemplates = {
      '技术报告': `本报告深入分析了${title}的技术发展趋势，探讨了关键技术突破和应用场景，为相关从业者提供了宝贵的技术洞察和发展建议。`,
      '行业分析': `通过对${title}的全面调研，本报告揭示了行业发展的核心驱动因素，分析了市场格局变化，为投资决策提供了重要参考。`,
      '市场研究': `本研究报告全面梳理了${title}的市场现状，深入分析了消费者需求变化和竞争格局，为企业战略制定提供了数据支撑。`,
      '投资报告': `基于详实的数据分析，本报告评估了${title}的投资价值和风险因素，为投资者提供了专业的投资建议和策略指导。`,
      '政策解读': `本报告深度解读了${title}相关政策的核心要点，分析了政策影响和实施路径，为相关机构和企业提供了政策应对建议。`
    }
    
    return summaryTemplates[category] || `本报告对${title}进行了深入研究，提供了全面的分析和专业见解，值得相关人士深入了解。`
  },

  /**
   * 分类切换
   */
  onCategoryTap(e) {
    const category = e.currentTarget.dataset.category
    
    // 分类切换埋点
    setTimeout(() => {
      try {
        tracking.trackButtonClick('category_nav', '分类导航点击', {
          category: category,
          pagePath: '/pages/index/index'
        })
        console.log('[博客首页] 分类切换埋点已发送:', category)
      } catch (error) {
        console.error('[博客首页] 分类切换埋点失败:', error)
      }
    }, 0)
    
    this.setData({
      currentCategory: category,
      page: 1,
      articles: [],
      filteredArticles: [],
      featuredArticles: []
    })
    
    this.loadBlogContent()
  },

  /**
   * 显示/隐藏搜索框
   */
  onSearchTap() {
    this.setData({
      showSearch: !this.data.showSearch
    })
    
    if (this.data.showSearch) {
      // 搜索框展开埋点
      setTimeout(() => {
        try {
          tracking.trackButtonClick('search_toggle', '搜索框展开')
          console.log('[博客首页] 搜索框展开埋点已发送')
        } catch (error) {
          console.error('[博客首页] 搜索框展开埋点失败:', error)
        }
      }, 0)
    }
  },

  /**
   * 关闭搜索框
   */
  onSearchClose() {
    this.setData({
      showSearch: false,
      searchKeyword: '',
      filteredArticles: this.data.articles
    })
  },

  /**
   * 搜索输入
   */
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({
      searchKeyword: keyword
    })
    
    // 实时搜索
    this.performSearch(keyword)
  },

  /**
   * 执行搜索
   */
  async performSearch(keyword) {
    if (!keyword || keyword.trim() === '') {
      this.setData({
        filteredArticles: this.data.articles
      })
      return
    }

    // 搜索埋点 - 异步执行
    setTimeout(() => {
      try {
        tracking.trackCustomEvent('blog_search', {
          keyword: keyword,
          pagePath: '/pages/index/index'
        })
        console.log('[博客首页] 搜索埋点已发送:', keyword)
      } catch (error) {
        console.error('[博客首页] 搜索埋点失败:', error)
      }
    }, 0)

    // 本地搜索
    const filtered = this.data.articles.filter(article => 
      article.title.toLowerCase().includes(keyword.toLowerCase()) ||
      article.summary.toLowerCase().includes(keyword.toLowerCase()) ||
      article.category.toLowerCase().includes(keyword.toLowerCase())
    )
    
    this.setData({
      filteredArticles: filtered
    })
  },

  /**
   * 搜索确认
   */
  onSearch() {
    this.performSearch(this.data.searchKeyword)
  },

  /**
   * 跳转到全部报告页面
   */
  onAllReportsTap() {
    // 全部报告按钮埋点
    setTimeout(() => {
      try {
        tracking.trackButtonClick('all_reports', '全部报告按钮点击')
        console.log('[博客首页] 全部报告按钮埋点已发送')
      } catch (error) {
        console.error('[博客首页] 全部报告按钮埋点失败:', error)
      }
    }, 0)
    
    wx.navigateTo({
      url: '../reportList/reportList'
    })
  },

  /**
   * 点击文章项
   */
  onArticleTap(e) {
    const articleId = e.currentTarget.dataset.id
    const type = e.currentTarget.dataset.type
    
    let article
    if (type === 'featured') {
      article = this.data.featuredArticles.find(item => item.id === articleId)
    } else {
      article = this.data.filteredArticles.find(item => item.id === articleId)
    }
    
    if (article) {
      // 文章点击埋点
      setTimeout(() => {
        try {
          tracking.trackButtonClick('blog_article', '博客文章点击', {
            articleId: articleId,
            articleTitle: article.title,
            articleCategory: article.category,
            clickType: type,
            clickPosition: 'blog_home'
          })
          console.log('[博客首页] 文章点击埋点已发送:', articleId, article.title)
        } catch (error) {
          console.error('[博客首页] 文章点击埋点失败:', error)
        }
      }, 0)

      // 跳转到报告详情页
      wx.navigateTo({
        url: `../reportDetail/reportDetail?id=${articleId}`,
        success: () => {
          console.log('[博客首页] 成功跳转到报告详情页:', articleId)
        },
        fail: (error) => {
          console.error('[博客首页] 跳转报告详情页失败:', error)
          wx.showToast({
            title: '跳转失败，请重试',
            icon: 'none'
          })
        }
      })
    }
  },

  /**
   * 阅读摘要
   */
  onReadSummary(e) {
    const articleId = e.currentTarget.dataset.id
    const article = this.data.filteredArticles.find(item => item.id === articleId)
    
    if (article) {
      // 阅读摘要埋点
      setTimeout(() => {
        try {
          tracking.trackButtonClick('read_summary', '阅读摘要按钮点击', {
            articleId: articleId,
            articleTitle: article.title
          })
          console.log('[博客首页] 阅读摘要埋点已发送:', articleId)
        } catch (error) {
          console.error('[博客首页] 阅读摘要埋点失败:', error)
        }
      }, 0)
      
      // 显示摘要详情
      wx.showModal({
        title: article.title,
        content: article.summary,
        showCancel: true,
        cancelText: '关闭',
        confirmText: '查看报告',
        success: (res) => {
          if (res.confirm) {
            this.onViewReport(e)
          }
        }
      })
    }
  },

  /**
   * 查看完整报告
   */
  onViewReport(e) {
    const articleId = e.currentTarget.dataset.id
    const article = this.data.filteredArticles.find(item => item.id === articleId)
    
    if (article) {
      // 查看报告埋点
      setTimeout(() => {
        try {
          tracking.trackButtonClick('view_report', '查看报告按钮点击', {
            articleId: articleId,
            articleTitle: article.title
          })
          console.log('[博客首页] 查看报告埋点已发送:', articleId)
        } catch (error) {
          console.error('[博客首页] 查看报告埋点失败:', error)
        }
      }, 0)
      
      wx.navigateTo({
        url: `../reportDetail/reportDetail?id=${articleId}`
      })
    }
  },

  /**
   * 加载更多内容
   */
  onLoadMore() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({
        page: this.data.page + 1
      })
      this.loadBlogContent()
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
          pagePath: '/pages/index/index'
        })
        console.log('[博客首页] 下拉刷新埋点已发送')
      } catch (error) {
        console.error('[博客首页] 下拉刷新埋点失败:', error)
      }
    }, 0)
    
    this.setData({
      page: 1,
      articles: [],
      filteredArticles: [],
      featuredArticles: []
    })
    
    await this.loadBlogContent()
    wx.stopPullDownRefresh()
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    this.onLoadMore()
  }
})