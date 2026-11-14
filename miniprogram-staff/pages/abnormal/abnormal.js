const app = getApp()

Page({
  data: {
    list: [],
    loading: false,
    status: '待处置',
    customers: [],
    customerIndex: -1,
    showForm: false,
    form: {
      abnormalType: '宝宝护理',
      urgency: '一般',
      description: ''
    },
    types: ['宝宝护理', '妈妈护理', '餐饮问题', '环境设施', '家属诉求'],
    urgencies: ['一般', '紧急']
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
      const [list, bench] = await Promise.all([
        app.request({ url: '/api/tenant/mobile/abnormals?status=' + encodeURIComponent(this.data.status) }),
        app.request({ url: '/api/tenant/mobile/workbench', silent: true })
      ])
      const seen = {}
      const customers = []
      ;(bench.items || []).forEach((row) => {
        if (row.orderId && !seen[row.orderId]) {
          seen[row.orderId] = true
          customers.push({ orderId: row.orderId, label: (row.customerName || '客户') + ' · ' + row.targetType })
        }
      })
      this.setData({ list: list || [], customers, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  switchStatus(e) {
    this.setData({ status: e.currentTarget.dataset.status }, () => this.load())
  },

  openForm() {
    this.setData({ showForm: true })
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  noop() {},

  pickCustomer(e) {
    this.setData({ customerIndex: Number(e.detail.value) })
  },

  pickType(e) {
    this.setData({ 'form.abnormalType': e.currentTarget.dataset.value })
  },

  pickUrgency(e) {
    this.setData({ 'form.urgency': e.currentTarget.dataset.value })
  },

  input(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value })
  },

  async submit() {
    const form = this.data.form
    const customer = this.data.customers[this.data.customerIndex]
    if (!customer) {
      wx.showToast({ title: '请选择客户', icon: 'none' })
      return
    }
    if (!form.description || form.description.trim().length < 5) {
      wx.showToast({ title: '请描述具体情况（不少于 5 个字）', icon: 'none' })
      return
    }
    try {
      await app.request({
        url: '/api/tenant/mobile/abnormals',
        method: 'POST',
        data: {
          orderId: customer.orderId,
          abnormalType: form.abnormalType,
          urgency: form.urgency,
          description: form.description.trim(),
          familyNotified: 0
        }
      })
      this.setData({
        showForm: false,
        customerIndex: -1,
        form: { abnormalType: '宝宝护理', urgency: '一般', description: '' }
      })
      wx.showModal({
        title: '已上报',
        content: form.urgency === '紧急'
          ? '已推送主管并置顶提醒，请同步守在客户身边直到处置人到达。'
          : '护理主管会在 30 分钟内处置，处置结果会同步到消息。',
        showCancel: false
      })
      this.load()
    } catch (e) {
      // 请求层已提示
    }
  },

  callSupervisor() {
    wx.showModal({
      title: '联系主管',
      content: '请在通讯录拨打护理主管电话；紧急情况同时按床头呼叫铃。',
      showCancel: false
    })
  }
})
