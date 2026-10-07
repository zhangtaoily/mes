import { get, post } from './request'

/** 菜单/路由节点（后端 RouterVO） */
export interface MenuNode {
  id: number
  parentId: number
  menuName: string
  menuType: number // 1目录 2菜单 3按钮
  path: string | null
  component: string | null
  perms: string | null
  children?: MenuNode[]
}

/** 当前登录用户信息 */
export interface UserInfo {
  userId: number
  userName: string
  empNo: string
  orgId: number
  roles: string[]
  perms: string[]
}

export interface LoginResult {
  token: string
}

export function loginApi(body: { loginName: string; password: string }) {
  return post<LoginResult>('/api/auth/login', body)
}

export function getUserInfo() {
  return get<UserInfo>('/api/auth/info')
}

export function getMenus() {
  return get<MenuNode[]>('/api/auth/menus')
}
