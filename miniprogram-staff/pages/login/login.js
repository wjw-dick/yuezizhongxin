const app = getApp()

Page({
  data: {
    tenantCode: 'demo',
    username: '',
    password: '',
    loading: false,
    tip: ''
  },

  onLoad() {
    this.setData({
      tenantCode: wx.getStorageSync('staffTenant') || app.globalData.tenantCode || 'demo'
    })
    if (app.token()) {
      wx.reLaunch({ url: '/pages/workbench/workbench' })
    }
  },

  input(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value })
  },

  async login() {
    const { tenantCode, username, password } = this.data
    if (!username || !password) {
      wx.showToast({ title: '请输入账号与密码', icon: 'none' })
      return
    }
    this.setData({ loading: true, tip: '' })
    app.globalData.tenantCode = tenantCode
    try {
      const login = await app.openRequest({
        url: '/api/tenant/auth/login',
        data: { tenantCode, username, password }
      })
      if (login.platform) {
        this.setData({ tip: '平台账号不能登录护理师端，请使用门店员工账号' })
        this.setData({ loading: false })
        return
      }
      app.saveLogin(login, tenantCode)
      wx.reLaunch({ url: '/pages/workbench/workbench' })
    } catch (e) {
      this.setData({ loading: false })
      return
    }
    this.setData({ loading: false })
  }
})
