import axios from 'axios'

/**
 * 租户解析与请求封装。
 *
 * H5 常被分享到微信群：链接带 ?tenant=xxx 时优先使用，其次读 localStorage，
 * 都取不到则回落到 demo，保证落地页永远有内容。
 */
const params = new URLSearchParams(location.search)
const tenantFromUrl = params.get('tenant')
if (tenantFromUrl) {
  localStorage.setItem('tenantCode', tenantFromUrl)
}
export const tenantCode = tenantFromUrl || localStorage.getItem('tenantCode') || 'demo'

const http = axios.create({ baseURL: '/', timeout: 15000 })

http.interceptors.request.use((config) => {
  config.headers = config.headers || {}
  config.headers['X-Tenant-Code'] = tenantCode
  const token = localStorage.getItem('memberToken')
  if (token) {
    config.headers['X-Member-Token'] = token
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data || {}
    if (body.code === 0) {
      return body.data
    }
    if (body.code === 40100) {
      localStorage.removeItem('memberToken')
    }
    return Promise.reject(new Error(body.msg || '请求失败'))
  },
  (error) => Promise.reject(error)
)

export interface HomeData {
  site: { name: string; phone: string; address: string; icp?: string }
  packages: any[]
  roomTypes: any[]
  articles?: any[]
  cases?: any[]
  banners?: any[]
}

export const api = {
  home: (lang = 'zh-CN') => http.get<any, HomeData>('/api/open/site/home', { params: { lang } }),
  contents: (type: string, lang = 'zh-CN') =>
    http.get<any, any[]>('/api/open/site/contents', { params: { type, lang, end: 'H5' } }),
  content: (id: number | string, lang = 'zh-CN') =>
    http.get<any, any>(`/api/open/site/contents/${id}`, { params: { lang } }),
  packages: () => http.get<any, any[]>('/api/open/site/packages'),
  rooms: () => http.get<any, any[]>('/api/open/site/rooms'),
  activities: () => http.get<any, any[]>('/api/open/site/contents', { params: { type: '活动', end: 'H5' } }),
  lead: (data: any) => http.post<any, any>('/api/open/leads', data),
  visit: (data: any) => http.post<any, any>('/api/open/leads/visits', data),
  queryOrder: (orderNo: string, phone: string) =>
    http.get<any, any>(`/api/open/orders/${orderNo}/query`, { params: { phone } }),
  pay: (id: number, data: any) => http.post<any, any>(`/api/open/orders/${id}/pay`, data),
  sms: (phone: string) => http.post<any, any>('/api/open/member/sms', { phone }),
  login: (phone: string, code: string) =>
    http.post<any, any>('/api/open/member/login', { phone, code, source: 'H5' }),
  profile: () => http.get<any, any>('/api/open/member/profile'),
  coupons: () => http.get<any, any[]>('/api/open/member/coupons'),
  claim: (id: number) => http.post<any, any>(`/api/open/member/coupons/${id}/claim`)
}

/** 埋点：记录分享渠道与落地页来源，供后台渠道分析使用 */
export function track(event: string, extra: Record<string, unknown> = {}) {
  const payload = {
    event,
    tenantCode,
    ch: params.get('ch') || localStorage.getItem('ch') || 'direct',
    path: location.pathname,
    ts: Date.now(),
    ...extra
  }
  // 后端未接埋点接口时不阻塞页面，仅保留本地队列便于后续批量上报
  const queue = JSON.parse(localStorage.getItem('h5Events') || '[]')
  queue.push(payload)
  localStorage.setItem('h5Events', JSON.stringify(queue.slice(-50)))
}
