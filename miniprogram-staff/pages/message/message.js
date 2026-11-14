const app = getApp()

Page({
  data: {
    list: [],
    loading: false
  },

  onShow() {
    this.load()
  },

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      const data = await app.request({ url: '/api/tenant/mobile/workbench' })
      const list = []
      ;(data.abnormals || []).forEach((row) => {
        list.push({
          id: 'ab-' + row.id,
          type: '异常',
          tone: row.urgency === '紧急' ? 'warn' : '',
          title: row.abnormalType + ' · ' + row.urgency,
          desc: row.description,
          time: row.createdTime,
          link: '/pages/abnormal/abnormal'
        })
      })
      ;(data.handovers || []).forEach((row) => {
        list.push({
          id: 'hd-' + row.id,
          type: '交接',
          tone: row.status === '已确认' ? '' : 'warn',
          title: row.shiftType + '交接单 · ' + row.status,
          desc: row.watchPoints,
          time: row.createdTime,
          link: '/pages/handover/handover'
        })
      })
      ;(data.meals || []).forEach((row) => {
        list.push({
          id: 'ml-' + row.id,
          type: '出餐',
          tone: '',
          title: row.mealType + '待核对',
          desc: row.dishes,
          time: row.createdTime,
          link: '/pages/meal/meal'
        })
      })
      this.setData({ list, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  open(e) {
    const link = e.currentTarget.dataset.link
    if (link.indexOf('/pages/meal') === 0) {
      wx.switchTab({ url: link })
      return
    }
    wx.navigateTo({ url: link })
  }
})
