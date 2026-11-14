const app = getApp()

Page({
  data: {
    loading: false,
    staff: null,
    date: '',
    attendance: null,
    items: [],
    handovers: [],
    abnormals: [],
    meals: [],
    doneCount: 0,
    totalCount: 0,
    customerCount: 0,
    pendingMeals: 0,
    clocking: false,
    rate: 0
  },

  onShow() {
    if (!app.token()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.setData({ staff: wx.getStorageSync('staffInfo') || null })
    this.load()
  },

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      const data = await app.request({ url: '/api/tenant/mobile/workbench' })
      const total = data.totalCount || 0
      const att = data.attendance || null
      this.setData({
        loading: false,
        date: data.date,
        attendance: att ? Object.assign({}, att, {
          checkIn: this.hhmm(att.checkInTime),
          checkOut: this.hhmm(att.checkOutTime)
        }) : null,
        items: (data.items || []).map((row) => Object.assign({}, row, {
          done: row.status === '已完成'
        })),
        handovers: data.handovers || [],
        abnormals: data.abnormals || [],
        meals: (data.meals || []).slice(0, 5),
        doneCount: data.doneCount || 0,
        totalCount: total,
        customerCount: data.customerCount || 0,
        pendingMeals: data.pendingMeals || 0,
        rate: total ? Math.round((data.doneCount || 0) * 100 / total) : 0,
        staffName: (data.staff && data.staff.name) || (this.data.staff && this.data.staff.realName) || '护理师'
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  /** 时间戳截取 HH:mm，避免直接展示 ISO 字符串 */
  hhmm(value) {
    if (!value) {
      return ''
    }
    const text = String(value)
    const idx = text.indexOf('T')
    return idx > 0 ? text.substring(idx + 1, idx + 6) : text.substring(0, 5)
  },

  async clock(e) {
    const type = e.currentTarget.dataset.type
    this.setData({ clocking: true })
    wx.getLocation({
      type: 'gcj02',
      success: async (loc) => {
        await this.submitClock(type, loc.latitude.toFixed(5) + ',' + loc.longitude.toFixed(5))
      },
      fail: async () => {
        await this.submitClock(type, '定位未授权')
      }
    })
  },

  async submitClock(type, location) {
    try {
      await app.request({
        url: '/api/tenant/mobile/attendance',
        method: 'POST',
        data: { type, location }
      })
      wx.showToast({ title: type === 'in' ? '上班打卡成功' : '下班打卡成功', icon: 'success' })
      this.load()
    } catch (e) {
      // 请求层已提示
    }
    this.setData({ clocking: false })
  },

  toExec(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/exec/exec?id=' + id })
  },

  toHandover() {
    wx.navigateTo({ url: '/pages/handover/handover' })
  },

  toAbnormal() {
    wx.navigateTo({ url: '/pages/abnormal/abnormal' })
  },

  toMeal() {
    wx.switchTab({ url: '/pages/meal/meal' })
  },

  toMessage() {
    wx.switchTab({ url: '/pages/message/message' })
  }
})
