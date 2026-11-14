const app = getApp()
const auth = require('../../utils/auth')

Page({
  data: {
    orderNo: '',
    status: 'paid',
    paymentId: '',
    title: '',
    desc: '',
    tip: ''
  },

  onLoad(query) {
    const status = query.status || 'paid'
    const preset = {
      paid: {
        title: '定金支付成功',
        desc: '档期已为您锁定，顾问会在 2 小时内电话确认入馆细节',
        tip: '请保留订单号，到店签约时出示'
      },
      pending: {
        title: '订单已提交',
        desc: '当前未开通在线支付，请到店或联系顾问完成定金支付',
        tip: '未支付定金的订单 24 小时后自动释放档期'
      },
      cancel: {
        title: '支付未完成',
        desc: '您已取消支付，档期会为您保留 24 小时',
        tip: '可在「订单」中重新发起支付'
      }
    }
    const item = preset[status] || preset.paid
    this.setData({
      orderNo: query.orderNo || app.globalData.lastOrderNo || '',
      status,
      paymentId: query.paymentId || '',
      title: item.title,
      desc: item.desc,
      tip: item.tip
    })
  },

  toOrders() {
    wx.switchTab({ url: '/pages/orders/orders' })
  },

  toHome() {
    wx.switchTab({ url: '/pages/index/index' })
  },

  copyOrderNo() {
    if (!this.data.orderNo) {
      return
    }
    wx.setClipboardData({ data: this.data.orderNo })
  },

  callStaff() {
    const phone = (app.globalData.site && app.globalData.site.phone) || '400-000-0000'
    wx.makePhoneCall({ phoneNumber: phone.replace(/-/g, '') })
  }
})
