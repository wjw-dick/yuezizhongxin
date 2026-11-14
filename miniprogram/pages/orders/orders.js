const app = getApp()
const auth = require('../../utils/auth')

const TABS = ['全部', '待支付', '待入住', '在住', '待结算', '已完成']

Page({
  data: {
    tabs: TABS,
    tab: '全部',
    list: [],
    loading: false,
    logged: false,
    detail: null,
    showDetail: false,
    paying: false
  },

  onLoad() {
    this.setData({ logged: auth.logged() })
  },

  onShow() {
    const logged = auth.logged()
    this.setData({ logged })
    if (logged) {
      this.load()
    } else {
      this.setData({ list: [] })
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
      const status = this.data.tab === '全部' ? '' : this.data.tab
      const list = await auth.memberRequest({
        url: '/api/open/customer/orders' + (status ? '?status=' + encodeURIComponent(status) : '')
      })
      this.setData({ list: (list || []).map(this.decorate), loading: false })
    } catch (e) {
      this.setData({ loading: false, list: [] })
      if (e && e.code === 40100) {
        this.setData({ logged: false })
      }
    }
  },

  /** 列表装饰：支付进度、状态配色、天数 */
  decorate(row) {
    const total = Number(row.totalAmount || 0)
    const paid = Number(row.paidAmount || 0)
    const rate = total > 0 ? Math.min(100, Math.round((paid / total) * 100)) : 0
    const start = row.checkinDate ? new Date(row.checkinDate) : null
    const end = row.checkoutDate ? new Date(row.checkoutDate) : null
    const days = start && end ? Math.round((end - start) / 86400000) : 0
    const tone = ['待支付'].includes(row.status) ? 'warn'
      : ['在住'].includes(row.status) ? 'live'
        : ['已取消', '已退订'].includes(row.status) ? 'muted' : ''
    return Object.assign({}, row, { rate, days, tone })
  },

  pickTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab }, () => this.load())
  },

  async openDetail(e) {
    const orderNo = e.currentTarget.dataset.no
    wx.showLoading({ title: '加载中' })
    try {
      const detail = await auth.memberRequest({ url: '/api/open/customer/orders/' + orderNo })
      this.setData({ detail, showDetail: true })
    } finally {
      wx.hideLoading()
    }
  },

  closeDetail() {
    this.setData({ showDetail: false, detail: null })
  },

  noop() {},

  async pay(e) {
    if (this.data.paying) {
      return
    }
    const id = e.currentTarget.dataset.id
    this.setData({ paying: true })
    wx.showLoading({ title: '发起支付' })
    try {
      const pay = await app.request({
        url: '/api/open/orders/' + id + '/pay',
        method: 'POST',
        data: { payType: '定金', method: '微信支付' }
      })
      wx.hideLoading()
      const params = pay && pay.prepay && pay.prepay.payParams
      if (!params || !params.timeStamp) {
        wx.showModal({
          title: '到店支付',
          content: '门店尚未开通在线支付，请到店或联系顾问完成定金支付，档期将为您保留 24 小时。',
          showCancel: false
        })
        this.setData({ paying: false })
        return
      }
      wx.requestPayment({
        timeStamp: params.timeStamp,
        nonceStr: params.nonceStr,
        package: params.package,
        signType: params.signType || 'RSA',
        paySign: params.paySign,
        success: () => {
          this.setData({ paying: false })
          wx.showToast({ title: '支付成功', icon: 'success' })
          this.load()
        },
        fail: () => {
          this.setData({ paying: false })
          wx.showToast({ title: '支付未完成', icon: 'none' })
        }
      })
    } catch (err) {
      wx.hideLoading()
      this.setData({ paying: false })
    }
  },

  applyRefund(e) {
    const orderNo = e.currentTarget.dataset.no
    wx.showModal({
      title: '申请退订',
      editable: true,
      placeholderText: '请填写退订原因（必填）',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        if (!res.content || !res.content.trim()) {
          wx.showToast({ title: '退订原因必填', icon: 'none' })
          return
        }
        try {
          const result = await auth.memberRequest({
            url: '/api/open/customer/orders/' + orderNo + '/refund',
            method: 'POST',
            data: { reason: res.content.trim() }
          })
          wx.showModal({
            title: '申请已提交',
            content: '退款单号 ' + result.refundNo + '，预计 3 个工作日内审核并原路退回。',
            showCancel: false
          })
          this.setData({ showDetail: false })
          this.load()
        } catch (err) {
          // 错误提示已在请求层统一处理
        }
      }
    })
  },

  applyChange() {
    wx.showModal({
      title: '变更订单',
      content: '换房、延期、改套餐请到店或联系顾问办理，涉及金额调整需重新签约确认。',
      showCancel: false
    })
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
  },

  previewContract(e) {
    const url = e.currentTarget.dataset.url
    if (!url) {
      wx.showToast({ title: '合同文件不存在', icon: 'none' })
      return
    }
    wx.downloadFile({
      url: url.indexOf('http') === 0 ? url : app.globalData.baseUrl + url,
      success: (res) => {
        wx.openDocument({ filePath: res.tempFilePath, showMenu: true })
      }
    })
  }
})
