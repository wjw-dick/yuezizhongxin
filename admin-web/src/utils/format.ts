export type Tone = 'ok' | 'info' | 'warn' | 'danger' | 'muted'

export const TENANT_STATUS: Record<string, { text: string; tone: Tone }> = {
  TRIAL: { text: '试用中', tone: 'info' },
  NORMAL: { text: '正常', tone: 'ok' },
  ARREARS: { text: '欠费', tone: 'warn' },
  DISABLED: { text: '已停用', tone: 'muted' },
  EXPIRED: { text: '已到期', tone: 'danger' }
}

export const BILL_STATUS: Record<string, { text: string; tone: Tone }> = {
  PENDING: { text: '待支付', tone: 'warn' },
  PAID: { text: '已支付', tone: 'ok' },
  OVERDUE: { text: '已逾期', tone: 'danger' }
}

export const tenantStatus = (code?: string) =>
  TENANT_STATUS[code || ''] || { text: code || '-', tone: 'muted' as Tone }

export const billStatus = (code?: string) =>
  BILL_STATUS[code || ''] || { text: code || '-', tone: 'muted' as Tone }

/** 后端返回 yyyy-MM-dd HH:mm:ss，表格只展示日期 */
export const fmtDate = (value?: string) => (value ? String(value).slice(0, 10) : '-')

export const fmtDateTime = (value?: string) => (value ? String(value).replace('T', ' ').slice(0, 19) : '-')

export const money = (value?: number | string) => {
  const num = Number(value ?? 0)
  return '¥' + num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 距今天数，负数表示已过期 */
export const daysLeft = (value?: string) => {
  if (!value) return null
  const target = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(target.getTime())) return null
  const diff = target.getTime() - Date.now()
  return Math.ceil(diff / 86400000)
}

export const percent = (used?: number, limit?: number) => {
  if (!limit || limit <= 0) return 0
  return Math.min(100, Math.round(((used || 0) / limit) * 100))
}
