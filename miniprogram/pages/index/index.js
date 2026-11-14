const app = getApp()

Page({
  data: {
    site: {},
    banners: [],
    packages: [],
    rooms: [],
    news: [],
    lang: 'zh-CN',
    loading: true
  },

  onLoad() {
    this.setData({ lang: app.globalData.lang })
    this.load()
  },

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      const data = await app.request({ url: '/api/open/site/home?lang=' + app.globalData.lang })
      this.setData({
        site: data.site || {},
        banners: data.banners || [],
        packages: (data.packages || []).slice(0, 3),
        rooms: (data.roomTypes || []).slice(0, 2),
        news: (data.news || []).slice(0, 3)
      })
      wx.setNavigationBarTitle({ title: (data.site && data.site.name) || '月之孕月子中心' })
    } catch (e) {
      // 首页失败时保留骨架，避免白屏
      this.setData({ site: { name: '月之孕月子中心', phone: '400-000-0000' } })
    } finally {
      this.setData({ loading: false })
    }
  },

  switchLang() {
    const next = app.globalData.lang === 'zh-CN' ? 'en-US' : 'zh-CN'
    app.globalData.lang = next
    wx.setStorageSync('lang', next)
    this.setData({ lang: next })
    this.load()
  },

  callStore() {
    const phone = this.data.site.phone
    if (phone) {
      wx.makePhoneCall({ phoneNumber: String(phone) })
    }
  },

  toBook() {
    wx.navigateTo({ url: '/pages/book/book' })
  },

  toRooms() {
    wx.switchTab({ url: '/pages/rooms/rooms' })
  },

  toCreate(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/create/create?packageId=' + id })
  },

  toServices() {
    wx.navigateTo({ url: '/pages/services/services' })
  },

  toCare() {
    wx.navigateTo({ url: '/pages/care/care' })
  },

  toArticle(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/index/index?articleId=' + id })
  }
})
