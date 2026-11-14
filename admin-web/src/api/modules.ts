import http from './http'

/** 登录：租户标识 + 账号密码，成功后令牌与租户标识一起持久化。 */
export const login = (payload: { tenantCode: string; username: string; password: string }) =>
  http.post('/api/tenant/auth/login', payload)

export const platformLogin = (payload: { username: string; password: string }) =>
  http.post('/api/platform/auth/login', payload)

/* ---------------- 系统参数（T20） ---------------- */
export const listConfigs = () => http.get('/api/tenant/configs')
export const saveConfigGroup = (payload: any) => http.put('/api/tenant/configs', payload)

/* ---------------- 数据字典（T29） ---------------- */
export const listDictCategories = () => http.get('/api/tenant/dicts/categories')
export const createDictCategory = (payload: any) => http.post('/api/tenant/dicts/categories', payload)
export const updateDictCategory = (id: number, payload: any) =>
  http.put(`/api/tenant/dicts/categories/${id}`, payload)
export const deleteDictCategory = (id: number) => http.delete(`/api/tenant/dicts/categories/${id}`)
export const listDictItems = (category: string) =>
  http.get(`/api/tenant/dicts/categories/${category}/items`)
export const createDictItem = (category: string, payload: any) =>
  http.post(`/api/tenant/dicts/categories/${category}/items`, payload)
export const updateDictItem = (id: number, payload: any) => http.put(`/api/tenant/dicts/items/${id}`, payload)
export const deleteDictItem = (id: number) => http.delete(`/api/tenant/dicts/items/${id}`)

/* ---------------- 多语言（T21） ---------------- */
export const listLanguages = () => http.get('/api/tenant/i18n/languages')
export const saveLanguage = (id: number | null, payload: any) =>
  id ? http.put(`/api/tenant/i18n/languages/${id}`, payload) : http.post('/api/tenant/i18n/languages', payload)
export const setDefaultLanguage = (id: number) => http.put(`/api/tenant/i18n/languages/${id}/default`)
export const deleteLanguage = (id: number) => http.delete(`/api/tenant/i18n/languages/${id}`)
export const listMessages = (langCode: string) => http.get('/api/tenant/i18n/messages', { params: { langCode } })
export const saveMessages = (payload: any) => http.put('/api/tenant/i18n/messages', payload)

/* ---------------- 菜单可视化（T22） ---------------- */
export const getNavTree = (endType: string) => http.get('/api/tenant/navs', { params: { endType } })
export const saveNavTree = (payload: any) => http.put('/api/tenant/navs', payload)

/* ---------------- 对象存储（T23/T24） ---------------- */
export const getStorageConfig = () => http.get('/api/tenant/storage-config')
export const testStorage = (payload: any) => http.post('/api/tenant/storage-config/test', payload)
export const saveStorage = (payload: any) => http.put('/api/tenant/storage-config', payload)

/* ---------------- 消息配置（T27） ---------------- */
export const getChannels = () => http.get('/api/tenant/messages/channels')
export const saveChannel = (payload: any) => http.put('/api/tenant/messages/channels', payload)
export const testSend = (params: { channelCode: string; phone: string; content?: string }) =>
  http.post('/api/tenant/messages/test', null, { params })
export const listTemplates = (params?: any) => http.get('/api/tenant/messages/templates', { params })
export const saveTemplate = (payload: any) => http.put('/api/tenant/messages/templates', payload)
export const deleteTemplate = (id: number) => http.delete(`/api/tenant/messages/templates/${id}`)

/* ---------------- SSO（T28） ---------------- */
export const getSsoConfig = () => http.get('/api/tenant/sso-config')
export const saveSsoConfig = (payload: any) => http.put('/api/tenant/sso-config', payload)
