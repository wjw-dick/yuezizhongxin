import { tenantHttp } from './client'

/** 列表：兼容数组与分页结构，统一返回 { records, total }。 */
export async function fetchList(path: string, params: Record<string, any> = {}) {
  const res: any = await tenantHttp.get(path, { params })
  const data = res?.data
  if (Array.isArray(data)) {
    return { records: data, total: data.length }
  }
  return {
    records: data?.records || [],
    total: Number(data?.total || 0)
  }
}

export const fetchOne = (path: string, id: number | string) => tenantHttp.get(`${path}/${id}`)

export const createOne = (path: string, payload: any) => tenantHttp.post(path, payload)

export const updateOne = (path: string, id: number | string, payload: any) =>
  tenantHttp.put(`${path}/${id}`, payload)

export const saveOne = (path: string, payload: any) => tenantHttp.post(path, payload)

export const removeOne = (path: string, id: number | string) => tenantHttp.delete(`${path}/${id}`)

/** 上传文件（带进度回调），返回 { objectKey, url, size }。 */
export function uploadFile(
  file: File,
  dir = 'common',
  onProgress?: (percent: number) => void
): Promise<{ objectKey: string; url: string; size: number }> {
  return new Promise((resolve, reject) => {
    const form = new FormData()
    form.append('file', file)
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `/api/files/upload?dir=${encodeURIComponent(dir)}`)
    const token = localStorage.getItem('yzz.token')
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    }
    const tenant = localStorage.getItem('yzz.tenantCode')
    if (tenant) {
      xhr.setRequestHeader('X-Tenant-Code', tenant)
    }
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }
    xhr.onload = () => {
      try {
        const body = JSON.parse(xhr.responseText)
        if (body.code === 0) {
          resolve(body.data)
        } else {
          reject(new Error(body.msg || '上传失败'))
        }
      } catch (e) {
        reject(new Error('上传失败'))
      }
    }
    xhr.onerror = () => reject(new Error('网络异常，上传失败'))
    xhr.send(form)
  })
}

/** 下载导出文件（POST），按后端文件名保存。 */
export async function downloadExport(payload: Record<string, any>) {
  const res = await tenantHttp.post('/api/tenant/reports/export', payload, {
    responseType: 'blob'
  } as any)
  const blob = res as unknown as Blob
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${payload.module || 'export'}-${new Date().toISOString().slice(0, 10)}.xls`
  link.click()
  URL.revokeObjectURL(url)
}
