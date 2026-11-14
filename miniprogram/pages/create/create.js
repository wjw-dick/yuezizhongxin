const app = getApp()

function fmt(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

Page({
  data: {
    packages: [],
    rooms: [],
    packageIndex: 0,
    roomIndex: 0,
    checkinDate: fmt(new Date()),
    checkoutDate: '',
    careModes: ['一对一', '一对二', '集中护理'],
    careIndex: 0,
    form: { customerName: '', phone: '', companionCount: 1, remark: '' },
    total: 0,
    deposit: 0,
    submitting: false
  },

  async onLoad(query) {
    try {
      const [packages, rooms] = await Promise.all([
        app.request({ url: '/api/open/site/packages' }),
        app.request({ url: '/api/open/site/rooms' })
      ])
      const list = packages || []
      let index = 0
      if (query.packageId) {
        const found = list.findIndex((p) => String(p.id) === String(query.packageId))
        index = found >= 0 ? found : 0
      }
      this.setData({ packages: list, rooms: rooms || [], packageIndex: index })
      this.calcDate()
    } catch (e) {
      // ignore
    }
  },

  calcDate() {
    const pkg = this.data.packages[this.data.packageIndex]
    if (!pkg) return
    const start = new Date(this.data.checkinDate)
    start.setDate(start.getDate() + Number(pkg.days || 28))
    const total = Number(pkg.price || 0)
    const deposit = Math.round(total * Number(pkg.depositRatio || 0.2) * 100) / 100
    this.setData({
      checkoutDate: fmt(start),
      total,
      deposit,
      'form.careMode': pkg.careMode || this.data.careModes[0]
    })
  },

  onPackage(e) {
    this.setData({ packageIndex: Number(e.detail.value) })
    this.calcDate()
  },

  onRoom(e) {
    this.setData({ roomIndex: Number(e.detail.value) })
  },

  onCare(e) {
    this.setData({ careIndex: Number(e.detail.value) })
  },

  onCheckin(e) {
    this.setData({ checkinDate: e.detail.value })
    this.calcDate()
  },

  onInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.key}`]: e.detail.value })
  },

  async submit() {
    const { packages, packageIndex, rooms, roomIndex, form, checkinDate, checkoutDate, careModes, careIndex } = this.data
    const pkg = packages[packageIndex]
    const room = rooms[roomIndex]
    if (!pkg || !room) {
      wx.showToast({ title: '请选择套餐与房间', icon: 'none' })
      return
    }
    if (!form.customerName.trim() || !/^1[3-9]\d{9}$/.test(form.phone)) {
      wx.showToast({ title: '请填写称呼与正确手机号', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const order = await app.request({
        url: '/api/open/orders',
        method: 'POST',
        data: {
          customerName: form.customerName,
          phone: form.phone,
          packageId: pkg.id,
          roomTypeId: room.id,
          roomId: room.id,
          checkinDate,
          checkoutDate,
          careMode: careModes[careIndex],
          companionCount: Number(form.companionCount) || 1,
          remark: form.remark,
          source: '小程序'
        }
      })
      const pay = await app.request({
        url: `/api/open/orders/${order.orderId || order.id}/pay`,
        method: 'POST',
        data: { payType: '定金', method: '微信支付' }
      })
      app.globalData.lastOrderNo = order.orderNo || ''
      this.requestPay(order, pay)
    } catch (e) {
      this.setData({ submitting: false })
    }
  },

  requestPay(order, pay) {
    const prepay = pay.prepay || {}
    const params = prepay.payParams || prepay
    if (!params || !params.timeStamp) {
      wx.showModal({
        title: '订单已创建',
        content: '档期已为您锁定，支付需在门店完成或稍后在订单中心重试。',
        showCancel: false,
        success: () => wx.redirectTo({ url: `/pages/result/result?orderNo=${order.orderNo}&status=pending` })
      })
      this.setData({ submitting: false })
      return
    }
    wx.requestPayment({
      timeStamp: String(params.timeStamp),
      nonceStr: params.nonceStr,
      package: params.package,
      signType: params.signType || 'RSA',
      paySign: params.paySign,
      success: () => {
        app.globalData.lastOrderNo = order.orderNo
        wx.redirectTo({ url: `/pages/result/result?orderNo=${order.orderNo}&status=paid&paymentId=${pay.paymentId}` })
      },
      fail: () => {
        wx.redirectTo({ url: `/pages/result/result?orderNo=${order.orderNo}&status=cancel` })
      },
      complete: () => this.setData({ submitting: false })
    })
  }
})
