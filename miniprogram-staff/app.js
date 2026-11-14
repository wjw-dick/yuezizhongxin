App({
  globalData: {
    // 生产环境改为 https://api.yuezizhongxin.com
    baseUrl: 'http://127.0.0.1:8080',
    tenantCode: 'demo',
    staff: null
  },

  onLaunch() {
    const tenant = wx.getStorageSync('staffTenant')
    if (tenant) {
      this.globalData.tenantCode = tenant
    }
    this.globalData.staff = wx.getStorageSync('staffInfo') || null
  },

  token() {
    return wx.getStorageSync('staffToken') || ''
  },

  saveLogin(login, tenantCode) {
    wx.setStorageSync('staffToken', login.token)
    wx.setStorageSync('staffInfo', login)
    wx.setStorageSync('staffTenant', tenantCode)
    this.globalData.staff = login
    this.globalData.tenantCode = tenantCode
  },

  logout() {
    wx.removeStorageSync('staffToken')
    wx.removeStorageSync('staffInfo')
    this.globalData.staff = null
  },

  /** 登录态请求：自动带令牌与租户标识，401 统一回登录页 */
  request({ url, method = 'GET', data, silent = false }) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: this.globalData.baseUrl + url,
        method,
        data,
        header: {
          'content-type': 'application/json',
          'X-Tenant-Code': this.globalData.tenantCode,
          Authorization: 'Bearer ' + this.token()
        },
        success: (res) => {
          const body = res.data || {}
          if (body.code === 0) {
            resolve(body.data)
            return
          }
          if (body.code === 40100) {
            this.logout()
            wx.reLaunch({ url: '/pages/login/login' })
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
  },

  /** 免登录请求（登录页专用） */
  openRequest({ url, method = 'POST', data }) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: this.globalData.baseUrl + url,
        method,
        data,
        header: {
          'content-type': 'application/json',
          'X-Tenant-Code': this.globalData.tenantCode
        },
        success: (res) => {
          const body = res.data || {}
          if (body.code === 0) {
            resolve(body.data)
            return
          }
          wx.showToast({ title: body.msg || '登录失败', icon: 'none' })
          reject(new Error(body.msg || '登录失败'))
        },
        fail: (err) => {
          wx.showToast({ title: '网络异常', icon: 'none' })
          reject(err)
        }
      })
    })
  }
})
