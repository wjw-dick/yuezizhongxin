const app = getApp()
const auth = require('../../utils/auth')

Page({
  data: {
    logged: false,
    living: false,
    loading: false,
    tab: 'rehab',
    order: null,
    rehab: { courses: [], assesses: [], appointments: [], records: [] },
    meal: { date: '', meals: [], restricts: [] },
    showAppoint: false,
    appoint: { itemId: '', date: '', startTime: '10:00', endTime: '11:00', courseIndex: -1 },
    courseOptions: [],
    showRestrict: false,
    restrictForm: { restrictType: '忌口', content: '' },
    showAddon: false,
    addon: { mealType: '加餐', dishes: '', charge: '0' },
    mealTypes: ['早餐', '午餐', '晚餐', '加餐', '夜宵'],
    today: ''
  },

  onLoad() {
    const now = new Date()
    const today = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0')
      + '-' + String(now.getDate()).padStart(2, '0')
    this.setData({ logged: auth.logged(), today, 'appoint.date': today })
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
        this.setData({ loading: false, living: false })
        return
      }
      this.setData({ order: stay.order, living: true })
      await Promise.all([this.loadRehab(), this.loadMeal()])
      this.setData({ loading: false })
    } catch (e) {
      this.setData({ loading: false })
      if (e && e.code === 40100) {
        this.setData({ logged: false })
      }
    }
  },

  async loadRehab() {
    const orderId = this.data.order.id
    const rehab = await auth.memberRequest({ url: '/api/open/customer/rehab?orderId=' + orderId })
    const courses = rehab.courses || []
    this.setData({
      rehab: {
        courses,
        assesses: rehab.assesses || [],
        appointments: rehab.appointments || [],
        records: rehab.records || []
      },
      courseOptions: courses.map((row) => ({
        label: row.itemName + '（余 ' + row.remain + ' 次）',
        itemId: row.course.itemId,
        remain: row.remain
      }))
    })
  },

  async loadMeal() {
    const orderId = this.data.order.id
    const meal = await auth.memberRequest({
      url: '/api/open/customer/meal?orderId=' + orderId + '&date=' + this.data.today
    })
    this.setData({ meal: { date: meal.date, meals: meal.meals || [], restricts: meal.restricts || [] } })
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  /* ---------- 康复 ---------- */

  openAppoint() {
    if (!this.data.courseOptions.length) {
      wx.showToast({ title: '暂无可用疗程，请联系顾问', icon: 'none' })
      return
    }
    this.setData({ showAppoint: true })
  },

  closeAppoint() {
    this.setData({ showAppoint: false })
  },

  noop() {},

  pickCourse(e) {
    const index = Number(e.detail.value)
    const option = this.data.courseOptions[index]
    this.setData({
      'appoint.courseIndex': index,
      'appoint.itemId': option ? option.itemId : ''
    })
  },

  onDate(e) {
    this.setData({ 'appoint.date': e.detail.value })
  },

  onStart(e) {
    const start = e.detail.value
    const hour = Number(start.split(':')[0])
    const end = String(hour + 1).padStart(2, '0') + ':' + start.split(':')[1]
    this.setData({ 'appoint.startTime': start, 'appoint.endTime': end })
  },

  onEnd(e) {
    this.setData({ 'appoint.endTime': e.detail.value })
  },

  async submitAppoint() {
    const form = this.data.appoint
    if (!form.itemId) {
      wx.showToast({ title: '请选择康复项目', icon: 'none' })
      return
    }
    try {
      await auth.memberRequest({
        url: '/api/open/customer/rehab/appoint',
        method: 'POST',
        data: {
          orderId: this.data.order.id,
          itemId: form.itemId,
          date: form.date,
          startTime: form.startTime,
          endTime: form.endTime
        }
      })
      this.setData({ showAppoint: false })
      wx.showModal({
        title: '预约已提交',
        content: '门店会根据治疗师排班确认时段，确认后在小程序通知您。',
        showCancel: false
      })
      this.loadRehab()
    } catch (e) {
      // 请求层已提示（含时段冲突）
    }
  },

  cancelAppoint(e) {
    const appointmentId = e.currentTarget.dataset.id
    wx.showModal({
      title: '取消预约',
      content: '取消后次数会退回疗程卡，确认取消？',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await auth.memberRequest({
            url: '/api/open/customer/rehab/cancel',
            method: 'POST',
            data: { orderId: this.data.order.id, appointmentId }
          })
          wx.showToast({ title: '已取消', icon: 'success' })
          this.loadRehab()
        } catch (e) {
          // 请求层已提示
        }
      }
    })
  },

  /* ---------- 月子餐 ---------- */

  openRestrict() {
    this.setData({ showRestrict: true })
  },

  closeRestrict() {
    this.setData({ showRestrict: false })
  },

  pickRestrictType(e) {
    this.setData({ 'restrictForm.restrictType': e.currentTarget.dataset.value })
  },

  inputRestrict(e) {
    this.setData({ 'restrictForm.content': e.detail.value })
  },

  async submitRestrict() {
    const content = this.data.restrictForm.content
    if (!content || !content.trim()) {
      wx.showToast({ title: '请填写忌口或过敏原', icon: 'none' })
      return
    }
    try {
      await auth.memberRequest({
        url: '/api/open/customer/meal/restrict',
        method: 'POST',
        data: {
          orderId: this.data.order.id,
          restrictType: this.data.restrictForm.restrictType,
          content: content.trim()
        }
      })
      this.setData({ showRestrict: false, restrictForm: { restrictType: '忌口', content: '' } })
      wx.showToast({ title: '已登记', icon: 'success' })
      this.loadMeal()
    } catch (e) {
      // 请求层已提示
    }
  },

  openAddon() {
    this.setData({ showAddon: true })
  },

  closeAddon() {
    this.setData({ showAddon: false })
  },

  pickMealType(e) {
    this.setData({ 'addon.mealType': e.currentTarget.dataset.value })
  },

  inputAddon(e) {
    this.setData({ ['addon.' + e.currentTarget.dataset.field]: e.detail.value })
  },

  async submitAddon() {
    const form = this.data.addon
    if (!form.dishes || !form.dishes.trim()) {
      wx.showToast({ title: '请填写加餐内容', icon: 'none' })
      return
    }
    wx.showModal({
      title: '确认加餐',
      content: (form.dishes + '（' + form.mealType + '）') + (Number(form.charge) > 0
        ? '，单独计费 ¥' + form.charge + '，出馆结算时一并收取。'
        : '，免费加餐由营养师确认。'),
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await auth.memberRequest({
            url: '/api/open/customer/meal/addon',
            method: 'POST',
            data: {
              orderId: this.data.order.id,
              date: this.data.today,
              mealType: form.mealType,
              dishes: form.dishes.trim(),
              charge: Number(form.charge) || 0
            }
          })
          this.setData({ showAddon: false, addon: { mealType: '加餐', dishes: '', charge: '0' } })
          wx.showToast({ title: '已提交', icon: 'success' })
          this.loadMeal()
        } catch (e) {
          // 请求层已提示
        }
      }
    })
  },

  toLogin() {
    auth.toLogin()
  },

  toRooms() {
    wx.switchTab({ url: '/pages/rooms/rooms' })
  }
})
