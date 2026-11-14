const app = getApp()
const auth = require('../../utils/auth')

Page({
  data: {
    logged: false,
    site: {},
    profile: null,
    phone: '',
    code: '',
    devCode: '',
    countdown: 0,
    sending: false,
    logging: false,
    points: [],
    coupons: [],
    claimable: [],
    recharges: [],
    addresses: [],
    referrals: [],
    panel: '',
    rechargeRule: null,
    rechargeAmount: '',
    ruleShown: false,
    addressForm: { receiver: '', phone: '', region: '', detail: '', isDefault: 0 },
    showAddress: false,
    lang: 'zh-CN',
    langs: [
      { code: 'zh-CN', label: '简体中文' },
      { code: 'en-US', label: 'English' }
    ]
  },

  onLoad() {
    const cached = auth.profile()
    this.setData({
      logged: auth.logged(),
      profile: cached,
      avatarChar: this.firstChar(cached && cached.nickname),
      lang: wx.getStorageSync('lang') || 'zh-CN'
    })
    this.loadSite()
    if (auth.logged()) {
      this.refreshProfile()
    }
  },

  onShow() {
    const logged = auth.logged()
    this.setData({ logged, profile: auth.profile() })
    if (logged && !this.data.profile) {
      this.refreshProfile()
    }
  },

  /** 头像占位取昵称首字（含手机号脱敏场景） */
  firstChar(name) {
    if (!name) {
      return '会'
    }
    const text = String(name).trim()
    return text ? text.substring(0, 1) : '会'
  },

  async loadSite() {
    try {
      const home = await app.request({ url: '/api/open/site/home', silent: true })
      app.globalData.site = (home && home.site) || {}
      this.setData({ site: app.globalData.site })
    } catch (e) {
      // 站点信息失败不阻塞页面
    }
  },

  async refreshProfile() {
    try {
      const profile = await auth.memberRequest({ url: '/api/open/member/profile' })
      auth.save(auth.token(), profile)
      this.setData({ profile, avatarChar: this.firstChar(profile && profile.nickname) })
    } catch (e) {
      if (e && e.code === 40100) {
        this.setData({ logged: false, profile: null })
      }
    }
  },

  inputPhone(e) {
    this.setData({ phone: e.detail.value })
  },

  inputCode(e) {
    this.setData({ code: e.detail.value })
  },

  async sendCode() {
    const phone = this.data.phone.trim()
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (this.data.sending || this.data.countdown > 0) {
      return
    }
    this.setData({ sending: true })
    try {
      const result = await app.request({
        url: '/api/open/member/sms',
        method: 'POST',
        data: { phone }
      })
      this.setData({ devCode: result.devCode || '', countdown: 60 })
      this.tick()
      wx.showToast({ title: '验证码已发送', icon: 'none' })
    } catch (e) {
      this.setData({ sending: false })
      return
    }
    this.setData({ sending: false })
  },

  tick() {
    if (this.data.countdown <= 0) {
      return
    }
    setTimeout(() => {
      this.setData({ countdown: this.data.countdown - 1 }, () => this.tick())
    }, 1000)
  },

  async doLogin() {
    const phone = this.data.phone.trim()
    const code = this.data.code.trim()
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (code.length < 4) {
      wx.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }
    this.setData({ logging: true })
    try {
      const result = await app.request({
        url: '/api/open/member/login',
        method: 'POST',
        data: { phone, code, source: '小程序' }
      })
      auth.save(result.token, result.profile)
      this.setData({
        logged: true,
        profile: result.profile,
        avatarChar: this.firstChar(result.profile && result.profile.nickname),
        code: '',
        devCode: ''
      })
      wx.showToast({ title: result.created ? '登录成功' : '欢迎回来', icon: 'success' })
    } catch (e) {
      // 请求层已提示
    }
    this.setData({ logging: false })
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需重新验证手机号，订单与护理记录不会丢失。',
      success: (res) => {
        if (!res.confirm) {
          return
        }
        auth.clear()
        this.setData({ logged: false, profile: null, panel: '', points: [], coupons: [], addresses: [] })
        wx.showToast({ title: '已退出', icon: 'none' })
      }
    })
  },

  openPanel(e) {
    const panel = e.currentTarget.dataset.panel
    if (!this.data.logged) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    this.setData({ panel })
    if (panel === 'points') {
      this.loadPoints()
    } else if (panel === 'coupons') {
      this.loadCoupons()
    } else if (panel === 'recharge') {
      this.loadRecharge()
    } else if (panel === 'addresses') {
      this.loadAddresses()
    } else if (panel === 'referral') {
      this.loadReferrals()
    }
  },

  closePanel() {
    this.setData({ panel: '', showAddress: false })
  },

  noop() {},

  async loadPoints() {
    const points = await auth.memberRequest({ url: '/api/open/member/points' })
    this.setData({ points: points || [] })
  },

  async loadCoupons() {
    const [coupons, claimable] = await Promise.all([
      auth.memberRequest({ url: '/api/open/member/coupons' }),
      auth.memberRequest({ url: '/api/open/member/coupons/claimable' })
    ])
    this.setData({ coupons: coupons || [], claimable: claimable || [] })
  },

  async claim(e) {
    const id = e.currentTarget.dataset.id
    try {
      await auth.memberRequest({ url: '/api/open/member/coupons/' + id + '/claim', method: 'POST' })
      wx.showToast({ title: '领取成功', icon: 'success' })
      this.loadCoupons()
      this.refreshProfile()
    } catch (err) {
      // 请求层已提示
    }
  },

  async loadRecharge() {
    const [rule, recharges] = await Promise.all([
      auth.memberRequest({ url: '/api/open/member/recharge-rule' }),
      auth.memberRequest({ url: '/api/open/member/recharges' })
    ])
    this.setData({ rechargeRule: rule, recharges: recharges || [], ruleShown: false })
  },

  showRule() {
    const rule = this.data.rechargeRule
    if (!rule) {
      return
    }
    wx.showModal({
      title: '预付卡规则',
      content: rule.items.join('\n'),
      showCancel: false
    })
    this.setData({ ruleShown: true })
  },

  inputRecharge(e) {
    this.setData({ rechargeAmount: e.detail.value })
  },

  async submitRecharge() {
    const amount = Number(this.data.rechargeAmount)
    if (!amount || amount <= 0) {
      wx.showToast({ title: '请输入储值金额', icon: 'none' })
      return
    }
    if (!this.data.ruleShown) {
      wx.showToast({ title: '请先查看预付卡规则', icon: 'none' })
      return
    }
    try {
      const result = await auth.memberRequest({
        url: '/api/open/member/recharge',
        method: 'POST',
        data: { amount, ruleShown: true }
      })
      wx.showModal({
        title: '储值成功',
        content: '本金 ¥' + result.amount + '，赠送 ¥' + result.giftAmount
          + '，当前余额 ¥' + result.balance + '。到店可开预付卡凭证。',
        showCancel: false
      })
      this.setData({ rechargeAmount: '' })
      this.loadRecharge()
      this.refreshProfile()
    } catch (e) {
      // 请求层已提示
    }
  },

  async loadAddresses() {
    const addresses = await auth.memberRequest({ url: '/api/open/member/addresses' })
    this.setData({ addresses: addresses || [] })
  },

  openAddress() {
    this.setData({
      showAddress: true,
      addressForm: { receiver: '', phone: '', region: '', detail: '', isDefault: 0 }
    })
  },

  closeAddress() {
    this.setData({ showAddress: false })
  },

  inputAddress(e) {
    this.setData({ ['addressForm.' + e.currentTarget.dataset.field]: e.detail.value })
  },

  toggleDefault() {
    this.setData({ 'addressForm.isDefault': this.data.addressForm.isDefault ? 0 : 1 })
  },

  async saveAddress() {
    const form = this.data.addressForm
    if (!form.receiver || !form.phone || !form.detail) {
      wx.showToast({ title: '收货人、电话、详细地址必填', icon: 'none' })
      return
    }
    try {
      await auth.memberRequest({
        url: '/api/open/member/addresses',
        method: 'POST',
        data: form
      })
      this.setData({ showAddress: false })
      wx.showToast({ title: '已保存', icon: 'success' })
      this.loadAddresses()
    } catch (e) {
      // 请求层已提示
    }
  },

  removeAddress(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除地址',
      content: '确认删除该地址？',
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await auth.memberRequest({ url: '/api/open/member/addresses/' + id, method: 'DELETE' })
          this.loadAddresses()
        } catch (err) {
          // 请求层已提示
        }
      }
    })
  },

  async loadReferrals() {
    const referrals = await auth.memberRequest({ url: '/api/open/customer/referrals' })
    this.setData({ referrals: referrals || [] })
  },

  copyReferral() {
    const nickname = (this.data.profile && this.data.profile.nickname) || '我'
    const text = nickname + ' 邀请您参观月之孕月子中心：环境、月子餐、康复都能实地体验，'
      + '报我手机号可享到店礼。'
    wx.setClipboardData({ data: text })
  },

  pickLang(e) {
    const index = Number(e.detail.value)
    const lang = this.data.langs[index].code
    wx.setStorageSync('lang', lang)
    app.globalData.lang = lang
    this.setData({ lang })
    wx.showToast({ title: '内容将按所选语言展示', icon: 'none' })
  },

  showAgreement(e) {
    const type = e.currentTarget.dataset.type
    const content = type === 'privacy'
      ? '我们仅收集服务必需信息（姓名、手机号、孕产周期、护理记录），用于预约、护理执行与售后服务；'
        + '手机号、证件号加密存储，前台与后台展示一律脱敏；未经您同意不向第三方提供，'
        + '法律法规另有规定的除外。您可随时联系门店查询、更正或删除个人信息。'
      : '预约参观与下单前请确认套餐内容、入离馆日期与退订规则；定金支付后档期锁定，'
        + '退订按合同约定期限扣除已发生费用；护理服务由持证护理师执行，'
        + '如对服务有异议可通过小程序「反馈」提交，门店 30 分钟内响应。'
    wx.showModal({ title: type === 'privacy' ? '隐私政策' : '用户协议', content, showCancel: false })
  },

  callStaff() {
    const phone = (this.data.site && this.data.site.phone) || '400-000-0000'
    wx.makePhoneCall({ phoneNumber: phone.replace(/-/g, '') })
  },

  toOrders() {
    wx.switchTab({ url: '/pages/orders/orders' })
  },

  toCare() {
    wx.navigateTo({ url: '/pages/care/care' })
  },

  toServices() {
    wx.navigateTo({ url: '/pages/services/services' })
  }
})
