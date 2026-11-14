const app = getApp()

Page({
  data: {
    id: null,
    loading: true,
    item: null,
    customerName: '',
    careMode: '',
    recent: [],
    form: {
      temperature: '',
      bloodPressure: '',
      pulse: '',
      weight: '',
      feedingAmount: '',
      jaundiceValue: '',
      stoolCount: '',
      content: ''
    },
    saving: false
  },

  onLoad(query) {
    this.setData({ id: Number(query.id) })
    this.load()
  },

  async load() {
    try {
      const data = await app.request({ url: '/api/tenant/mobile/items/' + this.data.id })
      this.setData({
        loading: false,
        item: data.item,
        customerName: data.customerName,
        careMode: data.careMode,
        recent: data.recent || []
      })
      wx.setNavigationBarTitle({ title: (data.item && data.item.itemName) || '护理执行' })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  input(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value })
  },

  async submit() {
    if (this.data.saving) {
      return
    }
    const form = this.data.form
    const payload = {}
    Object.keys(form).forEach((key) => {
      if (form[key] !== '' && form[key] !== null) {
        payload[key] = key === 'content' || key === 'bloodPressure' ? form[key] : Number(form[key])
      }
    })
    if (!payload.content) {
      wx.showToast({ title: '请填写护理记录内容', icon: 'none' })
      return
    }
    if (payload.temperature && (payload.temperature < 34 || payload.temperature > 42)) {
      wx.showToast({ title: '体温超出合理范围（34-42℃）', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    try {
      await app.request({
        url: '/api/tenant/mobile/items/' + this.data.id + '/execute',
        method: 'POST',
        data: payload
      })
      wx.showModal({
        title: '已提交',
        content: '护理记录已同步至客户小程序与护理看板。',
        showCancel: false,
        success: () => wx.navigateBack()
      })
    } catch (e) {
      // 请求层已提示
    }
    this.setData({ saving: false })
  }
})
