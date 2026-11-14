import axios from 'axios'

/**
 * 官网与 H5 共用开放接口。
 * 租户通过 X-Tenant-Code 传递（部署时由 nginx 按子域名注入，也可用 ?tenant= 兜底）。
 */
const params = new URLSearchParams(location.search)
const tenant =
  params.get('tenant') ||
  localStorage.getItem('yzz.tenantCode') ||
  (location.hostname.split('.').length > 2 ? location.hostname.split('.')[0] : 'demo')

if (tenant) {
  localStorage.setItem('yzz.tenantCode', tenant)
}

export const http = axios.create({ baseURL: '', timeout: 15000 })

http.interceptors.request.use((config) => {
  config.headers['X-Tenant-Code'] = tenant
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body?.data
  },
  (error) => Promise.reject(new Error(error.response?.data?.msg || error.message || '网络异常'))
)

export const tenantCode = tenant

export const siteApi = {
  home: (lang?: string) => http.get('/api/open/site/home', { params: { lang } }),
  contents: (type: string, lang?: string) => http.get('/api/open/site/contents', { params: { type, lang } }),
  content: (id: number | string, lang?: string) => http.get(`/api/open/site/contents/${id}`, { params: { lang } }),
  rooms: () => http.get('/api/open/site/rooms'),
  packages: () => http.get('/api/open/site/packages'),
  nav: () => http.get('/api/open/site/nav', { params: { end: 'PC官网' } }),
  meta: () => http.get('/api/open/site/meta'),
  entries: (type?: string) => http.get('/api/open/site/entries', { params: { type } }),
  scan: (id: number) => http.post(`/api/open/site/entries/${id}/scan`),
  lead: (payload: Record<string, any>) => http.post('/api/open/leads', payload),
  visit: (payload: Record<string, any>) => http.post('/api/open/leads/visits', payload)
}
