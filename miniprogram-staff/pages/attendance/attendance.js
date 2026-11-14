const app = getApp()

Page({
  data: {
    records: [],
    profile: null,
    loading: false,
    clocking: false,
    month: ''
  },

  onShow() {
    if (!app.token()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load()
  },

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      const [records, profile] = await Promise.all([
        app.request({ url: '/api/tenant/mobile/attendance' }),
        app.request({ url: '/api/tenant/mobile/profile', silent: true })
      ])
      const name = profile && profile.name ? String(profile.name) : ''
      this.setData({
        records: records || [],
        profile,
        avatarChar: name ? name.substring(0, 1) : '员',
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  clock(e) {
    const type = e.currentTarget.dataset.type
    this.setData({ clocking: true })
    wx.getLocation({
      type: 'gcj02',
      success: async (loc) => {
        await this.submit(type, loc.latitude.toFixed(5) + ',' + loc.longitude.toFixed(5))
      },
      fail: async () => {
        await this.submit(type, '定位未授权')
      }
    })
  },

  async submit(type, location) {
    try {
      await app.request({
        url: '/api/tenant/mobile/attendance',
        method: 'POST',
        data: { type, location }
      })
      wx.showToast({ title: '打卡成功', icon: 'success' })
      this.load()
    } catch (e) {
      // 请求层已提示
    }
    this.setData({ clocking: false })
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需重新输入账号密码。',
      success: (res) => {
        if (!res.confirm) {
          return
        }
        app.logout()
        wx.reLaunch({ url: '/pages/login/login' })
      }
    })
  }
})
