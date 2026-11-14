const app = getApp()

Page({
  data: {
    tab: 'pending',
    list: [],
    rooms: [],
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
    const checked = this.data.tab === 'pending' ? 0 : 1
    try {
      const list = await app.request({
        url: '/api/tenant/mobile/meals?checked=' + checked
      })
      const groups = {}
      ;(list || []).forEach((row) => {
        const key = row.roomId || 0
        groups[key] = groups[key] || []
        groups[key].push(row)
      })
      const rooms = Object.keys(groups).map((key) => ({
        roomId: Number(key),
        meals: groups[key]
      }))
      this.setData({ list: list || [], rooms, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab }, () => this.load())
  },

  check(e) {
    const id = e.currentTarget.dataset.id
    const dishes = e.currentTarget.dataset.dishes
    wx.showActionSheet({
      itemList: ['正常出餐', '有问题（填写说明）'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.submit(id, null)
        } else {
          wx.showModal({
            title: '出餐异常',
            editable: true,
            placeholderText: '如：菜品温度不足、忌口未规避',
            success: (input) => {
              if (!input.confirm) {
                return
              }
              if (!input.content || !input.content.trim()) {
                wx.showToast({ title: '请填写异常说明', icon: 'none' })
                return
              }
              this.submit(id, input.content.trim())
            }
          })
        }
      }
    })
  },

  async submit(id, abnormal) {
    try {
      await app.request({
        url: '/api/tenant/mobile/meals/' + id + '/check',
        method: 'POST',
        data: { abnormal }
      })
      wx.showToast({ title: abnormal ? '已记录异常' : '已核对', icon: 'success' })
      this.load()
    } catch (e) {
      // 请求层已提示
    }
  },

  noop() {}
})
