/**
 * 会员登录态与统一请求封装。
 *
 * 令牌存本地，所有需要会员身份的接口统一走 memberRequest，
 * 40100/40101 时清理本地态并引导重新登录。
 */
const TOKEN_KEY = 'memberToken'
const PROFILE_KEY = 'memberProfile'

function token() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function profile() {
  return wx.getStorageSync(PROFILE_KEY) || null
}

function save(tokenValue, profileValue) {
  wx.setStorageSync(TOKEN_KEY, tokenValue)
  if (profileValue) {
    wx.setStorageSync(PROFILE_KEY, profileValue)
  }
}

function clear() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(PROFILE_KEY)
}

function logged() {
  return !!token()
}

/** 带会员令牌的请求；未登录直接抛错，由调用方渲染登录引导 */
function memberRequest(options) {
  const t = token()
  if (!t) {
    const err = new Error('未登录')
    err.code = 40100
    return Promise.reject(err)
  }
  const app = getApp()
  return app.request(Object.assign({}, options, {
    header: Object.assign({ 'X-Member-Token': t }, options.header || {})
  })).catch((err) => {
    if (err && (err.code === 40100 || err.code === 40101)) {
      clear()
    }
    throw err
  })
}

function toLogin() {
  wx.showToast({ title: '请先登录', icon: 'none' })
  setTimeout(() => {
    wx.switchTab({ url: '/pages/member/member' })
  }, 400)
}

module.exports = { token, profile, save, clear, logged, memberRequest, toLogin }
