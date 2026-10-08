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

// ============ 字典管理（sys_dict / sys_dict_item） ============

export interface SysDict {
  id: number
  dictType: string
  dictName?: string
  status?: number
}
export interface SysDictItem {
  id: number
  dictType: string
  itemLabel: string
  itemValue: string
  sortNo?: number
  status?: number
}

export function pageDicts(params: { pageNum?: number; pageSize?: number; dictName?: string; dictType?: string; status?: number }) {
  return get<PageResult<SysDict>>('/api/sys/dicts', params)
}
export function saveDict(body: Partial<SysDict>) { return post<void>('/api/sys/dicts', body) }
export function updateDict(id: number, body: Partial<SysDict>) { return put<void>(`/api/sys/dicts/${id}`, body) }
export function deleteDict(id: number) { return del<void>(`/api/sys/dicts/${id}`) }
export function getDictItems(id: number) { return get<SysDictItem[]>(`/api/sys/dicts/${id}/items`) }
export function saveDictItem(id: number, body: Partial<SysDictItem>) { return post<void>(`/api/sys/dicts/${id}/items`, body) }
export function updateDictItem(id: number, itemId: number, body: Partial<SysDictItem>) { return put<void>(`/api/sys/dicts/${id}/items/${itemId}`, body) }
export function deleteDictItem(id: number, itemId: number) { return del<void>(`/api/sys/dicts/${id}/items/${itemId}`) }
