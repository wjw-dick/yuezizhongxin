import { platformHttp } from './client'

/* ---------------- 平台运营端 ---------------- */

export const platformLogin = (payload: { username: string; password: string }) =>
  platformHttp.post('/api/platform/auth/login', payload)

export const platformMenus = () => platformHttp.get('/api/platform/auth/menus')

/** 租户列表：关键词、状态、30 天内到期 */
export const listTenants = (params: Record<string, unknown>) =>
  platformHttp.get('/api/platform/tenants', { params })
export const getTenant = (id: number) => platformHttp.get(`/api/platform/tenants/${id}`)
export const createTenant = (payload: Record<string, unknown>) =>
  platformHttp.post('/api/platform/tenants', payload)
export const disableTenant = (id: number) => platformHttp.post(`/api/platform/tenants/${id}/disable`)
export const resumeTenant = (id: number) => platformHttp.post(`/api/platform/tenants/${id}/resume`)
export const renewTenant = (id: number, days: number) =>
  platformHttp.post(`/api/platform/tenants/${id}/renew`, { days })
export const adjustQuota = (id: number, payload: Record<string, unknown>) =>
  platformHttp.put(`/api/platform/tenants/${id}/quota`, payload)

export const listPackages = () => platformHttp.get('/api/platform/packages')
export const savePackage = (payload: Record<string, unknown>) =>
  platformHttp.post('/api/platform/packages', payload)
export const deletePackage = (id: number) => platformHttp.delete(`/api/platform/packages/${id}`)

export const listBills = (params: Record<string, unknown>) =>
  platformHttp.get('/api/platform/bills', { params })
export const generateBills = (billPeriod: string) =>
  platformHttp.post('/api/platform/bills/generate', { billPeriod })
export const payBill = (id: number) => platformHttp.post(`/api/platform/bills/${id}/pay`)
export const markOverdue = () => platformHttp.post('/api/platform/bills/mark-overdue')

export const listAudits = (params: Record<string, unknown>) =>
  platformHttp.get('/api/platform/audits', { params })
