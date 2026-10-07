import { get, post, put, del } from './request'
import type { PageResult } from './request'

// ============ 系统管理（system） ============

export interface SysUser {
  id: number
  orgId?: number
  empNo?: string
  userName: string
  loginName: string
  phone?: string
  status: number
}

export interface SysRole {
  id: number
  roleName: string
  roleKey: string
  dataScope?: number
  status: number
  remark?: string
}

export function pageUsers(params: { pageNum?: number; pageSize?: number; loginName?: string; userName?: string; orgId?: number; status?: number }) {
  return get<PageResult<SysUser>>('/api/sys/users', params)
}
export function saveUser(body: { loginName: string; userName: string; empNo?: string; orgId?: number; phone?: string; password?: string; roleIds?: number[] }) {
  return post<void>('/api/sys/users', body)
}
export function updateUser(id: number, body: { loginName?: string; userName?: string; empNo?: string; orgId?: number; phone?: string; roleIds?: number[] }) {
  return put<void>(`/api/sys/users/${id}`, body)
}
export function deleteUser(id: number) {
  return del<void>(`/api/sys/users/${id}`)
}
export function resetPassword(id: number, password: string) {
  return put<void>(`/api/sys/users/${id}/password`, { password })
}
export function changeUserStatus(id: number, status: number) {
  return put<void>(`/api/sys/users/${id}/status/${status}`)
}

export function getAllRoles() {
  return get<SysRole[]>('/api/sys/roles/all')
}
export function pageRoles(params: { pageNum?: number; pageSize?: number; roleName?: string }) {
  return get<PageResult<SysRole>>('/api/sys/roles', params)
}
