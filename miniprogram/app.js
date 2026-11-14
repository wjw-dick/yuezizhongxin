App({
  globalData: {
    // 生产环境改为 https://api.yuezizhongxin.com，并在小程序后台配置 request 合法域名
    baseUrl: 'http://127.0.0.1:8080',
    tenantCode: 'demo',
    lang: 'zh-CN',
    site: {},
    orderDraft: null,
    lastOrderNo: ''
  },

  onLaunch() {
    const tenant = wx.getStorageSync('tenantCode')
    if (tenant) {
      this.globalData.tenantCode = tenant
    }
    const lang = wx.getStorageSync('lang')
    if (lang) {
      this.globalData.lang = lang
    }
  },

  /** 统一请求：带租户标识、错误提示与登录态兜底 */
  request({ url, method = 'GET', data, silent = false, header = {} }) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: this.globalData.baseUrl + url,
        method,
        data,
        header: Object.assign({
          'content-type': 'application/json',
          'X-Tenant-Code': this.globalData.tenantCode
        }, header),
        success: (res) => {
          const body = res.data || {}
          if (body.code === 0) {
            resolve(body.data)
            return
          }
          if (!silent) {
            wx.showToast({ title: body.msg || '请求失败', icon: 'none' })
          }
          const err = new Error(body.msg || '请求失败')
          err.code = body.code
          reject(err)
        },
        fail: (err) => {
          if (!silent) {
            wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
          }
          reject(err)
        }
      })
    })
  }
})
