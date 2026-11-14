const app = getApp()

Page({
  data: {
    list: [],
    loading: false,
    showForm: false,
    form: {
      shiftType: '白班',
      watchPoints: '',
      unfinished: '',
      abnormalNotes: ''
    },
    shiftTypes: ['白班', '夜班']
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
      const list = await app.request({ url: '/api/tenant/mobile/handovers' })
      this.setData({ list: list || [], loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  openForm() {
    this.setData({ showForm: true })
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  noop() {},

  pickShift(e) {
    this.setData({ 'form.shiftType': e.currentTarget.dataset.value })
  },

  input(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value })
  },

  async submit() {
    const form = this.data.form
    if (!form.watchPoints || form.watchPoints.trim().length < 5) {
      wx.showToast({ title: '请填写重点观察事项', icon: 'none' })
      return
    }
    try {
      await app.request({
        url: '/api/tenant/mobile/handovers',
        method: 'POST',
        data: {
          shiftDate: null,
          shiftType: form.shiftType,
          watchPoints: form.watchPoints.trim(),
          unfinished: form.unfinished,
          abnormalNotes: form.abnormalNotes
        }
      })
      this.setData({
        showForm: false,
        form: { shiftType: '白班', watchPoints: '', unfinished: '', abnormalNotes: '' }
      })
      wx.showToast({ title: '交接单已创建', icon: 'success' })
      this.load()
    } catch (e) {
      // 请求层已提示
    }
  },

  confirm(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '接班确认',
      content: '确认已了解重点观察事项与未完成事项，并完成接班？',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await app.request({
            url: '/api/tenant/mobile/handovers/' + id + '/confirm',
            method: 'POST',
            data: { signKey: 'APP' }
          })
          wx.showToast({ title: '已确认接班', icon: 'success' })
          this.load()
        } catch (err) {
          // 请求层已提示
        }
      }
    })
  }
})
