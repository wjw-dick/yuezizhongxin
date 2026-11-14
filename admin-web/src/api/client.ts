import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'

/** 租户后台的令牌、租户标识与登录用户 */
export const TENANT_KEY = 'yzz.tenantCode'
export const TOKEN_KEY = 'yzz.token'
export const USER_KEY = 'yzz.user'

/** 平台运营端独立令牌：两端令牌不互通，避免误用入口 */
export const PLATFORM_TOKEN_KEY = 'yzz.platform.token'
export const PLATFORM_USER_KEY = 'yzz.platform.user'

const BUSINESS_CONFIRM_CODE = 40900

function build(tokenKey: string, tenantScoped: boolean, loginPath: string): AxiosInstance {
  const instance = axios.create({ baseURL: '', timeout: 20000 })

  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem(tokenKey)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (tenantScoped) {
      // 租户标识来自登录页选择，后端会与令牌内租户比对，篡改直接 403
      const tenant = localStorage.getItem(TENANT_KEY)
      if (tenant) {
        config.headers['X-Tenant-Code'] = tenant
      }
    }
    return config
  })

  instance.interceptors.response.use(
    (response) => {
      const body = response.data
      if (body && typeof body.code === 'number' && body.code !== 0) {
        // 40900 用于需要二次确认的场景，交由页面自行处理
        if (body.code !== BUSINESS_CONFIRM_CODE) {
          ElMessage.error(body.msg || '请求失败')
        }
        return Promise.reject(body)
      }
      return body
    },
    (error) => {
      const body = error.response?.data
      if (error.response?.status === 401) {
        localStorage.removeItem(tokenKey)
        if (!location.hash.includes('/login')) {
          location.hash = '#' + loginPath
        }
      }
      const msg = body?.msg || error.message || '网络异常'
      if (body?.code !== BUSINESS_CONFIRM_CODE) {
        ElMessage.error(msg)
      }
      return Promise.reject(body || { code: -1, msg })
    }
  )

  return instance
}

export const tenantHttp = build(TOKEN_KEY, true, '/login')
export const platformHttp = build(PLATFORM_TOKEN_KEY, false, '/login?app=platform')
