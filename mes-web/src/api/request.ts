import axios from 'axios'
import type { AxiosResponse } from 'axios'
import { antd } from '@/components/antdStatic'

/** 后端统一返回体 R<T> */
export interface R<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 分页返回结构 */
export interface PageResult<T = unknown> {
  total: number
  rows: T[]
}

const TOKEN_KEY = 'mes_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

/** 业务异常（code != 200 / HTTP 错误） */
export class BizError extends Error {
  code: number
  constructor(msg: string, code: number) {
    super(msg)
    this.code = code
  }
}

const http = axios.create({ timeout: 30000 })

// 请求拦截：附加 token
http.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：仅处理 HTTP 层错误（401 登录过期 / 403 无权限 / 网络异常）
http.interceptors.response.use(undefined, (error) => {
  const status = error.response?.status
  const r = error.response?.data as R | undefined
  if (status === 401) {
    tokenStore.clear()
    if (!location.hash.startsWith('#/login')) {
      antd.message?.error('登录已过期，请重新登录')
      location.hash = '#/login'
    }
    return Promise.reject(new BizError('未登录或登录已过期', 401))
  }
  if (status === 403) {
    antd.message?.error(r?.msg || '没有操作权限')
    return Promise.reject(new BizError(r?.msg || '没有操作权限', 403))
  }
  antd.message?.error(r?.msg || error.message || '网络异常')
  return Promise.reject(new BizError(r?.msg || error.message, status ?? -1))
})

/** 解包统一返回体：code=200 → data，否则报错 */
function unwrap<T>(resp: AxiosResponse<R<T>>): T {
  const r = resp.data
  if (r && typeof r.code === 'number') {
    if (r.code === 200) return r.data
    antd.message?.error(r.msg || '操作失败')
    throw new BizError(r.msg || '操作失败', r.code)
  }
  return r as unknown as T
}

/** GET：返回已解包的 data */
export function get<T>(url: string, params?: object): Promise<T> {
  return http.get<R<T>>(url, { params }).then(unwrap)
}

/** POST */
export function post<T>(url: string, body?: unknown, params?: object): Promise<T> {
  return http.post<R<T>>(url, body, { params }).then(unwrap)
}

/** PUT */
export function put<T>(url: string, body?: unknown, params?: object): Promise<T> {
  return http.put<R<T>>(url, body, { params }).then(unwrap)
}

/** DELETE */
export function del<T>(url: string, params?: object): Promise<T> {
  return http.delete<R<T>>(url, { params }).then(unwrap)
}

export default http
