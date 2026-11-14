const app = getApp()
const auth = require('../../utils/auth')

Page({
  data: {
    logged: false,
    living: false,
    loading: false,
    order: null,
    roomNo: '',
    care: null,
    todayItems: [],
    records: [],
    trendMom: [],
    trendBaby: [],
    trendTab: '产妇',
    rate: 0,
    showFeedback: false,
    feedback: { abnormalType: '家属反馈', urgency: '一般', description: '' },
    showConfirm: false,
    confirmForm: { satisfied: true, remark: '' }
  },

  onLoad() {
    this.setData({ logged: auth.logged() })
  },

  onShow() {
    const logged = auth.logged()
    this.setData({ logged })
    if (logged) {
      this.load()
    }
  },

  onPullDownRefresh() {
    if (!auth.logged()) {
      wx.stopPullDownRefresh()
      return
    }
    this.load().then(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      const stay = await auth.memberRequest({ url: '/api/open/customer/stay' })
      if (!stay || !stay.living) {
        this.setData({ loading: false, living: false, care: null })
        return
      }
      const care = stay.care || {}
      const rate = care.rate || 0
      this.setData({
        loading: false,
        living: true,
        order: stay.order,
        roomNo: stay.roomNo || '',
        care,
        todayItems: care.today || [],
        records: (care.records || []).slice(0, 20),
        trendMom: this.normalizeTrend(care.trendMom),
        trendBaby: this.normalizeTrend(care.trendBaby),
        rate,
        ringStyle: this.ringStyle(rate)
      })
    } catch (e) {
      this.setData({ loading: false })
      if (e && e.code === 40100) {
        this.setData({ logged: false })
      }
    }
  },

  /** 完成率环形进度（conic-gradient 字符串，避免依赖 CSS 变量兼容性） */
  ringStyle(rate) {
    const deg = Math.max(0, Math.min(100, rate)) * 3.6
    return 'background: conic-gradient(#8ee0bf 0deg, #8ee0bf ' + deg
      + 'deg, rgba(255,255,255,0.22) ' + deg + 'deg, rgba(255,255,255,0.22) 360deg);'
  },

  /** 体征趋势转成柱高比例，便于纯 CSS 渲染 */
  normalizeTrend(points) {
    if (!points || !points.length) {
      return []
    }
    const rows = points.filter((p) => p.temperature)
    if (!rows.length) {
      return []
    }
    const values = rows.map((p) => Number(p.temperature))
    const max = Math.max.apply(null, values) + 0.5
    const min = Math.min.apply(null, values) - 0.5
    return rows.slice(-14).map((p) => {
      const value = Number(p.temperature)
      const height = Math.max(12, Math.round(((value - min) / (max - min || 1)) * 100))
      return {
        label: (p.time || '').toString().slice(5, 10),
        value: value.toFixed(1),
        height
      }
    })
  },

  pickTrend(e) {
    this.setData({ trendTab: e.currentTarget.dataset.type })
  },

  openFeedback() {
    this.setData({ showFeedback: true })
  },

  closeFeedback() {
    this.setData({ showFeedback: false })
  },

  noop() {},

  inputFeedback(e) {
    this.setData({ ['feedback.' + e.currentTarget.dataset.field]: e.detail.value })
  },

  pickUrgency(e) {
    this.setData({ 'feedback.urgency': e.currentTarget.dataset.value })
  },

  pickType(e) {
    this.setData({ 'feedback.abnormalType': e.currentTarget.dataset.value })
  },

  async submitFeedback() {
    const form = this.data.feedback
    if (!form.description || form.description.trim().length < 5) {
      wx.showToast({ title: '请描述具体问题（不少于 5 个字）', icon: 'none' })
      return
    }
    try {
      await auth.memberRequest({
        url: '/api/open/customer/care/feedback',
        method: 'POST',
        data: {
          orderId: this.data.order.id,
          abnormalType: form.abnormalType,
          urgency: form.urgency,
          description: form.description.trim()
        }
      })
      this.setData({ showFeedback: false, feedback: { abnormalType: '家属反馈', urgency: '一般', description: '' } })
      wx.showModal({
        title: '已提交',
        content: form.urgency === '紧急'
          ? '已通知值班主管，护理主管 10 分钟内会与您联系；如情况紧急请直接按呼叫铃。'
          : '护理主管会在 30 分钟内响应并给出处置结果。',
        showCancel: false
      })
    } catch (e) {
      // 请求层已提示
    }
  },

  openConfirm() {
    this.setData({ showConfirm: true })
  },

  closeConfirm() {
    this.setData({ showConfirm: false })
  },

  pickSatisfied(e) {
    this.setData({ 'confirmForm.satisfied': e.currentTarget.dataset.value === '1' })
  },

  inputConfirm(e) {
    this.setData({ 'confirmForm.remark': e.detail.value })
  },

  async submitConfirm() {
    try {
      await auth.memberRequest({
        url: '/api/open/customer/care/confirm',
        method: 'POST',
        data: {
          orderId: this.data.order.id,
          satisfied: this.data.confirmForm.satisfied,
          remark: this.data.confirmForm.remark
        }
      })
      this.setData({ showConfirm: false })
      wx.showToast({ title: '已提交确认', icon: 'success' })
      this.load()
    } catch (e) {
      // 请求层已提示
    }
  },

  toLogin() {
    auth.toLogin()
  },

  toRooms() {
    wx.switchTab({ url: '/pages/rooms/rooms' })
  },

  callStaff() {
    const phone = (app.globalData.site && app.globalData.site.phone) || '400-000-0000'
    wx.makePhoneCall({ phoneNumber: phone.replace(/-/g, '') })
  }
})
