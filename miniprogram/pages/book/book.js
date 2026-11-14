const app = getApp()
const SLOTS = ['09:00-10:30', '10:30-12:00', '14:00-15:30', '15:30-17:00', '17:00-18:30']

function today() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

Page({
  data: {
    slots: SLOTS,
    slotIndex: 0,
    room: '',
    form: {
      customerName: '',
      phone: '',
      dueDate: '',
      visitDate: '',
      personCount: 2,
      remark: ''
    },
    minDate: today(),
    submitting: false
  },

  onLoad(query) {
    this.setData({
      room: query.room ? decodeURIComponent(query.room) : '',
      'form.visitDate': today(),
      'form.intendRoomType': query.room ? decodeURIComponent(query.room) : ''
    })
  },

  onInput(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ [`form.${key}`]: e.detail.value })
  },

  onDate(e) {
    this.setData({ 'form.visitDate': e.detail.value })
  },

  onDue(e) {
    this.setData({ 'form.dueDate': e.detail.value })
  },

  onSlot(e) {
    this.setData({ slotIndex: Number(e.detail.value) })
  },

  onPerson(e) {
    this.setData({ 'form.personCount': Number(e.detail.value) })
  },

  async submit() {
    const f = this.data.form
    if (!f.customerName.trim()) {
      wx.showToast({ title: '请填写称呼', icon: 'none' })
      return
    }
    if (!/^1[3-9]\d{9}$/.test(f.phone)) {
      wx.showToast({ title: '请填写正确的手机号', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const lead = await app.request({
        url: '/api/open/leads',
        method: 'POST',
        data: {
          customerName: f.customerName,
          phone: f.phone,
          dueDate: f.dueDate || null,
          intendRoomType: f.intendRoomType || null,
          remark: f.remark,
          sourceCode: '小程序'
        }
      })
      await app.request({
        url: '/api/open/leads/visits',
        method: 'POST',
        data: {
          leadId: lead.leadId,
          visitDate: f.visitDate,
          timeSlot: SLOTS[this.data.slotIndex],
          personCount: f.personCount,
          feedback: '小程序预约参观'
        }
      })
      wx.showModal({
        title: '预约成功',
        content: '顾问会在 30 分钟内电话确认时段，到店请出示预约信息。',
        showCancel: false,
        success: () => wx.navigateBack()
      })
    } catch (e) {
      // 错误提示已在请求层处理
    } finally {
      this.setData({ submitting: false })
    }
  },

  callStore() {
    wx.makePhoneCall({ phoneNumber: '4000000000' })
  }
})
